import React from 'react';
import { AlertTriangle, CheckCircle2, PhoneCall, ShieldCheck, ShieldAlert, Radio } from 'lucide-react';

interface Props {
  incidentId: string;
  triggerType: string;
  status: string;
  startedAt: number;
  resolvedAt?: number | null;
  onResolve?: () => void;
}

export const EmergencyHeader: React.FC<Props> = ({
  incidentId,
  triggerType,
  status,
  startedAt,
  resolvedAt,
  onResolve,
}) => {
  const isResolved = status === 'RESOLVED';
  const isStandby = status === 'STANDBY' || incidentId === 'awaiting-sos';
  const durationMinutes = Math.max(0, Math.floor(((resolvedAt || Date.now()) - startedAt) / 60000));

  return (
    <header className="border-b border-slate-800 bg-[#090D16]/95 backdrop-blur sticky top-0 z-50 shadow-md">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 py-3.5 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        {/* Left branding & status */}
        <div className="flex items-center space-x-3.5">
          <div
            className={`p-2.5 rounded-xl ${
              isResolved
                ? 'bg-emerald-500/20 text-emerald-400 border border-emerald-500/30'
                : isStandby
                ? 'bg-slate-800 text-slate-400 border border-slate-700'
                : 'bg-rose-500/20 text-rose-500 border border-rose-500/40 emergency-pulse'
            }`}
          >
            {isResolved ? (
              <CheckCircle2 className="w-7 h-7" />
            ) : isStandby ? (
              <Radio className="w-7 h-7" />
            ) : (
              <AlertTriangle className="w-7 h-7" />
            )}
          </div>
          <div>
            <div className="flex items-center space-x-2.5 flex-wrap">
              <span className="text-xl font-bold tracking-tight text-white">Maaya Shield</span>
              <span
                className={`text-xs px-2.5 py-0.5 rounded-full font-bold uppercase tracking-wider ${
                  isResolved
                    ? 'bg-emerald-950 text-emerald-400 border border-emerald-700'
                    : isStandby
                    ? 'bg-slate-900 text-slate-400 border border-slate-700'
                    : 'bg-rose-950 text-rose-400 border border-rose-800 animate-pulse'
                }`}
              >
                {isResolved ? 'RESOLVED / SAFE' : isStandby ? 'STANDBY • AWAITING SOS' : 'ACTIVE EMERGENCY SOS'}
              </span>
            </div>
            <p className="text-xs text-slate-400 mt-0.5">
              {isStandby ? (
                <span>Network online • Listening for emergency distress beacon</span>
              ) : (
                <>
                  Incident ID:{' '}
                  <span className="font-mono text-slate-300">
                    {incidentId.length > 12 ? `${incidentId.slice(0, 8)}...` : incidentId}
                  </span>{' '}
                  • Triggered by:{' '}
                  <span className="font-semibold text-rose-400">{triggerType || 'MANUAL_IN_APP'}</span>{' '}
                  ({durationMinutes}m duration)
                  {isResolved && (
                    <span className="ml-1 text-emerald-400 font-semibold">• All Evidence Secured</span>
                  )}
                </>
              )}
            </p>
          </div>
        </div>

        {/* Right actions */}
        <div className="flex items-center space-x-2.5 w-full sm:w-auto">
          {/* Resolve Emergency Action (if currently active) */}
          {!isResolved && !isStandby && onResolve && (
            <button
              onClick={onResolve}
              className="px-3.5 py-2 rounded-lg bg-emerald-600 hover:bg-emerald-500 text-white font-semibold text-xs transition-colors flex items-center space-x-1.5 shadow-lg shadow-emerald-950/40"
              title="Confirm victim is safe and conclude emergency session"
            >
              <ShieldCheck className="w-4 h-4" />
              <span>Mark Safe & Resolve</span>
            </button>
          )}

          {/* Emergency Police Dial */}
          <a
            href="tel:112"
            className="flex-1 sm:flex-initial inline-flex items-center justify-center space-x-2 px-4 py-2 rounded-lg bg-rose-600 hover:bg-rose-500 text-white font-semibold text-sm transition-colors shadow-lg shadow-rose-900/30"
          >
            <PhoneCall className="w-4 h-4" />
            <span>Call 112 (Police)</span>
          </a>
        </div>
      </div>
    </header>
  );
};
