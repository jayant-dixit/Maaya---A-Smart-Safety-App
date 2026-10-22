import React from 'react';
import { Clock, Route, Compass, MapPin } from 'lucide-react';

interface Props {
  incidentId: string;
  status: string;
  durationText: string;
  onOpenGoogleMaps: () => void;
  onShowFullPath: () => void;
  onRecenter: () => void;
}

export const FloatingIncidentHeader: React.FC<Props> = ({
  incidentId,
  status,
  durationText,
  onOpenGoogleMaps,
  onShowFullPath,
  onRecenter
}) => {
  const isResolved = status === 'RESOLVED';

  // Format long UUIDs cleanly (e.g. #bc28d9d2) so they don't break layout
  const displayId = incidentId.length > 20
    ? `#${incidentId.replace('#', '').slice(0, 8)}`
    : incidentId.startsWith('#')
    ? incidentId
    : `#${incidentId}`;

  return (
    <div className="absolute top-0 inset-x-0 z-20 p-3 pt-3 pointer-events-none">
      <div className="flex items-start justify-between w-full max-w-lg mx-auto">
        {/* Left: Maaya Brand & Floating Status Card */}
        <div className="space-y-1.5 pointer-events-auto">
          {/* Brand Logo */}
          <div className="flex items-center space-x-1 pl-0.5">
            <span className="text-xl font-black tracking-tight text-[#17233F]">maaya</span>
            <svg className="w-4 h-4 text-[#7552E8] fill-current" viewBox="0 0 24 24">
              <path d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm0 9.5c-1.38 0-2.5-1.12-2.5-2.5s1.12-2.5 2.5-2.5 2.5 1.12 2.5 2.5-1.12 2.5-2.5 2.5z"/>
            </svg>
          </div>

          {/* Floating White Incident Card */}
          <div className="bg-white/95 backdrop-blur-md border border-slate-200/90 rounded-2xl shadow-[0_4px_16px_rgba(0,0,0,0.1)] p-3 pr-4 max-w-[210px]">
            {/* Status Indicator */}
            <div className="flex items-center space-x-1.5">
              <span
                className={`w-2 h-2 rounded-full ${
                  isResolved ? 'bg-slate-400' : 'bg-emerald-500 animate-pulse'
                }`}
              />
              <span
                className={`text-xs font-bold ${
                  isResolved ? 'text-slate-600' : 'text-purple-600'
                }`}
              >
                {isResolved ? 'Emergency Resolved' : 'Emergency Active'}
              </span>
            </div>

            {/* Incident ID */}
            <div className="text-[11px] text-slate-500 font-medium mt-1 truncate">
              Incident ID <span className="text-slate-800 font-bold">{displayId}</span>
            </div>

            {/* Duration */}
            <div className="flex items-center space-x-1 text-[11px] text-slate-500 font-medium mt-0.5">
              <Clock className="w-3 h-3 text-slate-400" />
              <span>Duration</span>
              <span className="text-slate-900 font-bold font-mono text-[11.5px] ml-0.5">
                {durationText}
              </span>
            </div>
          </div>
        </div>

        {/* Right: 3 Floating Circular Map Controls */}
        <div className="flex items-center space-x-2 pt-1 pointer-events-auto">
          {/* 1. Open in Maps */}
          <button
            onClick={onOpenGoogleMaps}
            className="flex flex-col items-center group active:scale-95 transition"
            title="Open in Google Maps"
          >
            <div className="w-10 h-10 bg-white rounded-full shadow-[0_3px_12px_rgba(0,0,0,0.12)] border border-slate-200/90 flex items-center justify-center group-hover:bg-slate-50 transition">
              <div className="relative flex items-center justify-center">
                <MapPin className="w-4 h-4 text-emerald-500 fill-emerald-500" />
                <div className="absolute w-1.5 h-1.5 rounded-full bg-amber-400 -top-0.5 right-0.5" />
              </div>
            </div>
            <span className="text-[9.5px] font-bold text-slate-800 mt-1 leading-tight text-center">
              Open in<br />Maps
            </span>
          </button>

          {/* 2. Full Path */}
          <button
            onClick={onShowFullPath}
            className="flex flex-col items-center group active:scale-95 transition"
            title="Show Full Route Path"
          >
            <div className="w-10 h-10 bg-white rounded-full shadow-[0_3px_12px_rgba(0,0,0,0.12)] border border-slate-200/90 flex items-center justify-center group-hover:bg-slate-50 transition">
              <Route className="w-4 h-4 text-blue-600" />
            </div>
            <span className="text-[9.5px] font-bold text-slate-800 mt-1 leading-tight text-center">
              Full<br />Path
            </span>
          </button>

          {/* 3. Recenter */}
          <button
            onClick={onRecenter}
            className="flex flex-col items-center group active:scale-95 transition"
            title="Recenter Map Camera"
          >
            <div className="w-10 h-10 bg-white rounded-full shadow-[0_3px_12px_rgba(0,0,0,0.12)] border border-slate-200/90 flex items-center justify-center group-hover:bg-slate-50 transition">
              <Compass className="w-4 h-4 text-slate-700" />
            </div>
            <span className="text-[9.5px] font-bold text-slate-800 mt-1 leading-tight text-center">
              Recenter<br />&nbsp;
            </span>
          </button>
        </div>
      </div>
    </div>
  );
};
