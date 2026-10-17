import React from 'react';
import { Heart, Battery, Navigation, Activity, CheckCircle2 } from 'lucide-react';

interface Props {
  heartRateBpm: number | null;
  batteryPercentage: number | null;
  accuracyMeters: number;
  latitude: number;
  longitude: number;
  status?: string;
}

export const LiveTelemetryCard: React.FC<Props> = ({
  heartRateBpm,
  batteryPercentage,
  accuracyMeters,
  latitude,
  longitude,
  status = 'ACTIVE',
}) => {
  const isHighStress = heartRateBpm && heartRateBpm > 130;
  const hasValidCoordinates = (latitude !== 0 || longitude !== 0) && !isNaN(latitude) && !isNaN(longitude);
  const isResolved = status === 'RESOLVED';
  const isStandby = status === 'STANDBY';

  return (
    <div className="grid grid-cols-2 lg:grid-cols-4 gap-3 sm:gap-4">
      {/* 1. Heart Rate */}
      <div
        className={`p-4 rounded-2xl border transition-all ${
          isHighStress
            ? 'bg-rose-950/40 border-rose-600/50 text-rose-300'
            : 'bg-[#131B2E] border-slate-800 text-slate-200'
        }`}
      >
        <div className="flex items-center justify-between">
          <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider">Heart Rate</span>
          <Heart className={`w-5 h-5 ${heartRateBpm ? 'text-rose-500 animate-bounce' : 'text-slate-600'}`} />
        </div>
        <div className="mt-2 flex items-baseline space-x-1.5">
          <span className={`text-2xl sm:text-3xl font-extrabold ${heartRateBpm ? 'text-white' : 'text-slate-500'}`}>
            {heartRateBpm && heartRateBpm > 0 ? heartRateBpm : 'N/A'}
          </span>
          {heartRateBpm && heartRateBpm > 0 ? (
            <span className="text-xs text-slate-400 font-medium">BPM</span>
          ) : (
            <span className="text-xs text-slate-500 font-medium">Wearable</span>
          )}
        </div>
        <p className="text-[11px] mt-1 text-slate-400 truncate">
          {isHighStress
            ? '⚠️ High Tachycardia / Panic Detected'
            : heartRateBpm && heartRateBpm > 0
            ? 'GATT 0x180D Real-time'
            : 'Not Available (No Sensor Linked)'}
        </p>
      </div>

      {/* 2. Battery */}
      <div className="p-4 rounded-2xl bg-[#131B2E] border border-slate-800 text-slate-200">
        <div className="flex items-center justify-between">
          <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider">Device Battery</span>
          <Battery
            className={`w-5 h-5 ${
              batteryPercentage != null
                ? batteryPercentage < 20
                  ? 'text-rose-500 animate-pulse'
                  : 'text-emerald-400'
                : 'text-slate-600'
            }`}
          />
        </div>
        <div className="mt-2 flex items-baseline space-x-1.5">
          <span className={`text-2xl sm:text-3xl font-extrabold ${batteryPercentage != null ? 'text-white' : 'text-slate-500'}`}>
            {batteryPercentage != null ? `${batteryPercentage}%` : 'N/A'}
          </span>
        </div>
        <p className="text-[11px] mt-1 text-slate-400 truncate">
          {batteryPercentage != null
            ? batteryPercentage < 20
              ? '⚠️ Low Battery Warning'
              : 'Power reserve normal'
            : 'Not Available'}
        </p>
      </div>

      {/* 3. GPS Accuracy */}
      <div className="p-4 rounded-2xl bg-[#131B2E] border border-slate-800 text-slate-200">
        <div className="flex items-center justify-between">
          <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider">GPS Precision</span>
          <Navigation className={`w-5 h-5 ${hasValidCoordinates ? 'text-sky-400' : 'text-slate-600'}`} />
        </div>
        <div className="mt-2 flex items-baseline space-x-1.5">
          <span className={`text-2xl sm:text-3xl font-extrabold ${hasValidCoordinates ? 'text-white' : 'text-slate-500'}`}>
            {hasValidCoordinates ? `±${Math.round(accuracyMeters || 10)}` : 'N/A'}
          </span>
          {hasValidCoordinates && <span className="text-xs text-slate-400 font-medium">meters</span>}
        </div>
        <p className="text-[11px] mt-1 text-slate-400 truncate font-mono">
          {hasValidCoordinates ? `${latitude.toFixed(5)}, ${longitude.toFixed(5)}` : 'Not Available (Acquiring GPS)'}
        </p>
      </div>

      {/* 4. Telemetry Link */}
      <div className="p-4 rounded-2xl bg-[#131B2E] border border-slate-800 text-slate-200">
        <div className="flex items-center justify-between">
          <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider">Telemetry Link</span>
          {isResolved ? (
            <CheckCircle2 className="w-5 h-5 text-emerald-400" />
          ) : (
            <Activity className={`w-5 h-5 ${isStandby ? 'text-slate-600' : 'text-emerald-400'}`} />
          )}
        </div>
        <div className="mt-2 flex items-baseline space-x-1.5">
          <span
            className={`text-2xl sm:text-3xl font-extrabold ${
              isResolved ? 'text-emerald-400' : isStandby ? 'text-slate-500' : 'text-emerald-400'
            }`}
          >
            {isResolved ? 'RESOLVED' : isStandby ? 'STANDBY' : 'LIVE'}
          </span>
        </div>
        <p className="text-[11px] mt-1 text-slate-400 truncate">
          {isResolved
            ? 'Incident Resolved • Evidence Saved'
            : isStandby
            ? 'Awaiting Distress Signal'
            : 'Encrypted WebSocket Sync'}
        </p>
      </div>
    </div>
  );
};
