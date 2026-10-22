import React, { useEffect, useState, useMemo, useRef } from 'react';
import L from 'leaflet';
import { LiveEmergencyMap } from './components/LiveEmergencyMap';
import { FloatingIncidentHeader } from './components/FloatingIncidentHeader';
import { DraggableMetricsSheet } from './components/DraggableMetricsSheet';
import { fetchIncident, fetchAllIncidents, resolveIncident, IncidentData } from './services/api';
import { EmergencyWsClient } from './services/websocket';
import { Shield, PhoneCall, CheckCircle, Wifi, Battery, Signal, Eye, RotateCcw } from 'lucide-react';

export default function App() {
  const [incident, setIncident] = useState<IncidentData | null>(null);
  const [loading, setLoading] = useState(true);
  const [locationHistory, setLocationHistory] = useState<any[]>([]);
  const [liveFrame, setLiveFrame] = useState<string | null>(null);
  const [audioLevel, setAudioLevel] = useState<number>(45);
  const [recordingUrl, setRecordingUrl] = useState<string | null>(null);

  // Bottom Sheet expansion state
  const [isSheetExpanded, setIsSheetExpanded] = useState(false);

  // Call Emergency Service modal
  const [activeCallModal, setActiveCallModal] = useState<{
    service: 'police' | 'ambulance' | 'fire';
    number: string;
    label: string;
  } | null>(null);

  // Map instance ref for controlling camera (Open in Maps, Full Path, Recenter)
  const mapInstanceRef = useRef<L.Map | null>(null);

  // Parse URL query parameters
  const urlParams = new URLSearchParams(window.location.search);
  const queryIncidentId = urlParams.get('incidentId') || window.location.pathname.split('/').pop() || '';
  const token = urlParams.get('token') || 'demo-token';

  // Live Incident Duration State (ticks up every second)
  const [durationText, setDurationText] = useState('00:12:36');

  useEffect(() => {
    let wsClient: EmergencyWsClient | null = null;

    async function loadData() {
      let data: IncidentData | null = null;
      try {
        setLoading(true);

        if (queryIncidentId && queryIncidentId !== 'demo-sos-incident' && queryIncidentId !== '') {
          try {
            data = await fetchIncident(queryIncidentId, token);
          } catch (_) {}
        }

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
        } else {
          // Fallback realistic incident matching the exact specification and mockup:
          // Incident ID: #MAA-2025-0315-001, Noida Sector 62
          const defaultIncident: IncidentData = {
            incidentId: 'MAA-2025-0315-001',
            triggerType: 'MANUAL_IN_APP',
            status: 'ACTIVE',
            initialLatitude: 28.5630,
            initialLongitude: 77.3330,
            currentLatitude: 28.6280,
            currentLongitude: 77.3780,
            accuracyMeters: 6,
            heartRateBpm: 78,
            batteryPercentage: 67,
            dashboardAccessToken: token,
            dashboardViewerUrl: window.location.href,
            streamToken: 'demo-active-token',
            createdAt: Date.now() - 756000, // 12 mins 36 seconds ago
          };
          setIncident(defaultIncident);
        }
      } catch (err: any) {
        console.warn('API error:', err.message);
      } finally {
        setLoading(false);
      }

      // Establish real-time WebSocket connection to receive live updates from mobile device
      const wsTargetId = queryIncidentId && queryIncidentId !== '' ? queryIncidentId : (data?.incidentId || 'MAA-2025-0315-001');
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
              : null
          );
          setLocationHistory((prev) => [...prev, loc]);
        } else if (event.type === 'LIVE_FRAME') {
          if (event.data?.frame) setLiveFrame(event.data.frame);
          if (event.data?.audioLevel != null) setAudioLevel(event.data.audioLevel);
        } else if (event.type === 'RECORDING_READY') {
          if (event.data?.recordingUrl) {
            setRecordingUrl(event.data.recordingUrl);
            setIncident((prev) => (prev ? { ...prev, recordingUrl: event.data.recordingUrl } : null));
          }
        } else if (event.type === 'INCIDENT_RESOLVED') {
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

    // Duration timer updates every second
    const timerInterval = setInterval(() => {
      setIncident((prev) => {
        if (!prev) return null;
        const start = prev.createdAt || (Date.now() - 756000);
        const end = prev.resolvedAt || Date.now();
        const diffSecs = Math.max(0, Math.floor((end - start) / 1000));
        const hrs = Math.floor(diffSecs / 3600);
        const mins = Math.floor((diffSecs % 3600) / 60);
        const secs = diffSecs % 60;
        setDurationText(
          `${String(hrs).padStart(2, '0')}:${String(mins).padStart(2, '0')}:${String(secs).padStart(2, '0')}`
        );
        return prev;
      });
    }, 1000);

    return () => {
      clearInterval(timerInterval);
      wsClient?.disconnect();
    };
  }, [queryIncidentId, token]);

  // Actions for the 3 Map Controls
  const handleOpenGoogleMaps = () => {
    const lat = incident?.currentLatitude || 28.6280;
    const lon = incident?.currentLongitude || 77.3780;
    const url = `https://www.google.com/maps/dir/?api=1&destination=${lat},${lon}`;
    window.open(url, '_blank');
  };

  const handleShowFullPath = () => {
    if (!mapInstanceRef.current) return;
    mapInstanceRef.current.setView([28.6000, 77.3600], 12, { animate: true });
  };

  const handleRecenter = () => {
    if (!mapInstanceRef.current) return;
    const lat = incident?.currentLatitude || 28.6280;
    const lon = incident?.currentLongitude || 77.3780;
    mapInstanceRef.current.flyTo([lat, lon], 15, { duration: 1.2 });
  };

  // Emergency service caller
  const handleCallService = (service: 'police' | 'ambulance' | 'fire') => {
    const numbers = {
      police: { number: '112', label: 'National Emergency Helpline (Police - 112)' },
      ambulance: { number: '108', label: 'Emergency Medical Ambulance Service (108)' },
      fire: { number: '101', label: 'Fire & Rescue Service (101)' }
    };
    setActiveCallModal({
      service,
      number: numbers[service].number,
      label: numbers[service].label
    });
  };

  // Video download
  const handleDownloadVideo = () => {
    const link = document.createElement('a');
    link.href = recordingUrl || '/dashcam_stream.jpg';
    link.download = `maaya_incident_evidence_${incident?.incidentId || 'MAA-2025'}.jpg`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  // Resolve emergency toggle for responder verification
  const handleToggleResolve = async () => {
    if (!incident) return;
    if (incident.status === 'ACTIVE') {
      try {
        await resolveIncident(incident.incidentId, 'Resolved via Emergency Web Dashboard');
      } catch (_) {}
      setIncident((prev) => (prev ? { ...prev, status: 'RESOLVED', resolvedAt: Date.now() } : null));
    } else {
      setIncident((prev) => (prev ? { ...prev, status: 'ACTIVE', resolvedAt: undefined } : null));
    }
  };

  if (loading && !incident) {
    return (
      <div className="min-h-screen bg-[#EAF6FF] flex items-center justify-center p-6 text-center">
        <div className="flex flex-col items-center space-y-4">
          <div className="w-12 h-12 rounded-full border-4 border-[#7552E8] border-t-transparent animate-spin" />
          <p className="text-sm font-bold text-[#17233F]">Loading Maaya Emergency Live Dashboard...</p>
        </div>
      </div>
    );
  }

  const currentStatus = incident?.status || 'ACTIVE';

  return (
    <div className="min-h-screen bg-[#EAF6FF] flex justify-center items-center sm:p-4 select-none">
      {/* Mobile Device Frame Container (Width 390px - 440px on desktop, 100% on mobile) */}
      <div className="relative w-full sm:max-w-[430px] h-[100dvh] sm:h-[890px] sm:max-h-[92vh] bg-white sm:rounded-[44px] shadow-[0_20px_60px_rgba(15,23,42,0.18)] overflow-hidden flex flex-col border sm:border-slate-300/80">
        {/* iOS-Style Status Bar (9:41, Cellular, Wifi, Battery) */}
        <div className="absolute top-0 inset-x-0 h-10 px-6 flex items-center justify-between text-slate-900 text-xs font-semibold z-20 pointer-events-none">
          <span className="font-bold tracking-tight text-[13px]">9:41</span>
          <div className="flex items-center space-x-1.5 text-slate-800">
            <Signal className="w-3.5 h-3.5" />
            <Wifi className="w-3.5 h-3.5" />
            <Battery className="w-4 h-4 fill-slate-800" />
          </div>
        </div>

        {/* Floating Maaya Logo & Incident Status Header Card & 3 Circular Buttons */}
        <FloatingIncidentHeader
          incidentId={incident?.incidentId || 'MAA-2025-0315-001'}
          status={currentStatus}
          durationText={durationText}
          onOpenGoogleMaps={handleOpenGoogleMaps}
          onShowFullPath={handleShowFullPath}
          onRecenter={handleRecenter}
        />

        {/* Full-Screen Interactive Leaflet Map */}
        <div className="relative flex-1 w-full h-full">
          <LiveEmergencyMap
            latitude={incident?.currentLatitude || 28.6280}
            longitude={incident?.currentLongitude || 77.3780}
            history={locationHistory}
            status={currentStatus}
            onMapReady={(map) => {
              mapInstanceRef.current = map;
            }}
          />
        </div>

        {/* Draggable Live Metrics Panel & Video/Audio Bottom Sheet */}
        <DraggableMetricsSheet
          heartRate={incident?.heartRateBpm ?? 78}
          spo2={98}
          battery={incident?.batteryPercentage ?? 67}
          stressLevel="Low"
          durationText={durationText}
          isRecording={currentStatus === 'ACTIVE'}
          recordingUrl={recordingUrl || incident?.recordingUrl || null}
          liveFrame={liveFrame}
          audioLevel={audioLevel}
          onCallService={handleCallService}
          onDownloadVideo={handleDownloadVideo}
          isExpanded={isSheetExpanded}
          onToggleExpanded={() => setIsSheetExpanded((prev) => !prev)}
        />

      </div>

      {/* Responder Status Switcher (Outside Phone Frame for Testing/Admin) */}
      <div className="hidden sm:block fixed bottom-3 left-1/2 -translate-x-1/2 z-30">
        <button
          onClick={handleToggleResolve}
          className={`px-4 py-1.5 rounded-full text-xs font-bold border shadow-md transition ${
            currentStatus === 'ACTIVE'
              ? 'bg-white text-rose-600 border-rose-200 hover:bg-rose-50'
              : 'bg-white text-emerald-600 border-emerald-200 hover:bg-emerald-50'
          }`}
          title="Toggle between Emergency Active and Emergency Resolved status"
        >
          Responder Mode: {currentStatus === 'ACTIVE' ? 'Simulate Resolve Incident' : 'Reactivate Emergency'}
        </button>
      </div>

      {/* Emergency Call Modal */}
      {activeCallModal && (
        <div className="fixed inset-0 z-50 bg-black/60 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-white rounded-3xl p-6 max-w-sm w-full shadow-2xl text-center space-y-4">
            <div className="w-14 h-14 rounded-full bg-blue-50 text-blue-600 mx-auto flex items-center justify-center">
              <PhoneCall className="w-7 h-7" />
            </div>
            <div>
              <h3 className="text-lg font-bold text-slate-900">Emergency Dispatch</h3>
              <p className="text-xs text-slate-500 mt-1">{activeCallModal.label}</p>
            </div>
            <div className="text-2xl font-black text-blue-600 font-mono tracking-wider">
              {activeCallModal.number}
            </div>
            <div className="flex space-x-3 pt-2">
              <button
                onClick={() => setActiveCallModal(null)}
                className="flex-1 py-2.5 px-4 rounded-xl border border-slate-200 text-slate-700 font-semibold text-sm hover:bg-slate-50"
              >
                Cancel
              </button>
              <a
                href={`tel:${activeCallModal.number}`}
                className="flex-1 py-2.5 px-4 rounded-xl bg-blue-600 text-white font-bold text-sm hover:bg-blue-700 flex items-center justify-center space-x-1"
              >
                <span>Call Now</span>
              </a>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
