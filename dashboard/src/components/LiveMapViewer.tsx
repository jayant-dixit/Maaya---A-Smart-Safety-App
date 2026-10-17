import React, { useEffect, useRef, useState, useMemo } from 'react';
import L from 'leaflet';
import {
  MapPin,
  ExternalLink,
  Copy,
  Check,
  Locate,
  Layers,
  Route,
  Play,
  RotateCcw,
  Compass,
  Footprints,
  Download,
  ShieldCheck,
  CheckCircle2
} from 'lucide-react';

interface LocationPoint {
  latitude: number;
  longitude: number;
  timestamp?: number;
  accuracy?: number;
  speed?: number | null;
  altitude?: number | null;
}

interface Props {
  latitude: number;
  longitude: number;
  accuracyMeters: number;
  history: LocationPoint[];
  status?: string;
}

// Fallback regional center when GPS fix is pending
const FALLBACK_LAT = 28.6139;
const FALLBACK_LON = 77.2090;

export const LiveMapViewer: React.FC<Props> = ({
  latitude,
  longitude,
  accuracyMeters,
  history = [],
  status = 'ACTIVE',
}) => {
  const mapContainerRef = useRef<HTMLDivElement | null>(null);
  const mapInstanceRef = useRef<L.Map | null>(null);
  const tileLayerRef = useRef<L.TileLayer | null>(null);
  const currentMarkerRef = useRef<L.Marker | null>(null);
  const startMarkerRef = useRef<L.Marker | null>(null);
  const accuracyCircleRef = useRef<L.Circle | null>(null);
  const tracePolylineGlowRef = useRef<L.Polyline | null>(null);
  const tracePolylineMainRef = useRef<L.Polyline | null>(null);
  const waypointLayerRef = useRef<L.LayerGroup | null>(null);

  const [copied, setCopied] = useState(false);
  const [mapStyle, setMapStyle] = useState<'streets' | 'dark'>('streets');
  const [autoCenter, setAutoCenter] = useState(true);
  const [simulatedPoints, setSimulatedPoints] = useState<LocationPoint[]>([]);
  const [isSimulating, setIsSimulating] = useState(false);
  const simulationTimerRef = useRef<any>(null);

  const isResolved = status === 'RESOLVED';
  const hasValidCoordinates = (latitude !== 0 || longitude !== 0) && !isNaN(latitude) && !isNaN(longitude);

  const activeLat = hasValidCoordinates ? latitude : FALLBACK_LAT;
  const activeLon = hasValidCoordinates ? longitude : FALLBACK_LON;

  // Combine real history with any user-triggered simulation points
  const combinedHistory = useMemo(() => {
    let list: LocationPoint[] = [];

    if (history && history.length > 0) {
      list = history.filter((p) => p && p.latitude !== 0 && p.longitude !== 0);
    }

    if (hasValidCoordinates && !list.some((p) => Math.abs(p.latitude - latitude) < 0.000005 && Math.abs(p.longitude - longitude) < 0.000005)) {
      list.push({
        latitude,
        longitude,
        accuracy: accuracyMeters || 8,
        timestamp: Date.now(),
      });
    }

    if (simulatedPoints.length > 0) {
      list = [...list, ...simulatedPoints];
    }

    return list;
  }, [history, latitude, longitude, accuracyMeters, hasValidCoordinates, simulatedPoints]);

  // Current focal coordinates
  const currentFocalPoint = useMemo(() => {
    if (simulatedPoints.length > 0) {
      return simulatedPoints[simulatedPoints.length - 1];
    }
    return {
      latitude: activeLat,
      longitude: activeLon,
      accuracy: accuracyMeters || 8,
    };
  }, [simulatedPoints, activeLat, activeLon, accuracyMeters]);

  // Calculate total path distance in meters
  const totalDistanceMeters = useMemo(() => {
    if (combinedHistory.length < 2) return 0;
    let dist = 0;
    for (let i = 1; i < combinedHistory.length; i++) {
      const p1 = L.latLng(combinedHistory[i - 1].latitude, combinedHistory[i - 1].longitude);
      const p2 = L.latLng(combinedHistory[i].latitude, combinedHistory[i].longitude);
      dist += p1.distanceTo(p2);
    }
    return Math.round(dist);
  }, [combinedHistory]);

  const googleMapsUrl = `https://www.google.com/maps/search/?api=1&query=${currentFocalPoint.latitude},${currentFocalPoint.longitude}`;

  const copyCoordinates = () => {
    if (!hasValidCoordinates) return;
    navigator.clipboard.writeText(`${currentFocalPoint.latitude.toFixed(6)}, ${currentFocalPoint.longitude.toFixed(6)}`);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const exportGpsEvidence = () => {
    const data = {
      incidentType: 'Maaya Shield Emergency GPS Trace Evidence',
      status,
      timestamp: new Date().toISOString(),
      totalWaypoints: combinedHistory.length,
      totalDistanceMeters,
      coordinates: combinedHistory.map((p, idx) => ({
        index: idx + 1,
        latitude: p.latitude,
        longitude: p.longitude,
        accuracyMeters: p.accuracy,
        speed: p.speed,
        timestamp: p.timestamp ? new Date(p.timestamp).toISOString() : null,
      })),
    };
    const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `emergency_gps_trace_evidence_${Date.now()}.json`;
    a.click();
    URL.revokeObjectURL(url);
  };

  // Custom live marker icon (changes style when resolved vs active)
  const createLiveMarkerIcon = () => {
    if (isResolved) {
      return L.divIcon({
        className: 'custom-resolved-marker',
        html: `
          <div style="position: relative; width: 44px; height: 44px; display: flex; align-items: center; justify-content: center; transform: translate(-22px, -22px);">
            <div style="width: 26px; height: 26px; border-radius: 50%; background: #059669; border: 3px solid #ffffff; box-shadow: 0 0 15px rgba(16, 185, 129, 0.9); display: flex; align-items: center; justify-content: center;">
              <div style="width: 8px; height: 8px; border-radius: 50%; background: #ffffff;"></div>
            </div>
            <div style="position: absolute; bottom: -18px; background: #064e3b; color: #a7f3d0; font-size: 9px; font-weight: 800; padding: 1px 5px; border-radius: 4px; border: 1px solid #059669; white-space: nowrap; font-family: monospace; box-shadow: 0 2px 6px rgba(0,0,0,0.5);">
              SAFE / RESOLVED
            </div>
          </div>
        `,
        iconSize: [44, 44],
        iconAnchor: [22, 22],
      });
    }

    return L.divIcon({
      className: 'custom-live-marker',
      html: `
        <div style="position: relative; width: 44px; height: 44px; display: flex; align-items: center; justify-content: center; transform: translate(-22px, -22px);">
          <div style="position: absolute; width: 44px; height: 44px; border-radius: 50%; background: rgba(244, 63, 94, 0.35); animation: ping 1.4s cubic-bezier(0, 0, 0.2, 1) infinite;"></div>
          <div style="position: absolute; width: 26px; height: 26px; border-radius: 50%; background: #e11d48; border: 3px solid #ffffff; box-shadow: 0 0 20px rgba(225, 29, 72, 1); display: flex; align-items: center; justify-content: center;">
            <div style="width: 8px; height: 8px; border-radius: 50%; background: #ffffff;"></div>
          </div>
          <div style="position: absolute; bottom: -18px; background: #991b1b; color: #ffffff; font-size: 9px; font-weight: 800; padding: 1px 5px; border-radius: 4px; border: 1px solid #f43f5e; white-space: nowrap; font-family: monospace; box-shadow: 0 2px 6px rgba(0,0,0,0.5);">
            LIVE
          </div>
        </div>
      `,
      iconSize: [44, 44],
      iconAnchor: [22, 22],
    });
  };

  // Custom start point marker icon
  const createStartMarkerIcon = (timestamp?: number) => {
    return L.divIcon({
      className: 'custom-start-marker',
      html: `
        <div style="position: relative; width: 32px; height: 32px; display: flex; align-items: center; justify-content: center; transform: translate(-16px, -16px);">
          <div style="width: 22px; height: 22px; border-radius: 50%; background: #10b981; border: 2.5px solid #ffffff; box-shadow: 0 0 12px rgba(16, 185, 129, 0.9); display: flex; align-items: center; justify-content: center;">
            <div style="width: 6px; height: 6px; border-radius: 50%; background: #ffffff;"></div>
          </div>
          <div style="position: absolute; top: -18px; background: #064e3b; color: #a7f3d0; font-size: 9px; font-weight: 800; padding: 1px 5px; border-radius: 4px; border: 1px solid #059669; white-space: nowrap; font-family: monospace;">
            START
          </div>
        </div>
      `,
      iconSize: [32, 32],
      iconAnchor: [16, 16],
    });
  };

  // Free OpenStreetMap tiles
  const getTileUrl = () => {
    return 'https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png';
  };

  // Initialize and update Leaflet map
  useEffect(() => {
    if (!mapContainerRef.current) return;

    const targetLat = currentFocalPoint.latitude;
    const targetLon = currentFocalPoint.longitude;

    // 1. Initialize map if not yet created
    if (!mapInstanceRef.current) {
      const map = L.map(mapContainerRef.current, {
        center: [targetLat, targetLon],
        zoom: hasValidCoordinates ? 16 : 13,
        zoomControl: false,
        attributionControl: false,
      });

      // Zoom control
      L.control.zoom({ position: 'topright' }).addTo(map);

      // Attribution
      L.control.attribution({ position: 'bottomright', prefix: false })
        .addAttribution('&copy; <a href="https://www.openstreetmap.org/copyright" target="_blank">OpenStreetMap</a> contributors')
        .addTo(map);

      // Tile layer
      const tile = L.tileLayer(getTileUrl(), {
        maxZoom: 19,
        subdomains: ['a', 'b', 'c'],
      }).addTo(map);
      tileLayerRef.current = tile;

      // Waypoint layer group
      const waypointGroup = L.layerGroup().addTo(map);
      waypointLayerRef.current = waypointGroup;

      // Trace Polylines
      const latLngs = combinedHistory.map((p) => [p.latitude, p.longitude] as [number, number]);

      const polyGlow = L.polyline(latLngs, {
        color: isResolved ? '#10b981' : '#e11d48',
        weight: 9,
        opacity: 0.35,
        lineCap: 'round',
        lineJoin: 'round',
      }).addTo(map);

      const polyMain = L.polyline(latLngs, {
        color: isResolved ? '#059669' : '#f43f5e',
        weight: 4,
        opacity: 0.95,
        dashArray: isResolved ? undefined : '10, 6',
        lineCap: 'round',
        lineJoin: 'round',
      }).addTo(map);

      tracePolylineGlowRef.current = polyGlow;
      tracePolylineMainRef.current = polyMain;

      // Accuracy Circle
      if (hasValidCoordinates) {
        const accCircle = L.circle([targetLat, targetLon], {
          radius: Math.max(currentFocalPoint.accuracy || 10, 8),
          color: isResolved ? '#10b981' : '#f43f5e',
          fillColor: isResolved ? '#10b981' : '#f43f5e',
          fillOpacity: 0.12,
          weight: 1.5,
        }).addTo(map);
        accuracyCircleRef.current = accCircle;

        // Start Marker (if trace has multiple points)
        if (combinedHistory.length > 1) {
          const startPt = combinedHistory[0];
          const startMarker = L.marker([startPt.latitude, startPt.longitude], {
            icon: createStartMarkerIcon(startPt.timestamp),
            zIndexOffset: 100,
          }).addTo(map);
          startMarker.bindPopup(`<b>Emergency SOS Origin</b><br/>${new Date(startPt.timestamp || Date.now()).toLocaleTimeString()}`);
          startMarkerRef.current = startMarker;
        }

        // Live / Resolved Beacon Marker
        const liveMarker = L.marker([targetLat, targetLon], {
          icon: createLiveMarkerIcon(),
          zIndexOffset: 1000,
        }).addTo(map);
        liveMarker.bindPopup(`<b>${isResolved ? 'Resolved Location' : 'Live Distress Beacon'}</b><br/>${targetLat.toFixed(6)}, ${targetLon.toFixed(6)}`);
        currentMarkerRef.current = liveMarker;
      }

      mapInstanceRef.current = map;

      setTimeout(() => {
        map.invalidateSize();
      }, 200);
    } else {
      // 2. Map already initialized: Update layers smoothly
      const map = mapInstanceRef.current;

      // Update focal beacon & accuracy circle
      if (hasValidCoordinates) {
        if (!currentMarkerRef.current) {
          const liveMarker = L.marker([targetLat, targetLon], {
            icon: createLiveMarkerIcon(),
            zIndexOffset: 1000,
          }).addTo(map);
          liveMarker.bindPopup(`<b>${isResolved ? 'Resolved Location' : 'Live Distress Beacon'}</b><br/>${targetLat.toFixed(6)}, ${targetLon.toFixed(6)}`);
          currentMarkerRef.current = liveMarker;
        } else {
          currentMarkerRef.current.setLatLng([targetLat, targetLon]);
          currentMarkerRef.current.setIcon(createLiveMarkerIcon());
        }

        if (!accuracyCircleRef.current) {
          const accCircle = L.circle([targetLat, targetLon], {
            radius: Math.max(currentFocalPoint.accuracy || 10, 8),
            color: isResolved ? '#10b981' : '#f43f5e',
            fillColor: isResolved ? '#10b981' : '#f43f5e',
            fillOpacity: 0.12,
            weight: 1.5,
          }).addTo(map);
          accuracyCircleRef.current = accCircle;
        } else {
          accuracyCircleRef.current.setLatLng([targetLat, targetLon]);
          accuracyCircleRef.current.setRadius(Math.max(currentFocalPoint.accuracy || 10, 8));
        }
      }

      // Update Trace Polylines
      const latLngs = combinedHistory.map((p) => [p.latitude, p.longitude] as [number, number]);
      if (tracePolylineGlowRef.current) {
        tracePolylineGlowRef.current.setLatLngs(latLngs);
        tracePolylineGlowRef.current.setStyle({ color: isResolved ? '#10b981' : '#e11d48' });
      }
      if (tracePolylineMainRef.current) {
        tracePolylineMainRef.current.setLatLngs(latLngs);
        tracePolylineMainRef.current.setStyle({
          color: isResolved ? '#059669' : '#f43f5e',
          dashArray: isResolved ? undefined : '10, 6',
        });
      }

      // Update Start Marker
      if (combinedHistory.length > 1) {
        const startPt = combinedHistory[0];
        if (!startMarkerRef.current) {
          const startMarker = L.marker([startPt.latitude, startPt.longitude], {
            icon: createStartMarkerIcon(startPt.timestamp),
            zIndexOffset: 100,
          }).addTo(map);
          startMarker.bindPopup(`<b>Emergency SOS Origin</b><br/>${new Date(startPt.timestamp || Date.now()).toLocaleTimeString()}`);
          startMarkerRef.current = startMarker;
        } else {
          startMarkerRef.current.setLatLng([startPt.latitude, startPt.longitude]);
        }
      }

      // Update Waypoint dots
      if (waypointLayerRef.current) {
        waypointLayerRef.current.clearLayers();
        if (combinedHistory.length > 2) {
          combinedHistory.slice(1, -1).forEach((pt, idx) => {
            const circle = L.circleMarker([pt.latitude, pt.longitude], {
              radius: 4,
              color: isResolved ? '#059669' : '#f43f5e',
              fillColor: '#ffffff',
              fillOpacity: 0.95,
              weight: 2,
            });
            const timeFormatted = pt.timestamp ? new Date(pt.timestamp).toLocaleTimeString() : `Point #${idx + 1}`;
            circle.bindPopup(`
              <div style="font-family: sans-serif; font-size: 11px;">
                <strong style="color: #e11d48;">Waypoint Fix #${idx + 1}</strong><br/>
                <span>Time: ${timeFormatted}</span><br/>
                ${pt.speed ? `<span>Speed: ${(pt.speed * 3.6).toFixed(1)} km/h</span><br/>` : ''}
                <span>Coords: ${pt.latitude.toFixed(5)}, ${pt.longitude.toFixed(5)}</span>
              </div>
            `);
            waypointLayerRef.current?.addLayer(circle);
          });
        }
      }

      // Auto-center on moving beacon if tracking mode is active
      if (autoCenter && hasValidCoordinates) {
        map.panTo([targetLat, targetLon], { animate: true, duration: 0.8 });
      }
    }
  }, [currentFocalPoint, combinedHistory, autoCenter, hasValidCoordinates, isResolved]);

  // Clean up on unmount
  useEffect(() => {
    return () => {
      if (simulationTimerRef.current) {
        clearInterval(simulationTimerRef.current);
      }
      if (mapInstanceRef.current) {
        mapInstanceRef.current.remove();
        mapInstanceRef.current = null;
      }
    };
  }, []);

  const handleRecenter = () => {
    setAutoCenter(true);
    if (mapInstanceRef.current && hasValidCoordinates) {
      mapInstanceRef.current.flyTo([currentFocalPoint.latitude, currentFocalPoint.longitude], 17, { duration: 1 });
    }
  };

  const handleFitTrace = () => {
    if (mapInstanceRef.current && combinedHistory.length > 0) {
      setAutoCenter(false);
      const bounds = L.latLngBounds(combinedHistory.map((p) => [p.latitude, p.longitude]));
      mapInstanceRef.current.fitBounds(bounds, { padding: [50, 50], maxZoom: 18 });
    }
  };

  // Simulation test tool
  const toggleSimulation = () => {
    if (isSimulating) {
      if (simulationTimerRef.current) clearInterval(simulationTimerRef.current);
      setIsSimulating(false);
      return;
    }

    setIsSimulating(true);
    setAutoCenter(true);

    const baseLat = activeLat;
    const baseLon = activeLon;

    const routeOffsets = [
      { dLat: 0.00030, dLon: 0.00035 },
      { dLat: 0.00065, dLon: 0.00075 },
      { dLat: 0.00095, dLon: 0.00115 },
      { dLat: 0.00130, dLon: 0.00140 },
      { dLat: 0.00170, dLon: 0.00160 },
      { dLat: 0.00210, dLon: 0.00190 },
      { dLat: 0.00245, dLon: 0.00225 },
    ];

    let step = 0;
    simulationTimerRef.current = setInterval(() => {
      if (step >= routeOffsets.length) {
        clearInterval(simulationTimerRef.current);
        setIsSimulating(false);
        return;
      }

      const offset = routeOffsets[step];
      const newPt: LocationPoint = {
        latitude: baseLat + offset.dLat,
        longitude: baseLon + offset.dLon,
        accuracy: 6,
        speed: 1.4,
        timestamp: Date.now(),
      };

      setSimulatedPoints((prev) => [...prev, newPt]);
      step++;
    }, 2000);
  };

  const resetSimulation = () => {
    if (simulationTimerRef.current) clearInterval(simulationTimerRef.current);
    setIsSimulating(false);
    setSimulatedPoints([]);
    handleRecenter();
  };

  return (
    <div className="bg-[#131B2E] border border-slate-800 rounded-2xl overflow-hidden flex flex-col h-full shadow-xl">
      {/* Top Map Toolbar */}
      <div className="p-3.5 border-b border-slate-800 flex flex-wrap items-center justify-between gap-2 bg-[#0E1524]">
        <div className="flex items-center space-x-2">
          <div
            className={`w-7 h-7 rounded-lg flex items-center justify-center ${
              isResolved
                ? 'bg-emerald-500/20 border border-emerald-500/30'
                : 'bg-rose-500/20 border border-rose-500/30'
            }`}
          >
            {isResolved ? (
              <CheckCircle2 className="w-4 h-4 text-emerald-400" />
            ) : (
              <Route className="w-4 h-4 text-rose-400 animate-pulse" />
            )}
          </div>
          <div>
            <div className="flex items-center space-x-2">
              <span className="font-bold text-white text-sm">
                {isResolved ? 'Archived GPS Route & Movement Trace' : 'Live Location & Movement Trace'}
              </span>
              <span
                className={`inline-flex items-center px-1.5 py-0.5 rounded text-[10px] font-semibold ${
                  isResolved
                    ? 'bg-emerald-500/20 text-emerald-400 border border-emerald-500/30'
                    : hasValidCoordinates
                    ? 'bg-rose-500/20 text-rose-400 border border-rose-500/30'
                    : 'bg-slate-800 text-slate-400 border border-slate-700'
                }`}
              >
                {isResolved ? 'ARCHIVED EVIDENCE' : hasValidCoordinates ? 'ACTIVE TRACE' : 'SEARCHING GPS'}
              </span>
            </div>
            <p className="text-[11px] text-slate-400">
              {hasValidCoordinates
                ? `High-accuracy telemetry • ${combinedHistory.length} recorded waypoints`
                : 'Awaiting initial satellite fix from mobile device'}
            </p>
          </div>
        </div>

        <div className="flex items-center flex-wrap space-x-1.5 sm:space-x-2">
          {/* Export GPS Evidence */}
          {combinedHistory.length > 0 && (
            <button
              onClick={exportGpsEvidence}
              className="px-2.5 py-1.5 rounded-lg bg-emerald-950/80 border border-emerald-700/80 hover:bg-emerald-900 text-emerald-300 text-xs font-medium inline-flex items-center space-x-1 transition-colors"
              title="Download GPS trace file as legal/police evidence"
            >
              <Download className="w-3.5 h-3.5 text-emerald-400" />
              <span>Store GPS Evidence</span>
            </button>
          )}

          {/* Fit Path Trace Button */}
          {combinedHistory.length > 1 && (
            <button
              onClick={handleFitTrace}
              className="px-2.5 py-1.5 rounded-lg bg-slate-900 border border-slate-700/80 hover:bg-slate-800 text-slate-200 text-xs font-medium inline-flex items-center space-x-1 transition-colors"
              title="Fit entire trajectory in viewport"
            >
              <Route className="w-3.5 h-3.5 text-rose-400" />
              <span>Full Path</span>
            </button>
          )}

          {/* Recenter Button */}
          {hasValidCoordinates && (
            <button
              onClick={handleRecenter}
              className={`px-2.5 py-1.5 rounded-lg text-xs font-medium inline-flex items-center space-x-1 transition-colors border ${
                autoCenter
                  ? 'bg-rose-950/80 text-rose-300 border-rose-700'
                  : 'bg-slate-900 text-slate-300 border-slate-700 hover:bg-slate-800'
              }`}
              title="Lock map view on victim location"
            >
              <Locate className="w-3.5 h-3.5 text-rose-400" />
              <span>{autoCenter ? 'Tracking' : 'Recenter'}</span>
            </button>
          )}

          {/* Toggle Map Style */}
          <button
            onClick={() => setMapStyle((prev) => (prev === 'streets' ? 'dark' : 'streets'))}
            className="p-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs inline-flex items-center space-x-1 transition-colors"
            title="Toggle Light / Dark Tactical Map"
          >
            <Layers className="w-3.5 h-3.5" />
          </button>

          {/* Copy Coordinates */}
          {hasValidCoordinates && (
            <button
              onClick={copyCoordinates}
              className="p-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs inline-flex items-center space-x-1 transition-colors"
              title="Copy coordinates"
            >
              {copied ? <Check className="w-3.5 h-3.5 text-emerald-400" /> : <Copy className="w-3.5 h-3.5" />}
            </button>
          )}

          {/* Open Google Maps */}
          {hasValidCoordinates && (
            <a
              href={googleMapsUrl}
              target="_blank"
              rel="noopener noreferrer"
              className="px-2.5 py-1.5 rounded-lg bg-rose-600 hover:bg-rose-700 text-white text-xs font-semibold inline-flex items-center space-x-1 transition-colors shadow"
            >
              <span>Google Maps</span>
              <ExternalLink className="w-3.5 h-3.5" />
            </a>
          )}
        </div>
      </div>

      {/* Main Map Viewport Container */}
      <div className="relative flex-1 min-h-[380px] bg-slate-950 flex flex-col overflow-hidden">
        {/* Leaflet DOM Node - ALWAYS rendered */}
        <div
          ref={mapContainerRef}
          className={`w-full h-full min-h-[380px] z-0 transition-all ${
            mapStyle === 'dark' ? 'dark-tactical-map' : ''
          }`}
        />

        {/* Top-Left Floating Tactical Telemetry HUD */}
        <div className="absolute top-3 left-3 z-[400] bg-[#090D16]/90 backdrop-blur-md border border-slate-700/80 rounded-xl px-3.5 py-2.5 shadow-2xl pointer-events-none">
          {hasValidCoordinates ? (
            <>
              <div className="flex items-center space-x-2">
                <span
                  className={`w-2.5 h-2.5 rounded-full ${
                    isResolved ? 'bg-emerald-400' : 'bg-rose-500 animate-ping'
                  }`}
                />
                <p className="text-xs font-mono font-bold text-white tracking-wide">
                  {currentFocalPoint.latitude.toFixed(6)}° N, {currentFocalPoint.longitude.toFixed(6)}° E
                </p>
              </div>
              <div className="flex items-center space-x-2 mt-1 text-[11px] text-slate-300 font-mono">
                <span className="text-emerald-400 font-semibold">
                  Fix: ±{Math.round(currentFocalPoint.accuracy || 8)}m
                </span>
                <span className="text-slate-600">•</span>
                <span className={isResolved ? 'text-emerald-300' : 'text-rose-300'}>
                  {combinedHistory.length} breadcrumb fixes
                </span>
              </div>
            </>
          ) : (
            <div className="flex items-center space-x-2 text-xs text-slate-400">
              <Compass className="w-4 h-4 text-rose-400 animate-spin" />
              <span>GPS: Not Available (Listening for signal...)</span>
            </div>
          )}
        </div>

        {/* Bottom-Left Live Trace Statistics HUD */}
        {combinedHistory.length > 0 && (
          <div className="absolute bottom-3 left-3 z-[400] bg-[#090D16]/90 backdrop-blur-md border border-slate-700/80 rounded-xl px-3 py-2 shadow-2xl flex items-center space-x-3 text-xs">
            <div className="flex items-center space-x-1.5">
              <Footprints className={`w-4 h-4 ${isResolved ? 'text-emerald-400' : 'text-rose-400'}`} />
              <span className="font-mono text-slate-200">
                <strong className={isResolved ? 'text-emerald-400' : 'text-rose-400'}>Path Trace:</strong>{' '}
                {totalDistanceMeters >= 1000
                  ? `${(totalDistanceMeters / 1000).toFixed(2)} km`
                  : `${totalDistanceMeters} m`}{' '}
                traversed
              </span>
            </div>
            <span className="text-slate-700">|</span>
            <div className="flex items-center space-x-1 text-slate-300 font-mono text-[11px]">
              <span className={`w-2 h-2 rounded-full ${isResolved ? 'bg-emerald-400' : 'bg-emerald-400'}`} />
              <span>{isResolved ? 'Evidence Sealed' : 'Live Sync'}</span>
            </div>
          </div>
        )}

        {/* Bottom-Right Simulator Tool */}
        <div className="absolute bottom-3 right-3 z-[400] flex items-center space-x-1.5 bg-[#090D16]/90 backdrop-blur-md border border-slate-700/80 rounded-xl p-1 shadow-2xl">
          <button
            id="btn-test-trace-motion"
            onClick={toggleSimulation}
            className={`px-2.5 py-1 rounded-lg text-xs font-medium inline-flex items-center space-x-1 transition-all ${
              isSimulating
                ? 'bg-amber-600 text-white animate-pulse'
                : 'bg-slate-800 hover:bg-slate-700 text-slate-200'
            }`}
            title="Simulate victim walking to test live trace polyline streaming"
          >
            <Play className="w-3 h-3 text-rose-400" />
            <span>{isSimulating ? 'Simulating Trace...' : 'Test Trace Motion'}</span>
          </button>
          {simulatedPoints.length > 0 && (
            <button
              onClick={resetSimulation}
              className="p-1 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 transition-colors"
              title="Reset simulation trace"
            >
              <RotateCcw className="w-3 h-3" />
            </button>
          )}
        </div>
      </div>
    </div>
  );
};
