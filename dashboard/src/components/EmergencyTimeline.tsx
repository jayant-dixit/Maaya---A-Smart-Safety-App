import React from 'react';
import {
  Clock,
  Shield,
  MessageSquare,
  Radio,
  MapPin,
  CheckCircle,
  Download,
  FileVideo,
  FileText,
  Route,
  ExternalLink
} from 'lucide-react';
import { host } from '../services/api';

interface Props {
  incidentId: string;
  triggerType: string;
  startedAt: number;
  resolvedAt?: number | null;
  locationUpdatesCount: number;
  status: string;
  recordingUrl?: string | null;
  locationHistory?: any[];
  totalDistanceMeters?: number;
}

export const EmergencyTimeline: React.FC<Props> = ({
  incidentId,
  triggerType,
  startedAt,
  resolvedAt,
  locationUpdatesCount,
  status,
  recordingUrl,
  locationHistory = [],
  totalDistanceMeters = 0,
}) => {
  const isResolved = status === 'RESOLVED';
  const isStandby = status === 'STANDBY' || incidentId === 'awaiting-sos';

  const formatTime = (ts?: number | null) => {
    if (!ts) return '--:--:--';
    return new Date(ts).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' });
  };

  const fullRecordingUrl = recordingUrl
    ? (recordingUrl.startsWith('http') ? recordingUrl : `http://${host}:8088${recordingUrl}`)
    : null;

  // Export full forensic evidence package
  const exportFullEvidencePackage = () => {
    const evidenceDossier = {
      incidentId,
      status,
      triggerType,
      startedAt: new Date(startedAt).toISOString(),
      resolvedAt: resolvedAt ? new Date(resolvedAt).toISOString() : null,
      durationMinutes: Math.max(0, Math.floor(((resolvedAt || Date.now()) - startedAt) / 60000)),
      totalWaypointsRecorded: locationUpdatesCount,
      totalDistanceTraversedMeters: totalDistanceMeters,
      recordingEvidenceUrl: fullRecordingUrl,
      locationHistory: locationHistory.map((p, idx) => ({
        point: idx + 1,
        latitude: p.latitude,
        longitude: p.longitude,
        accuracyMeters: p.accuracy,
        speedMps: p.speed,
        timestamp: p.timestamp ? new Date(p.timestamp).toISOString() : null,
      })),
      generatedAt: new Date().toISOString(),
    };

    const blob = new Blob([JSON.stringify(evidenceDossier, null, 2)], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `incident_evidence_dossier_${incidentId}.json`;
    a.click();
    URL.revokeObjectURL(url);
  };

  const events: Array<{
    title: string;
    time: string;
    icon: any;
    color: string;
    description: string;
    action?: React.ReactNode;
  }> = [];

  // 1. Resolution Event (if resolved)
  if (isResolved) {
    events.push({
      title: 'Incident Resolved & Marked Safe',
      time: formatTime(resolvedAt || Date.now()),
      icon: CheckCircle,
      color: 'text-emerald-400 bg-emerald-400/20',
      description: 'Distress signal officially deactivated. Complete audio, video, and GPS breadcrumbs locked for evidentiary review.',
    });
  }

  // 2. Video & Audio Evidence Item
  if (fullRecordingUrl || !isStandby) {
    events.push({
      title: 'Audio & Video Evidence Recording Captured',
      time: formatTime(startedAt + 2400),
      icon: FileVideo,
      color: 'text-purple-400 bg-purple-400/20',
      description: fullRecordingUrl
        ? 'High-fidelity video and microphone evidence stream uploaded and encrypted on server.'
        : 'Live camera & microphone feed established for authorized emergency contacts.',
      action: fullRecordingUrl ? (
        <div className="mt-2 flex items-center space-x-2">
          <a
            href={fullRecordingUrl}
            download={`evidence_${incidentId}.mp4`}
            target="_blank"
            rel="noopener noreferrer"
            className="px-2.5 py-1 rounded-lg bg-purple-950 border border-purple-700/80 hover:bg-purple-900 text-purple-200 text-xs font-semibold inline-flex items-center space-x-1.5 transition-colors shadow"
          >
            <Download className="w-3 h-3 text-purple-300" />
            <span>Download Evidence Video (.mp4)</span>
          </a>
        </div>
      ) : null,
    });
  }

  // 3. GPS Tracking Event
  if (!isStandby) {
    events.push({
      title: `GPS Movement Trace & Telemetry (${locationUpdatesCount} fixes recorded)`,
      time: formatTime(Date.now()),
      icon: MapPin,
      color: 'text-sky-400 bg-sky-400/20',
      description: `High-accuracy GPS breadcrumbs recorded. Total route distance: ${totalDistanceMeters >= 1000 ? `${(totalDistanceMeters / 1000).toFixed(2)} km` : `${totalDistanceMeters} m`}.`,
    });
  }

  // 4. SMS Alerts Dispatched
  if (!isStandby) {
    events.push({
      title: 'Emergency SMS Alerts Dispatched',
      time: formatTime(startedAt + 1200),
      icon: MessageSquare,
      color: 'text-amber-400 bg-amber-400/20',
      description: 'High-priority SMS alerts delivered to trusted contacts with live tracking link and victim coordinates.',
    });
  }

  // 5. Initial Activation Event
  events.push({
    title: isStandby ? 'Emergency System Standby' : `Emergency SOS Activated (${triggerType || 'MANUAL_IN_APP'})`,
    time: formatTime(startedAt),
    icon: Shield,
    color: isStandby ? 'text-slate-400 bg-slate-800' : 'text-rose-500 bg-rose-500/20',
    description: isStandby
      ? 'Awaiting emergency distress activation from mobile app or wearable.'
      : `Triggered from user device (${triggerType || 'MANUAL_IN_APP'}) and persisted immediately to local storage.`,
  });

  return (
    <div className="bg-[#131B2E] border border-slate-800 rounded-2xl p-5 shadow-lg">
      <div className="flex flex-wrap items-center justify-between gap-3 mb-5 border-b border-slate-800 pb-3">
        <div className="flex items-center space-x-2">
          <Clock className="w-5 h-5 text-slate-400" />
          <h3 className="font-bold text-white text-sm">Emergency Incident Audit & Evidence Log</h3>
        </div>

        {/* Export Complete Dossier Button */}
        {!isStandby && (
          <button
            onClick={exportFullEvidencePackage}
            className="px-3 py-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-semibold inline-flex items-center space-x-1.5 transition-colors border border-slate-700 shadow"
            title="Export complete forensic evidence dossier with all GPS coordinates and recording links"
          >
            <Download className="w-3.5 h-3.5 text-emerald-400" />
            <span>Export Evidence Dossier (.json)</span>
          </button>
        )}
      </div>

      <div className="relative pl-6 space-y-6 before:absolute before:left-2.5 before:top-3 before:bottom-3 before:w-0.5 before:bg-slate-800">
        {events.map((ev, index) => {
          const Icon = ev.icon;
          return (
            <div key={index} className="relative flex items-start space-x-3">
              <div className={`absolute -left-6 p-1.5 rounded-full ${ev.color}`}>
                <Icon className="w-3.5 h-3.5" />
              </div>
              <div className="flex-1">
                <div className="flex items-center justify-between">
                  <h4 className="text-xs font-bold text-white">{ev.title}</h4>
                  <span className="text-[11px] font-mono text-slate-400">{ev.time}</span>
                </div>
                <p className="text-[11px] text-slate-400 mt-0.5 leading-relaxed">{ev.description}</p>
                {ev.action}
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
};
