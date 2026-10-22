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

  return (
    <div className="absolute top-0 inset-x-0 z-10 p-4 pt-10 pointer-events-none">
      <div className="flex items-start justify-between max-w-lg mx-auto">
        {/* Left: Maaya Brand & Incident Status Card */}
        <div className="space-y-2 pointer-events-auto">
          {/* Brand Logo */}
          <div className="flex items-center space-x-1 pl-1">
            <span className="text-2xl font-black tracking-tight text-[#17233F]">maaya</span>
            {/* Custom Purple Location Pin from Mockup */}
            <svg className="w-5 h-5 text-[#7552E8] fill-current" viewBox="0 0 24 24">
              <path d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm0 9.5c-1.38 0-2.5-1.12-2.5-2.5s1.12-2.5 2.5-2.5 2.5 1.12 2.5 2.5-1.12 2.5-2.5 2.5z"/>
            </svg>
          </div>

          {/* Floating White Incident Card */}
          <div className="bg-white/95 backdrop-blur-md border border-slate-100 rounded-2xl shadow-[0_4px_20px_rgba(0,0,0,0.08)] p-3.5 pr-6 min-w-[210px]">
            {/* Status Indicator */}
            <div className="flex items-center space-x-2">
              <span
                className={`w-2.5 h-2.5 rounded-full ${
                  isResolved ? 'bg-slate-400' : 'bg-emerald-500 animate-pulse'
                }`}
              />
              <span
                className={`text-[13px] font-bold ${
                  isResolved ? 'text-slate-600' : 'text-purple-600'
                }`}
              >
                {isResolved ? 'Emergency Resolved' : 'Emergency Active'}
              </span>
            </div>

            {/* Incident ID */}
            <div className="text-[11.5px] text-slate-500 font-medium mt-1">
              Incident ID{' '}
              <span className="text-slate-700 font-semibold">
                {incidentId.startsWith('#') ? incidentId : `#${incidentId}`}
              </span>
            </div>

            {/* Duration */}
            <div className="flex items-center space-x-1.5 text-[11.5px] text-slate-500 font-medium mt-1">
              <Clock className="w-3.5 h-3.5 text-slate-400" />
              <span>Duration</span>
              <span className="text-slate-900 font-bold font-mono text-[12px] ml-1">
                {durationText}
              </span>
            </div>
          </div>
        </div>

        {/* Right: 3 Floating Circular Map Controls */}
        <div className="flex items-center space-x-2.5 pointer-events-auto">
          {/* 1. Open in Maps */}
          <button
            onClick={onOpenGoogleMaps}
            className="flex flex-col items-center group active:scale-95 transition"
            title="Open in Google Maps"
          >
            <div className="w-11 h-11 bg-white rounded-full shadow-[0_4px_14px_rgba(0,0,0,0.1)] border border-slate-100 flex items-center justify-center group-hover:bg-slate-50 transition">
              <div className="relative flex items-center justify-center">
                <MapPin className="w-5 h-5 text-emerald-500 fill-emerald-500" />
                <div className="absolute w-2 h-2 rounded-full bg-amber-400 -top-0.5 right-0.5" />
              </div>
            </div>
            <span className="text-[10px] font-bold text-slate-700 mt-1 leading-tight text-center min-h-[24px] flex items-center">
              Open in Maps
            </span>
          </button>

          {/* 2. Full Path */}
          <button
            onClick={onShowFullPath}
            className="flex flex-col items-center group active:scale-95 transition"
            title="Show Full Route Path"
          >
            <div className="w-11 h-11 bg-white rounded-full shadow-[0_4px_14px_rgba(0,0,0,0.1)] border border-slate-100 flex items-center justify-center group-hover:bg-slate-50 transition">
              <Route className="w-5 h-5 text-blue-600" />
            </div>
            <span className="text-[10px] font-bold text-slate-700 mt-1 leading-tight text-center min-h-[24px] flex items-center">
              Full Path
            </span>
          </button>

          {/* 3. Recenter */}
          <button
            onClick={onRecenter}
            className="flex flex-col items-center group active:scale-95 transition"
            title="Recenter Map Camera"
          >
            <div className="w-11 h-11 bg-white rounded-full shadow-[0_4px_14px_rgba(0,0,0,0.1)] border border-slate-100 flex items-center justify-center group-hover:bg-slate-50 transition">
              <Compass className="w-5 h-5 text-slate-700" />
            </div>
            <span className="text-[10px] font-bold text-slate-700 mt-1 leading-tight text-center min-h-[24px] flex items-center">
              Recenter
            </span>
          </button>
        </div>
      </div>
    </div>
  );
};
