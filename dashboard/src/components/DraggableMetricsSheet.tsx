import React, { useState, useRef } from 'react';
import {
  Activity,
  Heart,
  Droplets,
  BatteryCharging,
  UserCheck,
  Phone,
  Shield,
  Plus,
  Flame,
  Video,
  Mic,
  Download,
  Maximize2,
  Cloud,
  ChevronUp,
  ChevronDown
} from 'lucide-react';

interface Props {
  heartRate: number;
  spo2: number;
  battery: number;
  stressLevel: string;
  durationText: string;
  isRecording?: boolean;
  recordingUrl?: string | null;
  liveFrame?: string | null;
  audioLevel?: number;
  onCallService: (service: 'police' | 'ambulance' | 'fire') => void;
  onDownloadVideo: () => void;
  isExpanded: boolean;
  onToggleExpanded: () => void;
}

export const DraggableMetricsSheet: React.FC<Props> = ({
  heartRate,
  spo2,
  battery,
  stressLevel,
  durationText,
  isRecording = true,
  recordingUrl,
  liveFrame,
  audioLevel = 45,
  onCallService,
  onDownloadVideo,
  isExpanded,
  onToggleExpanded
}) => {
  const [fullscreenVideo, setFullscreenVideo] = useState(false);
  const touchStartY = useRef<number | null>(null);

  // Compute battery bar fill count (out of 5 bars)
  const filledBars = Math.max(1, Math.min(5, Math.round((battery / 100) * 5)));

  // Touch swipe handling to slide sheet up or down
  const handleTouchStart = (e: React.TouchEvent) => {
    touchStartY.current = e.touches[0].clientY;
  };

  const handleTouchEnd = (e: React.TouchEvent) => {
    if (touchStartY.current === null) return;
    const touchEndY = e.changedTouches[0].clientY;
    const diff = touchStartY.current - touchEndY;
    // Swipe up
    if (diff > 40 && !isExpanded) {
      onToggleExpanded();
    }
    // Swipe down
    else if (diff < -40 && isExpanded) {
      onToggleExpanded();
    }
    touchStartY.current = null;
  };

  return (
    <div
      onTouchStart={handleTouchStart}
      onTouchEnd={handleTouchEnd}
      className={`fixed bottom-0 inset-x-0 bg-white rounded-t-[28px] shadow-[0_-8px_30px_rgba(0,0,0,0.15)] border-t border-slate-100 z-30 transition-all duration-300 ease-out flex flex-col ${
        isExpanded ? 'h-[85vh] max-h-[85vh]' : 'h-[116px]'
      }`}
    >
      {/* Drag Handle Bar */}
      <div
        onClick={onToggleExpanded}
        className="w-full pt-2.5 pb-1 flex flex-col items-center justify-center cursor-pointer select-none active:opacity-70 flex-shrink-0"
      >
        <div className="w-12 h-1.5 bg-slate-300 rounded-full" />
      </div>

      {/* ========================================================================= */}
      {/* 15% COLLAPSED STATE: COMPACT 4 HEALTH METRICS ONLY                       */}
      {/* ========================================================================= */}
      {!isExpanded ? (
        <div
          onClick={onToggleExpanded}
          className="px-3 pb-2 flex-1 flex flex-col justify-between cursor-pointer select-none"
        >
          {/* Header row in collapsed state */}
          <div className="flex items-center justify-between px-1 mb-1">
            <div className="flex items-center space-x-1.5">
              <Activity className="w-3.5 h-3.5 text-purple-600" />
              <span className="text-xs font-bold text-[#17233F]">Live Health Metrics</span>
            </div>
            <div className="flex items-center space-x-1 text-[10px] text-purple-600 font-bold">
              <span>Swipe up for services & video</span>
              <ChevronUp className="w-3.5 h-3.5 text-purple-600 animate-bounce" />
            </div>
          </div>

          {/* 4 Compact Health Metric Cards (Fits in the 15% bottom height!) */}
          <div className="grid grid-cols-4 gap-2">
            {/* 1. Heart Rate */}
            <div className="bg-[#F8FAFC] border border-slate-200/80 rounded-xl p-1.5 flex items-center space-x-1.5 shadow-sm">
              <div className="w-6 h-6 rounded-full bg-rose-50 flex items-center justify-center text-rose-500 flex-shrink-0">
                <Heart className="w-3.5 h-3.5 fill-rose-500" />
              </div>
              <div className="min-w-0">
                <div className="text-[9px] text-slate-500 font-medium leading-none">Heart</div>
                <div className="text-[13px] font-bold text-[#17233F] leading-tight mt-0.5 truncate">
                  {heartRate} <span className="text-[8px] font-normal text-slate-500">bpm</span>
                </div>
              </div>
            </div>

            {/* 2. SpO2 */}
            <div className="bg-[#F8FAFC] border border-slate-200/80 rounded-xl p-1.5 flex items-center space-x-1.5 shadow-sm">
              <div className="w-6 h-6 rounded-full bg-sky-50 flex items-center justify-center text-sky-500 flex-shrink-0">
                <Droplets className="w-3.5 h-3.5 fill-sky-500" />
              </div>
              <div className="min-w-0">
                <div className="text-[9px] text-slate-500 font-medium leading-none">SpO₂</div>
                <div className="text-[13px] font-bold text-[#17233F] leading-tight mt-0.5 truncate">
                  {spo2}<span className="text-[9px] font-normal text-slate-500">%</span>
                </div>
              </div>
            </div>

            {/* 3. Battery */}
            <div className="bg-[#F8FAFC] border border-slate-200/80 rounded-xl p-1.5 flex items-center space-x-1.5 shadow-sm">
              <div className="w-6 h-6 rounded-full bg-emerald-50 flex items-center justify-center text-emerald-500 flex-shrink-0">
                <BatteryCharging className="w-3.5 h-3.5" />
              </div>
              <div className="min-w-0">
                <div className="text-[9px] text-slate-500 font-medium leading-none">Battery</div>
                <div className="text-[13px] font-bold text-[#17233F] leading-tight mt-0.5 truncate">
                  {battery}<span className="text-[9px] font-normal text-slate-500">%</span>
                </div>
              </div>
            </div>

            {/* 4. Stress Level */}
            <div className="bg-[#F8FAFC] border border-slate-200/80 rounded-xl p-1.5 flex items-center space-x-1.5 shadow-sm">
              <div className="w-6 h-6 rounded-full bg-teal-50 flex items-center justify-center text-teal-600 flex-shrink-0">
                <UserCheck className="w-3.5 h-3.5" />
              </div>
              <div className="min-w-0">
                <div className="text-[9px] text-slate-500 font-medium leading-none">Stress</div>
                <div className="text-[12px] font-bold text-[#17233F] leading-tight mt-0.5 truncate">
                  {stressLevel}
                </div>
              </div>
            </div>
          </div>
        </div>
      ) : (
        /* ========================================================================= */
        /* EXPANDED STATE: FULL METRICS, CALLING SHORTCUTS, LIVE VIDEO & AUDIO       */
        /* ========================================================================= */
        <div className="flex-1 overflow-y-auto px-4 pb-8 space-y-4">
          {/* Header with Collapse Action */}
          <div
            onClick={onToggleExpanded}
            className="flex items-center justify-between py-1 cursor-pointer select-none"
          >
            <div className="flex items-center space-x-2">
              <div className="w-7 h-7 rounded-full bg-purple-50 flex items-center justify-center text-purple-600">
                <Activity className="w-4 h-4" />
              </div>
              <h2 className="text-base font-bold text-[#17233F]">Live Telemetry & Metrics</h2>
            </div>
            <div className="flex items-center space-x-1 text-xs text-slate-500 font-semibold">
              <span>Collapse to map</span>
              <ChevronDown className="w-4 h-4" />
            </div>
          </div>

          {/* Full Detailed 4 Metrics Cards with Waveforms */}
          <div className="grid grid-cols-4 gap-2">
            {/* 1. Heart Rate */}
            <div className="bg-[#F8FAFC] border border-slate-100 rounded-2xl p-2.5 flex flex-col justify-between">
              <div>
                <div className="w-6 h-6 rounded-full bg-rose-50 flex items-center justify-center text-rose-500 mb-1">
                  <Heart className="w-3.5 h-3.5 fill-rose-500" />
                </div>
                <div className="text-[10px] font-medium text-slate-500">Heart Rate</div>
                <div className="text-[16px] font-bold text-[#17233F] leading-tight mt-0.5">
                  {heartRate} <span className="text-[9px] font-normal text-slate-500">bpm</span>
                </div>
              </div>
              <div className="mt-2 h-6 w-full flex items-center">
                <svg className="w-full h-6 stroke-rose-500 fill-none" viewBox="0 0 70 20">
                  <path
                    d="M 0 10 L 15 10 L 20 14 L 25 3 L 30 18 L 35 8 L 40 12 L 45 10 L 70 10"
                    strokeWidth="1.8"
                    strokeLinecap="round"
                    strokeLinejoin="round"
                  />
                </svg>
              </div>
            </div>

            {/* 2. SpO2 */}
            <div className="bg-[#F8FAFC] border border-slate-100 rounded-2xl p-2.5 flex flex-col justify-between">
              <div>
                <div className="w-6 h-6 rounded-full bg-sky-50 flex items-center justify-center text-sky-500 mb-1">
                  <Droplets className="w-3.5 h-3.5 fill-sky-500" />
                </div>
                <div className="text-[10px] font-medium text-slate-500">SpO₂</div>
                <div className="text-[16px] font-bold text-[#17233F] leading-tight mt-0.5">
                  {spo2} <span className="text-[9px] font-normal text-slate-500">%</span>
                </div>
              </div>
              <div className="mt-2 h-6 w-full flex items-center">
                <svg className="w-full h-6" viewBox="0 0 70 20">
                  <defs>
                    <linearGradient id="spo2FillExp" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="0%" stopColor="#0284C7" stopOpacity="0.3" />
                      <stop offset="100%" stopColor="#0284C7" stopOpacity="0.0" />
                    </linearGradient>
                  </defs>
                  <path d="M 0 12 Q 18 4 35 12 T 70 12 L 70 20 L 0 20 Z" fill="url(#spo2FillExp)" />
                  <path
                    d="M 0 12 Q 18 4 35 12 T 70 12"
                    fill="none"
                    stroke="#0284C7"
                    strokeWidth="1.8"
                    strokeLinecap="round"
                  />
                </svg>
              </div>
            </div>

            {/* 3. Phone Battery */}
            <div className="bg-[#F8FAFC] border border-slate-100 rounded-2xl p-2.5 flex flex-col justify-between">
              <div>
                <div className="w-6 h-6 rounded-full bg-emerald-50 flex items-center justify-center text-emerald-500 mb-1">
                  <BatteryCharging className="w-3.5 h-3.5" />
                </div>
                <div className="text-[10px] font-medium text-slate-500">Phone Battery</div>
                <div className="text-[16px] font-bold text-[#17233F] leading-tight mt-0.5">
                  {battery} <span className="text-[9px] font-normal text-slate-500">%</span>
                </div>
              </div>
              <div className="mt-2 flex items-center space-x-1 h-6">
                {[1, 2, 3, 4, 5].map((bar) => (
                  <div
                    key={bar}
                    className={`flex-1 h-4 rounded-sm transition-colors ${
                      bar <= filledBars ? 'bg-emerald-500' : 'bg-slate-200'
                    }`}
                  />
                ))}
              </div>
            </div>

            {/* 4. Stress Level */}
            <div className="bg-[#F8FAFC] border border-slate-100 rounded-2xl p-2.5 flex flex-col justify-between">
              <div>
                <div className="w-6 h-6 rounded-full bg-teal-50 flex items-center justify-center text-teal-600 mb-1">
                  <UserCheck className="w-3.5 h-3.5" />
                </div>
                <div className="text-[10px] font-medium text-slate-500">Stress Level</div>
                <div className="text-[15px] font-bold text-[#17233F] leading-tight mt-0.5">
                  {stressLevel}
                </div>
              </div>
              <div className="mt-2 h-6 w-full flex items-center justify-center">
                <svg className="w-12 h-6" viewBox="0 0 40 20">
                  <path
                    d="M 5 18 A 15 15 0 0 1 35 18"
                    fill="none"
                    stroke="#E2E8F0"
                    strokeWidth="3.5"
                    strokeLinecap="round"
                  />
                  <path
                    d="M 5 18 A 15 15 0 0 1 20 4"
                    fill="none"
                    stroke="#10B981"
                    strokeWidth="4"
                    strokeLinecap="round"
                  />
                </svg>
              </div>
            </div>
          </div>

          {/* Section 2: Emergency Contacts & Services */}
          <div className="pt-2">
            <div className="flex items-center space-x-2 mb-2.5">
              <div className="w-6 h-6 rounded-full bg-purple-50 flex items-center justify-center text-purple-600">
                <Phone className="w-3.5 h-3.5" />
              </div>
              <h3 className="text-[15px] font-bold text-[#17233F]">Emergency Contacts & Services</h3>
            </div>

            <div className="grid grid-cols-3 gap-2.5">
              <button
                onClick={() => onCallService('police')}
                className="flex items-center justify-center space-x-2 py-3 px-3 bg-[#EFF6FF] hover:bg-[#DBEAFE] border border-[#BFDBFE] rounded-2xl transition shadow-sm active:scale-95"
              >
                <div className="w-7 h-7 rounded-full bg-blue-600 flex items-center justify-center text-white shadow-sm">
                  <Shield className="w-4 h-4 fill-white" />
                </div>
                <span className="text-[14px] font-bold text-slate-900">Police</span>
              </button>

              <button
                onClick={() => onCallService('ambulance')}
                className="flex items-center justify-center space-x-2 py-3 px-3 bg-[#FEF2F2] hover:bg-[#FEE2E2] border border-[#FECACA] rounded-2xl transition shadow-sm active:scale-95"
              >
                <div className="w-7 h-7 rounded-full bg-rose-500 flex items-center justify-center text-white shadow-sm">
                  <Plus className="w-4 h-4 stroke-[3]" />
                </div>
                <span className="text-[14px] font-bold text-slate-900">Ambulance</span>
              </button>

              <button
                onClick={() => onCallService('fire')}
                className="flex items-center justify-center space-x-2 py-3 px-3 bg-[#FFF7ED] hover:bg-[#FFEDD5] border border-[#FED7AA] rounded-2xl transition shadow-sm active:scale-95"
              >
                <div className="w-7 h-7 rounded-full bg-amber-500 flex items-center justify-center text-white shadow-sm">
                  <Flame className="w-4 h-4 fill-white" />
                </div>
                <span className="text-[14px] font-bold text-slate-900">Fire</span>
              </button>
            </div>
          </div>

          {/* Section 3: Live Video & Audio Evidence */}
          <div className="pt-3 border-t border-slate-100">
            <div className="flex items-center justify-between mb-3">
              <div className="flex items-center space-x-2">
                <div className="w-7 h-7 rounded-full bg-purple-50 flex items-center justify-center text-purple-600">
                  <Video className="w-4 h-4" />
                </div>
                <div>
                  <h3 className="text-base font-bold text-[#17233F]">Live Video & Audio</h3>
                  <div className="flex items-center space-x-1.5 text-xs text-slate-500 mt-0.5">
                    <span className="w-2 h-2 rounded-full bg-rose-500 animate-pulse" />
                    <span>Recording • {durationText}</span>
                  </div>
                </div>
              </div>

              <div className="flex items-center space-x-1 px-2.5 py-1 bg-emerald-50 text-emerald-600 rounded-full text-[11px] font-medium border border-emerald-100">
                <Cloud className="w-3.5 h-3.5 text-emerald-500" />
                <span>Auto uploading to cloud</span>
              </div>
            </div>

            {/* Live Video Frame - Full Width & Increased Height */}
            <div className="relative w-full rounded-2xl overflow-hidden bg-black aspect-[16/10] sm:aspect-video shadow-md group">
              <img
                src={liveFrame || '/dashcam_stream.jpg'}
                alt="Live Camera Feed"
                className="w-full h-full object-cover"
              />
              <div className="absolute top-2.5 left-2.5 px-2.5 py-1 bg-rose-600/95 text-white rounded-md text-[10px] font-bold tracking-wider flex items-center space-x-1.5 shadow">
                <span className="w-1.5 h-1.5 rounded-full bg-white animate-pulse" />
                <span>LIVE</span>
              </div>
              <button
                onClick={() => setFullscreenVideo(true)}
                className="absolute top-2.5 right-2.5 p-1.5 bg-black/50 hover:bg-black/75 text-white rounded-lg transition"
                title="Fullscreen"
              >
                <Maximize2 className="w-4 h-4" />
              </button>
              <div className="absolute bottom-2 left-2.5 px-2 py-0.5 bg-black/60 backdrop-blur-xs rounded text-[9.5px] font-mono text-white/90 drop-shadow">
                2025-03-15 21:41:52 • CAM-01 FRONT
              </div>
            </div>

            {/* Audio & Download Controls below Video */}
            <div className="grid grid-cols-2 gap-3 mt-3">
              {/* Audio Recording */}
              <div className="bg-[#F8FAFC] border border-slate-100 rounded-2xl p-2.5 flex flex-col justify-between">
                <div className="flex items-center space-x-2">
                  <div className="w-7 h-7 rounded-full bg-purple-100 flex items-center justify-center text-purple-600 shrink-0">
                    <Mic className="w-3.5 h-3.5" />
                  </div>
                  <div className="text-[11.5px] font-bold text-slate-800">Audio Recording</div>
                </div>
                <div className="flex items-center justify-center space-x-1 my-2 h-7 px-1">
                  <div className="w-1 bg-purple-400 h-3 rounded-full animate-audio-1" />
                  <div className="w-1 bg-purple-500 h-5 rounded-full animate-audio-2" />
                  <div className="w-1 bg-purple-600 h-6 rounded-full animate-audio-3" />
                  <div className="w-1 bg-purple-500 h-4 rounded-full animate-audio-4" />
                  <div className="w-1 bg-purple-600 h-7 rounded-full animate-audio-5" />
                  <div className="w-1 bg-purple-400 h-3 rounded-full animate-audio-2" />
                  <div className="w-1 bg-purple-500 h-5 rounded-full animate-audio-3" />
                  <div className="w-1 bg-purple-600 h-4 rounded-full animate-audio-1" />
                  <div className="w-1 bg-purple-400 h-2 rounded-full animate-audio-4" />
                </div>
                <div className="text-[10px] text-right font-mono text-slate-500">
                  {durationText}
                </div>
              </div>

              {/* Download Button */}
              <button
                onClick={onDownloadVideo}
                className="w-full h-full min-h-[90px] py-2.5 px-3 bg-[#EFF6FF] hover:bg-[#DBEAFE] border border-[#BFDBFE] rounded-2xl text-blue-700 flex flex-col items-center justify-center transition shadow-sm active:scale-95"
              >
                <div className="w-7 h-7 rounded-full bg-blue-100 flex items-center justify-center text-blue-600 mb-1">
                  <Download className="w-3.5 h-3.5" />
                </div>
                <div className="text-[12px] font-bold">Download Video</div>
                <span className="text-[9.5px] text-slate-500 mt-0.5 text-center leading-tight">Saved in secure cloud</span>
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Fullscreen Video Modal */}
      {fullscreenVideo && (
        <div className="fixed inset-0 z-50 bg-black/95 flex flex-col items-center justify-center p-4">
          <div className="relative max-w-2xl w-full">
            <button
              onClick={() => setFullscreenVideo(false)}
              className="absolute -top-10 right-0 text-white font-bold text-sm bg-slate-800 px-3 py-1 rounded-full"
            >
              ✕ Close
            </button>
            <img
              src={liveFrame || '/dashcam_stream.jpg'}
              alt="Fullscreen Live Feed"
              className="w-full rounded-2xl shadow-2xl object-cover"
            />
          </div>
        </div>
      )}
    </div>
  );
};
