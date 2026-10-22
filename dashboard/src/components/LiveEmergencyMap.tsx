import React, { useEffect, useRef } from 'react';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';

interface LocationPoint {
  latitude: number;
  longitude: number;
  timestamp?: number;
  accuracy?: number;
}

interface Props {
  latitude: number;
  longitude: number;
  history?: LocationPoint[];
  status?: string;
  onMapReady?: (map: L.Map) => void;
}

// Default realistic demo route through Noida Sector 62 / Fortis / Botanical Garden matching the mockup
const DEMO_ROUTE_COORDINATES: [number, number][] = [
  [28.5630, 77.3330], // Botanical Garden Metro Area
  [28.5720, 77.3420],
  [28.5850, 77.3550], // Near Golf Course Sector 37
  [28.6000, 77.3620], // NH 24 / Highway junction
  [28.6180, 77.3710], // Near Fortis Hospital Sector 62
  [28.6280, 77.3780], // Sector 62 Noida
  [28.6360, 77.3870]  // Incident Destination (Sector 63 / 62 border)
];

export const LiveEmergencyMap: React.FC<Props> = ({
  latitude,
  longitude,
  history = [],
  status = 'ACTIVE',
  onMapReady
}) => {
  const mapContainerRef = useRef<HTMLDivElement | null>(null);
  const mapInstanceRef = useRef<L.Map | null>(null);
  const userMarkerRef = useRef<L.Marker | null>(null);
  const carMarkerRef = useRef<L.Marker | null>(null);
  const destMarkerRef = useRef<L.Marker | null>(null);
  const routePolylineRef = useRef<L.Polyline | null>(null);
  const routeGlowRef = useRef<L.Polyline | null>(null);

  // Determine active coordinates (fallback to realistic demo coords if 0)
  const hasGps = (latitude !== 0 || longitude !== 0) && !isNaN(latitude) && !isNaN(longitude);
  const currentLat = hasGps ? latitude : 28.5850;
  const currentLon = hasGps ? longitude : 77.3550;

  useEffect(() => {
    if (!mapContainerRef.current) return;

    if (!mapInstanceRef.current) {
      // 1. Initialize Leaflet Map with light, crisp street styling
      const map = L.map(mapContainerRef.current, {
        center: [28.6000, 77.3600],
        zoom: 12.8,
        zoomControl: false,
        attributionControl: false
      });

      // Standard OpenStreetMap tiles (free, fast, zero watermark)
      L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        maxZoom: 19
      }).addTo(map);

      // Reposition zoom controls to bottom-right out of the way of cards
      L.control.zoom({ position: 'bottomright' }).addTo(map);

      mapInstanceRef.current = map;
      if (onMapReady) onMapReady(map);
    }

    const map = mapInstanceRef.current;

    // 2. Prepare Route Polyline
    let routeCoords: [number, number][] = [];
    if (history.length >= 2) {
      routeCoords = history
        .filter((p) => p.latitude !== 0 && p.longitude !== 0)
        .map((p) => [p.latitude, p.longitude] as [number, number]);
    }
    if (routeCoords.length < 2) {
      routeCoords = DEMO_ROUTE_COORDINATES;
    }

    // Glow effect underneath route
    if (routeGlowRef.current) {
      routeGlowRef.current.setLatLngs(routeCoords);
    } else {
      routeGlowRef.current = L.polyline(routeCoords, {
        color: '#7552E8',
        weight: 12,
        opacity: 0.28,
        lineCap: 'round',
        lineJoin: 'round'
      }).addTo(map);
    }

    // Main vibrant purple-blue route polyline
    if (routePolylineRef.current) {
      routePolylineRef.current.setLatLngs(routeCoords);
    } else {
      routePolylineRef.current = L.polyline(routeCoords, {
        color: '#4F46E5',
        weight: 5,
        opacity: 0.95,
        lineCap: 'round',
        lineJoin: 'round'
      }).addTo(map);
    }

    // 3. User Start / Current Location Marker (Blue dot with animated pulse halo)
    const userLocationIcon = L.divIcon({
      className: 'user-pulse-marker-wrapper',
      html: `
        <div class="relative flex items-center justify-center w-12 h-12 -ml-6 -mt-6">
          <div class="absolute w-12 h-12 bg-blue-500/25 rounded-full map-radar-pulse"></div>
          <div class="absolute w-7 h-7 bg-blue-500/40 rounded-full"></div>
          <div class="w-4 h-4 bg-blue-600 rounded-full border-2 border-white shadow-lg z-10"></div>
        </div>
      `,
      iconSize: [48, 48],
      iconAnchor: [24, 24]
    });

    const startPos = routeCoords[0] || [currentLat, currentLon];
    if (userMarkerRef.current) {
      userMarkerRef.current.setLatLng(startPos);
    } else {
      userMarkerRef.current = L.marker(startPos, { icon: userLocationIcon }).addTo(map);
    }

    // 4. White Vehicle Marker (Car along route with purple underglow)
    const midIdx = Math.min(Math.floor(routeCoords.length / 2), routeCoords.length - 1);
    const carPos = routeCoords[midIdx] || [currentLat, currentLon];

    const carIcon = L.divIcon({
      className: 'car-icon-wrapper',
      html: `
        <div class="relative flex items-center justify-center w-10 h-10 -ml-5 -mt-5">
          <div class="absolute w-10 h-10 bg-purple-600/35 rounded-full blur-[3px]"></div>
          <div class="w-8 h-8 bg-white rounded-full border border-purple-200 shadow-md flex items-center justify-center z-10 transform -rotate-45">
            <svg class="w-5 h-5 text-slate-800" viewBox="0 0 24 24" fill="currentColor">
              <path d="M18.92 6.01C18.72 5.42 18.16 5 17.5 5h-11c-.66 0-1.21.42-1.42 1.01L3 12v8c0 .55.45 1 1 1h1c.55 0 1-.45 1-1v-1h12v1c0 .55.45 1 1 1h1c.55 0 1-.45 1-1v-8l-2.08-5.99zM6.85 7h10.29l1.04 3H5.81l1.04-3zM19 17H5v-4.66l.12-.34h13.77l.11.34V17z"/>
              <circle cx="7.5" cy="14.5" r="1.5"/>
              <circle cx="16.5" cy="14.5" r="1.5"/>
            </svg>
          </div>
        </div>
      `,
      iconSize: [40, 40],
      iconAnchor: [20, 20]
    });

    if (carMarkerRef.current) {
      carMarkerRef.current.setLatLng(carPos);
    } else {
      carMarkerRef.current = L.marker(carPos, { icon: carIcon }).addTo(map);
    }

    // 5. Incident Destination Pin (Red/Coral pin at end of trip)
    const endPos = routeCoords[routeCoords.length - 1] || [currentLat, currentLon];
    const destinationIcon = L.divIcon({
      className: 'destination-pin-wrapper',
      html: `
        <div class="relative flex flex-col items-center -ml-5 -mt-10">
          <div class="w-10 h-10 bg-rose-500 rounded-full flex items-center justify-center text-white shadow-lg border-2 border-white">
            <svg class="w-5 h-5" viewBox="0 0 24 24" fill="currentColor">
              <path d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm0 9.5c-1.38 0-2.5-1.12-2.5-2.5s1.12-2.5 2.5-2.5 2.5 1.12 2.5 2.5-1.12 2.5-2.5 2.5z"/>
            </svg>
          </div>
          <div class="w-2.5 h-2.5 bg-rose-600 rounded-full -mt-1 blur-[1px]"></div>
        </div>
      `,
      iconSize: [40, 40],
      iconAnchor: [20, 40]
    });

    if (destMarkerRef.current) {
      destMarkerRef.current.setLatLng(endPos);
    } else {
      destMarkerRef.current = L.marker(endPos, { icon: destinationIcon }).addTo(map);
    }

  }, [latitude, longitude, history, status, onMapReady]);

  return (
    <div className="absolute inset-0 w-full h-full z-0">
      <div ref={mapContainerRef} className="w-full h-full" />
    </div>
  );
};
