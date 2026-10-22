import React, { useState } from 'react';
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

  // Compute battery bar fill count (out of 5 bars)
  const filledBars = Math.max(1, Math.min(5, Math.round((battery / 100) * 5)));

  return (
    <div
      className={`absolute bottom-0 inset-x-0 bg-white rounded-t-[32px] shadow-[0_-8px_30px_rgba(0,0,0,0.12)] border-t border-slate-100 z-20 transition-all duration-300 ease-in-out ${
        isExpanded ? 'max-h-[88vh] overflow-y-auto' : 'max-h-[58vh] overflow-hidden'
      }`}
    >
      {/* Drag Handle & Tap Area */}
      <div
        onClick={onToggleExpanded}
        className="w-full pt-3 pb-1 flex flex-col items-center justify-center cursor-pointer select-none group"
      >
        <div className="w-12 h-1.5 bg-slate-300 rounded-full group-hover:bg-slate-400 transition" />
        <div className="flex items-center text-[11px] font-semibold text-slate-400 mt-1">
          {isExpanded ? (
            <span className="flex items-center">
              Swipe down for map view <ChevronDown className="w-3.5 h-3.5 ml-0.5" />
            </span>
          ) : (
            <span className="flex items-center">
              Swipe up for live video & audio <ChevronUp className="w-3.5 h-3.5 ml-0.5 text-purple-600 animate-bounce" />
            </span>
          )}
        </div>
      </div>

      <div className="px-4 pb-6 pt-1 space-y-4 max-w-lg mx-auto">
        {/* ========================================== */}
        {/* SECTION 1: LIVE METRICS                    */}
        {/* ========================================== */}
        <div>
          {/* Section Header */}
          <div className="flex items-center justify-between mb-3">
            <div className="flex items-center space-x-2">
              <div className="w-7 h-7 rounded-full bg-purple-50 flex items-center justify-center text-purple-600">
                <Activity className="w-4 h-4" />
              </div>
              <h2 className="text-base font-bold text-[#17233F]">Live Metrics</h2>
            </div>
            <div className="flex items-center space-x-1.5 px-2.5 py-1 bg-emerald-50 text-emerald-600 rounded-full text-[11px] font-medium border border-emerald-100">
              <span className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse" />
              <span>Updated 10 sec ago</span>
            </div>
          </div>

          {/* 4 Cards Grid */}
          <div className="grid grid-cols-4 gap-2">
            {/* 1. Heart Rate */}
            <div className="bg-[#F8FAFC] border border-slate-100 rounded-2xl p-2.5 flex flex-col justify-between">
              <div>
                <div className="w-6 h-6 rounded-full bg-rose-50 flex items-center justify-center text-rose-500 mb-1">
                  <Heart className="w-3.5 h-3.5 fill-rose-500" />
                </div>
                <div className="text-[10.5px] font-medium text-slate-500">Heart Rate</div>
                <div className="text-[17px] font-bold text-[#17233F] leading-tight mt-0.5">
                  {heartRate} <span className="text-[10px] font-normal text-slate-500">bpm</span>
                </div>
              </div>
              {/* ECG Waveform */}
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
                <div className="text-[10.5px] font-medium text-slate-500">SpO₂</div>
                <div className="text-[17px] font-bold text-[#17233F] leading-tight mt-0.5">
                  {spo2} <span className="text-[10px] font-normal text-slate-500">%</span>
                </div>
              </div>
              {/* Blue Sine Trend Wave */}
              <div className="mt-2 h-6 w-full flex items-center">
                <svg className="w-full h-6" viewBox="0 0 70 20">
                  <defs>
                    <linearGradient id="spo2Fill" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="0%" stopColor="#0284C7" stopOpacity="0.3" />
                      <stop offset="100%" stopColor="#0284C7" stopOpacity="0.0" />
                    </linearGradient>
                  </defs>
                  <path d="M 0 12 Q 18 4 35 12 T 70 12 L 70 20 L 0 20 Z" fill="url(#spo2Fill)" />
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
                <div className="text-[10.5px] font-medium text-slate-500">Phone Battery</div>
                <div className="text-[17px] font-bold text-[#17233F] leading-tight mt-0.5">
                  {battery} <span className="text-[10px] font-normal text-slate-500">%</span>
                </div>
              </div>
              {/* Segmented Battery Bars */}
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
                <div className="text-[10.5px] font-medium text-slate-500">Stress Level</div>
                <div className="text-[16px] font-bold text-[#17233F] leading-tight mt-0.5">
                  {stressLevel}
                </div>
              </div>
              {/* Semicircular Gauge Arc */}
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
        </div>

        {/* ========================================== */}
        {/* SECTION 2: EMERGENCY CONTACTS & SERVICES  */}
        {/* ========================================== */}
        <div className="pt-1">
          <div className="flex items-center space-x-2 mb-2.5">
            <div className="w-6 h-6 rounded-full bg-purple-50 flex items-center justify-center text-purple-600">
              <Phone className="w-3.5 h-3.5" />
            </div>
            <h2 className="text-[15px] font-bold text-[#17233F]">Emergency Contacts & Services</h2>
          </div>

          <div className="grid grid-cols-3 gap-2.5">
            {/* Police (112) */}
            <button
              onClick={() => onCallService('police')}
              className="flex items-center justify-center space-x-2 py-3 px-3 bg-[#EFF6FF] hover:bg-[#DBEAFE] border border-[#BFDBFE] rounded-2xl transition shadow-sm active:scale-95"
            >
              <div className="w-7 h-7 rounded-full bg-blue-600 flex items-center justify-center text-white shadow-sm">
                <Shield className="w-4 h-4 fill-white" />
              </div>
              <span className="text-[14px] font-bold text-slate-900">Police</span>
            </button>

            {/* Ambulance (108) */}
            <button
              onClick={() => onCallService('ambulance')}
              className="flex items-center justify-center space-x-2 py-3 px-3 bg-[#FEF2F2] hover:bg-[#FEE2E2] border border-[#FECACA] rounded-2xl transition shadow-sm active:scale-95"
            >
              <div className="w-7 h-7 rounded-full bg-rose-500 flex items-center justify-center text-white shadow-sm">
                <Plus className="w-4 h-4 stroke-[3]" />
              </div>
              <span className="text-[14px] font-bold text-slate-900">Ambulance</span>
            </button>

            {/* Fire (101) */}
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

        {/* ========================================== */}
        {/* SECTION 3: LIVE VIDEO & AUDIO EVIDENCE    */}
        {/* ========================================== */}
        <div className="pt-2 border-t border-slate-100">
          {/* Header */}
          <div className="flex items-center justify-between mb-3">
            <div className="flex items-center space-x-2">
              <div className="w-7 h-7 rounded-full bg-purple-50 flex items-center justify-center text-purple-600">
                <Video className="w-4 h-4" />
              </div>
              <div>
                <h2 className="text-base font-bold text-[#17233F]">Live Video & Audio</h2>
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

          {/* 2-Column Live Feed Content */}
          <div className="grid grid-cols-2 gap-3">
            {/* Left: Live Video Preview */}
            <div className="relative rounded-2xl overflow-hidden bg-black aspect-[4/3] shadow-md group">
              <img
                src={liveFrame || '/dashcam_stream.jpg'}
                alt="Live Camera Feed"
                className="w-full h-full object-cover"
              />

              {/* Red "▶ LIVE" badge */}
              <div className="absolute top-2 left-2 px-2 py-0.5 bg-rose-600/95 text-white rounded-md text-[10px] font-bold tracking-wider flex items-center space-x-1 shadow">
                <span>▶ LIVE</span>
              </div>

              {/* Maximize Button */}
              <button
                onClick={() => setFullscreenVideo(true)}
                className="absolute top-2 right-2 p-1.5 bg-black/50 hover:bg-black/75 text-white rounded-lg transition"
              >
                <Maximize2 className="w-3.5 h-3.5" />
              </button>

              {/* Timestamp Overlay */}
              <div className="absolute bottom-1.5 left-2 text-[9px] font-mono text-white/90 drop-shadow">
                2025-03-15 21:41:52
              </div>
            </div>

            {/* Right: Audio Recording & Download Button */}
            <div className="flex flex-col justify-between space-y-2">
              {/* Audio Waveform Card */}
              <div className="bg-[#F8FAFC] border border-slate-100 rounded-2xl p-2.5 flex-1 flex flex-col justify-between">
                <div className="flex items-center space-x-2">
                  <div className="w-7 h-7 rounded-full bg-purple-100 flex items-center justify-center text-purple-600">
                    <Mic className="w-3.5 h-3.5" />
                  </div>
                  <div>
                    <div className="text-[11.5px] font-bold text-slate-800">Audio Recording</div>
                  </div>
                </div>

                {/* Animated Frequency Bars */}
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

              {/* Download Video Button */}
              <button
                onClick={onDownloadVideo}
                className="w-full py-2.5 px-3 bg-[#EFF6FF] hover:bg-[#DBEAFE] border border-[#BFDBFE] rounded-xl text-blue-700 flex flex-col items-center justify-center transition shadow-sm active:scale-95"
              >
                <div className="flex items-center space-x-1.5">
                  <Download className="w-3.5 h-3.5 text-blue-600" />
                  <span className="text-[12px] font-bold">Download Video</span>
                </div>
                <span className="text-[9.5px] text-slate-500 mt-0.5">Saved in secure cloud storage</span>
              </button>
            </div>
          </div>
        </div>
      </div>

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
