export interface IncidentData {
  incidentId: string;
  triggerType: string;
  status: string;
  initialLatitude: number;
  initialLongitude: number;
  currentLatitude: number;
  currentLongitude: number;
  accuracyMeters: number;
  heartRateBpm: number | null;
  batteryPercentage: number | null;
  dashboardAccessToken: string;
  dashboardViewerUrl: string;
  streamToken: string;
  notes?: string;
  createdAt: number;
  resolvedAt?: number;
  recordingUrl?: string;
  locationHistory?: Array<{
    latitude: number;
    longitude: number;
    accuracy: number;
    heartRateBpm?: number;
    batteryPercentage?: number;
    timestamp: number;
  }>;
}

export const BACKEND_URL: string =
  ((import.meta as any).env?.VITE_BACKEND_URL as string) ||
  (typeof window !== 'undefined' &&
   window.location.hostname !== 'localhost' &&
   window.location.hostname !== '127.0.0.1' &&
   !window.location.hostname.startsWith('192.168.')
    ? 'https://maaya-nrba.onrender.com'
    : `http://${(typeof window !== 'undefined' && window.location.hostname) ? window.location.hostname : 'localhost'}:8088`);

export const host = (typeof window !== 'undefined' && window.location.hostname) ? window.location.hostname : 'localhost';
export const API_BASE = `${BACKEND_URL.replace(/\/+$/, '')}/api`;

export async function fetchIncident(incidentId: string, token: string): Promise<IncidentData> {
  const res = await fetch(`${API_BASE}/incidents/${incidentId}?token=${encodeURIComponent(token)}`);
  if (!res.ok) {
    throw new Error(`Failed to fetch incident (${res.status})`);
  }
  return res.json();
}

export async function fetchAllIncidents(): Promise<IncidentData[]> {
  try {
    const res = await fetch(`${API_BASE}/incidents`);
    if (!res.ok) return [];
    return await res.json();
  } catch {
    return [];
  }
}

export async function resolveIncident(incidentId: string, reason: string): Promise<void> {
  const res = await fetch(`${API_BASE}/incidents/resolve`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ incidentId, resolutionReason: reason }),
  });
  if (!res.ok) {
    throw new Error('Failed to resolve incident');
  }
}
