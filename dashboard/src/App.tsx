import React, { useEffect, useState, useMemo } from 'react';
import { EmergencyHeader } from './components/EmergencyHeader';
import { LiveTelemetryCard } from './components/LiveTelemetryCard';
import { LiveMapViewer } from './components/LiveMapViewer';
import { VideoPlayer } from './components/VideoPlayer';
import { EmergencyTimeline } from './components/EmergencyTimeline';
import { fetchIncident, fetchAllIncidents, resolveIncident, IncidentData } from './services/api';
import { EmergencyWsClient } from './services/websocket';
import { Shield, RefreshCw } from 'lucide-react';

export default function App() {
  const [incident, setIncident] = useState<IncidentData | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [locationHistory, setLocationHistory] = useState<any[]>([]);
  const [liveFrame, setLiveFrame] = useState<string | null>(null);
  const [audioLevel, setAudioLevel] = useState<number>(0);
  const [recordingUrl, setRecordingUrl] = useState<string | null>(null);

  // Parse URL query parameters
  const urlParams = new URLSearchParams(window.location.search);
  const queryIncidentId = urlParams.get('incidentId') || window.location.pathname.split('/').pop() || '';
  const token = urlParams.get('token') || 'demo-token';

  // Calculate total path distance traversed
  const totalDistanceMeters = useMemo(() => {
    if (locationHistory.length < 2) return 0;
    let dist = 0;
    for (let i = 1; i < locationHistory.length; i++) {
      const p1 = locationHistory[i - 1];
      const p2 = locationHistory[i];
      if (p1.latitude && p1.longitude && p2.latitude && p2.longitude) {
        const R = 6371e3;
        const φ1 = (p1.latitude * Math.PI) / 180;
        const φ2 = (p2.latitude * Math.PI) / 180;
        const Δφ = ((p2.latitude - p1.latitude) * Math.PI) / 180;
        const Δλ = ((p2.longitude - p1.longitude) * Math.PI) / 180;
        const a = Math.sin(Δφ / 2) * Math.sin(Δφ / 2) + Math.cos(φ1) * Math.cos(φ2) * Math.sin(Δλ / 2) * Math.sin(Δλ / 2);
        const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        dist += R * c;
      }
    }
    return Math.round(dist);
  }, [locationHistory]);

  const handleResolveEmergency = async () => {
    if (!incident || incident.incidentId === 'awaiting-sos') return;
    try {
      await resolveIncident(incident.incidentId, 'Resolved via Emergency Web Dashboard');
      setIncident((prev) =>
        prev
          ? {
              ...prev,
              status: 'RESOLVED',
              resolvedAt: Date.now(),
            }
          : null
      );
    } catch (err: any) {
      console.error('Resolve error:', err);
    }
  };

  useEffect(() => {
    let wsClient: EmergencyWsClient | null = null;

    async function loadData() {
      let data: IncidentData | null = null;
      try {
        setLoading(true);

        // 1. If explicit incidentId passed in URL (other than demo), try loading it
        if (queryIncidentId && queryIncidentId !== 'demo-sos-incident' && queryIncidentId !== '') {
          try {
            data = await fetchIncident(queryIncidentId, token);
          } catch (_) {}
        }

        // 2. If no explicit incident or not found, check backend for actual incidents
        if (!data) {
          try {
            const all = await fetchAllIncidents();
            const sorted = [...all].sort((a, b) => (b.createdAt || 0) - (a.createdAt || 0));
            const active = sorted.find((i) => i.status === 'ACTIVE') || sorted[0];
            if (active) {
              data = active;
            }
          } catch (_) {}
        }

        if (data) {
          setIncident(data);
          setLocationHistory(data.locationHistory || []);
          if (data.recordingUrl) setRecordingUrl(data.recordingUrl);
          if ((data as any).lastLiveFrame) setLiveFrame((data as any).lastLiveFrame);
          if ((data as any).lastAudioLevel != null) setAudioLevel((data as any).lastAudioLevel);
          setError(null);
        } else {
          // 3. If no incidents on backend yet, create a clean standby state (NO fake data)
          const standbyIncident: IncidentData = {
            incidentId: 'awaiting-sos',
            triggerType: 'NONE',
            status: 'STANDBY',
            initialLatitude: 0.0,
            initialLongitude: 0.0,
            currentLatitude: 0.0,
            currentLongitude: 0.0,
            accuracyMeters: 0,
            heartRateBpm: null,
            batteryPercentage: null,
            dashboardAccessToken: token,
            dashboardViewerUrl: window.location.href,
            streamToken: 'awaiting',
            createdAt: Date.now(),
          };

          // Try browser geolocation to center local map in standby
          if (typeof navigator !== 'undefined' && navigator.geolocation) {
            navigator.geolocation.getCurrentPosition(
              (pos) => {
                setIncident((prev) =>
                  prev && prev.status === 'STANDBY'
                    ? {
                        ...prev,
                        initialLatitude: pos.coords.latitude,
                        initialLongitude: pos.coords.longitude,
                        currentLatitude: pos.coords.latitude,
                        currentLongitude: pos.coords.longitude,
                        accuracyMeters: pos.coords.accuracy || 10,
                      }
                    : prev
                );
              },
              () => {},
              { timeout: 3000 }
            );
          }

          setIncident(standbyIncident);
        }
      } catch (err: any) {
        console.warn('API error:', err.message);
      } finally {
        setLoading(false);
      }

      // Establish real-time WebSocket connection to receive live updates from mobile device
      const wsTargetId = queryIncidentId && queryIncidentId !== '' ? queryIncidentId : (data?.incidentId || 'default-emergency-channel');
      wsClient = new EmergencyWsClient(wsTargetId, token, (event) => {
        if (event.type === 'LOCATION_UPDATE') {
          const loc = event.data;
          setIncident((prev) =>
            prev
              ? {
                  ...prev,
                  incidentId: loc.incidentId || prev.incidentId,
                  currentLatitude: loc.latitude,
                  currentLongitude: loc.longitude,
                  accuracyMeters: loc.accuracy || prev.accuracyMeters,
                  heartRateBpm: loc.heartRateBpm ?? prev.heartRateBpm,
                  batteryPercentage: loc.batteryPercentage ?? prev.batteryPercentage,
                  status: prev.status === 'RESOLVED' ? 'RESOLVED' : 'ACTIVE',
                }
              : {
                  incidentId: loc.incidentId || wsTargetId,
                  triggerType: 'MANUAL_IN_APP',
                  status: 'ACTIVE',
                  initialLatitude: loc.latitude,
                  initialLongitude: loc.longitude,
                  currentLatitude: loc.latitude,
                  currentLongitude: loc.longitude,
                  accuracyMeters: loc.accuracy || 10,
                  heartRateBpm: loc.heartRateBpm ?? null,
                  batteryPercentage: loc.batteryPercentage ?? null,
                  dashboardAccessToken: token,
                  dashboardViewerUrl: window.location.href,
                  streamToken: 'active',
                  createdAt: Date.now(),
                }
          );

          setLocationHistory((prev) => [...prev, loc]);
        } else if (event.type === 'LIVE_FRAME') {
          if (event.data?.frame) {
            setLiveFrame(event.data.frame);
          }
          if (event.data?.audioLevel != null) {
            setAudioLevel(event.data.audioLevel);
          }
        } else if (event.type === 'RECORDING_READY') {
          if (event.data?.recordingUrl) {
            setRecordingUrl(event.data.recordingUrl);
            setIncident((prev) => (prev ? { ...prev, recordingUrl: event.data.recordingUrl } : null));
          }
        } else if (event.type === 'INCIDENT_RESOLVED') {
          console.log('[Dashboard] Received INCIDENT_RESOLVED event');
          setIncident((prev) =>
            prev
              ? {
                  ...prev,
                  status: 'RESOLVED',
                  resolvedAt: event.data?.resolvedAt || Date.now(),
                }
              : null
          );
        }
      });
    }

    loadData();

    // Periodic sync ensuring current incident status, coordinates, and recordings are always live
    const pollInterval = setInterval(async () => {
      try {
        const all = await fetchAllIncidents();
        const sorted = [...all].sort((a, b) => (b.createdAt || 0) - (a.createdAt || 0));

        setIncident((prev) => {
          const targetId = queryIncidentId && queryIncidentId !== 'awaiting-sos'
            ? queryIncidentId
            : prev?.incidentId && prev.incidentId !== 'awaiting-sos'
            ? prev.incidentId
            : null;

          const latest = (targetId ? sorted.find((i) => i.incidentId === targetId) : null)
            || sorted.find((i) => i.status === 'ACTIVE')
            || sorted[0];

          if (latest) {
            if (!prev || prev.incidentId === 'awaiting-sos' || prev.incidentId !== latest.incidentId) {
              setLocationHistory(latest.locationHistory || []);
              if (latest.recordingUrl) setRecordingUrl(latest.recordingUrl);
              return latest;
            }

            if (latest.locationHistory && latest.locationHistory.length > 0) {
              setLocationHistory(latest.locationHistory);
            }

            return {
              ...prev,
              status: latest.status, // Update status (e.g. ACTIVE -> RESOLVED)
              resolvedAt: latest.resolvedAt ?? prev.resolvedAt,
              currentLatitude: latest.currentLatitude,
              currentLongitude: latest.currentLongitude,
              accuracyMeters: latest.accuracyMeters,
              heartRateBpm: latest.heartRateBpm,
              batteryPercentage: latest.batteryPercentage,
              recordingUrl: latest.recordingUrl || prev.recordingUrl,
            };
          }
          return prev;
        });
      } catch (_) {}
    }, 3000);

    return () => {
      clearInterval(pollInterval);
      wsClient?.disconnect();
    };
  }, [queryIncidentId, token]);

  if (loading && !incident) {
    return (
      <div className="min-h-screen bg-[#090D16] flex items-center justify-center p-6 text-center">
        <div className="flex flex-col items-center space-y-4">
          <div className="w-12 h-12 rounded-full border-2 border-rose-500 border-t-transparent animate-spin"></div>
          <p className="text-sm font-semibold text-slate-300">Connecting to Maaya Emergency Response Network...</p>
        </div>
      </div>
    );
  }

  if (!incident) {
    return (
      <div className="min-h-screen bg-[#090D16] flex items-center justify-center p-6 text-center">
        <div className="max-w-md bg-[#131B2E] border border-slate-800 rounded-2xl p-8">
          <Shield className="w-12 h-12 text-rose-500 mx-auto mb-4" />
          <h2 className="text-lg font-bold text-white">Emergency Session Not Found</h2>
          <p className="text-xs text-slate-400 mt-2">
            The emergency link may be invalid or the authorized session token has expired.
          </p>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-[#090D16] text-slate-100 flex flex-col">
      {/* Top Header */}
      <EmergencyHeader
        incidentId={incident.incidentId}
        triggerType={incident.triggerType}
        status={incident.status}
        startedAt={incident.createdAt}
        resolvedAt={incident.resolvedAt}
        onResolve={handleResolveEmergency}
      />

      {/* Main Grid Content */}
      <main className="flex-1 max-w-7xl w-full mx-auto p-4 sm:p-6 space-y-6">
        {/* Real-time Telemetry Cards (shows actual data or Not Available) */}
        <LiveTelemetryCard
          heartRateBpm={incident.heartRateBpm}
          batteryPercentage={incident.batteryPercentage}
          accuracyMeters={incident.accuracyMeters}
          latitude={incident.currentLatitude}
          longitude={incident.currentLongitude}
          status={incident.status}
        />

        {/* 2-Column Layout: Map Trace & Video Player */}
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 min-h-[460px]">
          {/* Live Map & Route Traces */}
          <LiveMapViewer
            latitude={incident.currentLatitude}
            longitude={incident.currentLongitude}
            accuracyMeters={incident.accuracyMeters}
            history={locationHistory}
            status={incident.status}
          />

          {/* Live Video / Replay of Recorded Evidence Video */}
          <VideoPlayer
            sessionId={incident.incidentId}
            isStreaming={incident.status === 'ACTIVE'}
            status={incident.status}
            liveFrame={liveFrame}
            audioLevel={audioLevel}
            recordingUrl={recordingUrl || incident.recordingUrl || null}
          />
        </div>

        {/* Incident Log & Evidence Dossier */}
        <EmergencyTimeline
          incidentId={incident.incidentId}
          triggerType={incident.triggerType}
          startedAt={incident.createdAt}
          resolvedAt={incident.resolvedAt}
          locationUpdatesCount={locationHistory.length}
          status={incident.status}
          recordingUrl={recordingUrl || incident.recordingUrl || null}
          locationHistory={locationHistory}
          totalDistanceMeters={totalDistanceMeters}
        />
      </main>
    </div>
  );
}
