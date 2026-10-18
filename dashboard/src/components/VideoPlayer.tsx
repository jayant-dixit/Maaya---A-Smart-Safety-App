import React, { useState, useEffect, useRef } from 'react';
import {
  Video,
  Mic,
  Maximize2,
  ShieldCheck,
  ShieldAlert,
  Volume2,
  VolumeX,
  Radio,
  Download,
  Play,
  FileVideo,
  CheckCircle2,
  Film
} from 'lucide-react';
import { host, BACKEND_URL } from '../services/api';

interface Props {
  sessionId: string;
  isStreaming: boolean;
  status?: string;
  liveFrame?: string | null;
  audioLevel?: number;
  recordingUrl?: string | null;
}

export const VideoPlayer: React.FC<Props> = ({
  sessionId,
  isStreaming,
  status = 'ACTIVE',
  liveFrame,
  audioLevel = 0,
  recordingUrl,
}) => {
  const isResolved = status === 'RESOLVED';
  const isStandby = status === 'STANDBY' || sessionId === 'awaiting-sos';

  const [isMuted, setIsMuted] = useState(false);
  const [isPlayingAudio, setIsPlayingAudio] = useState(false);
  const [audioBlocked, setAudioBlocked] = useState(false);
  const [currentTimeStr, setCurrentTimeStr] = useState<string>('');
  const [audioLevels, setAudioLevels] = useState<number[]>([15, 25, 30, 45, 20, 35, 50, 28, 18, 40]);
  const liveAudioRef = useRef<HTMLAudioElement | null>(null);
  const recordedVideoRef = useRef<HTMLVideoElement | null>(null);

  // Live ticking clock overlay
  useEffect(() => {
    const updateTime = () => {
      const now = new Date();
      setCurrentTimeStr(now.toLocaleTimeString() + '.' + String(now.getMilliseconds()).padStart(3, '0').slice(0, 2));
    };
    updateTime();
    const interval = setInterval(updateTime, 100);
    return () => clearInterval(interval);
  }, []);

  const fullRecordingUrl = recordingUrl
    ? (recordingUrl.startsWith('http') ? recordingUrl : `${BACKEND_URL.replace(/\/+$/, '')}${recordingUrl}`)
    : null;

  // Auto-play live audio as soon as recording chunks are available (during ACTIVE mode)
  useEffect(() => {
    if (!liveAudioRef.current || !fullRecordingUrl || isResolved) return;

    if (liveAudioRef.current.src !== fullRecordingUrl) {
      liveAudioRef.current.src = fullRecordingUrl;
    }

    if (!isMuted) {
      liveAudioRef.current.muted = false;
      liveAudioRef.current.play()
        .then(() => {
          setIsPlayingAudio(true);
          setAudioBlocked(false);
        })
        .catch((err) => {
          console.log('Audio autoplay prevented by browser policy:', err.message);
          setAudioBlocked(true);
          setIsPlayingAudio(false);
        });
    } else {
      liveAudioRef.current.pause();
      setIsPlayingAudio(false);
    }
  }, [fullRecordingUrl, isMuted, isResolved]);

  const handleStartAudio = () => {
    if (liveAudioRef.current) {
      liveAudioRef.current.muted = false;
      liveAudioRef.current.play()
        .then(() => {
          setIsMuted(false);
          setIsPlayingAudio(true);
          setAudioBlocked(false);
        })
        .catch((e) => console.warn('Audio play error:', e));
    }
  };

  // Sync real audio levels from phone microphone
  useEffect(() => {
    if (audioLevel > 0 && !isMuted) {
      setAudioLevels((prev) =>
        prev.map((_, i) => Math.min(22, Math.floor((audioLevel / 100) * 20) + (i % 4) * 2 + 3))
      );
    } else if (isStreaming && !isMuted && !isResolved) {
      const interval = setInterval(() => {
        setAudioLevels((prev) =>
          prev.map(() => Math.floor(Math.random() * 20) + 4)
        );
      }, 150);
      return () => clearInterval(interval);
    }
  }, [audioLevel, isStreaming, isMuted, isResolved]);

  return (
    <div className="bg-[#131B2E] border border-slate-800 rounded-2xl overflow-hidden flex flex-col h-full shadow-lg">
      {/* Hidden real-time audio element for live distress audio */}
      <audio
        ref={liveAudioRef}
        playsInline
        autoPlay
        loop
        onEnded={() => {
          if (liveAudioRef.current && !isMuted) {
            liveAudioRef.current.currentTime = 0;
            liveAudioRef.current.play().catch(() => {});
          }
        }}
        onError={() => setIsPlayingAudio(false)}
        onPlay={() => {
          setIsPlayingAudio(true);
          setAudioBlocked(false);
        }}
      />

      {/* Stream Header */}
      <div className="p-3.5 border-b border-slate-800 flex items-center justify-between bg-[#0E1524]">
        <div className="flex items-center space-x-2">
          {isResolved ? (
            <div className="w-7 h-7 rounded-lg bg-emerald-500/20 border border-emerald-500/30 flex items-center justify-center">
              <Film className="w-4 h-4 text-emerald-400" />
            </div>
          ) : (
            <div className="w-7 h-7 rounded-lg bg-rose-500/20 border border-rose-500/30 flex items-center justify-center">
              <Video className="w-4 h-4 text-rose-500" />
            </div>
          )}
          <div>
            <div className="flex items-center space-x-2">
              <span className="font-bold text-white text-sm">
                {isResolved ? 'Archived Video & Audio Evidence' : 'Real-Time Video & Audio Stream'}
              </span>
            </div>
            <p className="text-[11px] text-slate-400">
              {isResolved
                ? 'Incident resolved • Recorded evidence preserved for contacts & law enforcement'
                : 'Live audio/video relay from victim device'}
            </p>
          </div>
        </div>

        <div className="flex items-center space-x-2">
          <span
            className={`text-[11px] px-2.5 py-1 rounded-full font-bold uppercase tracking-wider flex items-center space-x-1.5 ${
              isResolved
                ? 'bg-emerald-950 text-emerald-400 border border-emerald-800'
                : isStreaming
                ? 'bg-rose-950 text-rose-400 border border-rose-800'
                : 'bg-slate-800 text-slate-400 border border-slate-700'
            }`}
          >
            <span
              className={`w-2 h-2 rounded-full ${
                isResolved
                  ? 'bg-emerald-400'
                  : isStreaming
                  ? 'bg-rose-500 animate-ping'
                  : 'bg-slate-500'
              }`}
            />
            <span>
              {isResolved
                ? 'Evidence Secured'
                : isStreaming
                ? liveFrame
                  ? 'Live Camera Stream'
                  : 'Distress Active'
                : 'Standby'}
            </span>
          </span>
        </div>
      </div>

      {/* Main Viewport Container */}
      <div className="relative flex-1 min-h-[340px] bg-black flex flex-col items-center justify-center p-3 sm:p-4">
        {/* Case 1: Incident RESOLVED -> Full Replay of Captured Video Evidence */}
        {isResolved ? (
          <div className="w-full h-full flex flex-col items-center justify-center">
            {fullRecordingUrl ? (
              <div className="w-full h-full min-h-[300px] flex flex-col items-center justify-between rounded-xl overflow-hidden relative bg-slate-950 border border-emerald-900/60 shadow-2xl">
                {/* HTML5 Video Player with playback controls */}
                <video
                  ref={recordedVideoRef}
                  controls
                  playsInline
                  src={fullRecordingUrl}
                  className="w-full h-full min-h-[260px] max-h-[360px] object-contain bg-black"
                />

                {/* Evidence Secured Overlay Header */}
                <div className="w-full p-2.5 bg-[#090D16]/95 border-t border-slate-800 flex flex-wrap items-center justify-between gap-2 text-xs">
                  <div className="flex items-center space-x-2 text-emerald-400 font-medium">
                    <CheckCircle2 className="w-4 h-4" />
                    <span>Evidence Sealed • MP4 Audio/Video Capture Ready</span>
                  </div>
                  <a
                    href={fullRecordingUrl}
                    download={`evidence_${sessionId}.mp4`}
                    className="px-3 py-1.5 rounded-lg bg-emerald-600 hover:bg-emerald-500 text-white font-semibold inline-flex items-center space-x-1.5 transition-colors shadow-lg shadow-emerald-950/50"
                  >
                    <Download className="w-3.5 h-3.5" />
                    <span>Store as Evidence (.mp4)</span>
                  </a>
                </div>
              </div>
            ) : liveFrame ? (
              <div className="w-full h-full min-h-[300px] rounded-xl overflow-hidden relative bg-black flex items-center justify-center">
                <img src={liveFrame} alt="Final Recorded Frame" className="w-full h-full object-cover opacity-85" />
                <div className="absolute inset-0 bg-black/40 flex flex-col items-center justify-center p-4 text-center">
                  <CheckCircle2 className="w-10 h-10 text-emerald-400 mb-2" />
                  <p className="text-sm font-bold text-white">Emergency Resolved</p>
                  <p className="text-xs text-slate-300 mt-1 max-w-xs">
                    Last camera sensor snapshot archived as evidence.
                  </p>
                </div>
              </div>
            ) : (
              <div className="text-center p-6 max-w-sm">
                <div className="w-12 h-12 rounded-full bg-emerald-950/60 border border-emerald-700/50 flex items-center justify-center mx-auto mb-3 text-emerald-400">
                  <ShieldCheck className="w-6 h-6" />
                </div>
                <p className="text-sm font-bold text-white">Emergency Successfully Resolved</p>
                <p className="text-xs text-slate-400 mt-1">
                  Distress session closed. All recorded telemetry and movement traces have been sealed for review.
                </p>
              </div>
            )}
          </div>
        ) : isStreaming ? (
          /* Case 2: ACTIVE Emergency Streaming View */
          <div className="w-full h-full flex flex-col items-center justify-center">
            <div className="w-full h-full min-h-[300px] bg-gradient-to-br from-slate-900 via-[#0B101E] to-slate-950 rounded-xl border border-slate-800 relative flex flex-col justify-between p-4 overflow-hidden shadow-2xl">
              {/* REAL LIVE CAMERA FRAME FROM ANDROID */}
              {liveFrame ? (
                <div className="absolute inset-0 flex items-center justify-center bg-black">
                  <img
                    src={liveFrame}
                    alt="Live Emergency Video Stream"
                    className="w-full h-full object-cover"
                  />
                  <div className="absolute inset-0 bg-black/10 pointer-events-none"></div>
                </div>
              ) : (
                <>
                  {/* Scanner scanlines */}
                  <div className="absolute inset-0 bg-[linear-gradient(rgba(18,16,16,0)_50%,rgba(0,0,0,0.25)_50%)] bg-[length:100%_4px] pointer-events-none opacity-40"></div>
                  <div className="absolute inset-0 bg-[radial-gradient(#1e293b_1px,transparent_1px)] [background-size:16px_16px] opacity-20 pointer-events-none"></div>

                  {/* Target Reticle */}
                  <div className="relative z-10 flex flex-col items-center justify-center my-auto pointer-events-none">
                    <div className="w-24 h-24 border border-rose-500/30 rounded-lg flex items-center justify-center relative">
                      <div className="w-2 h-2 bg-rose-500/80 rounded-full animate-ping"></div>
                      <div className="absolute top-0 left-0 w-3 h-3 border-t-2 border-l-2 border-rose-500"></div>
                      <div className="absolute top-0 right-0 w-3 h-3 border-t-2 border-r-2 border-rose-500"></div>
                      <div className="absolute bottom-0 left-0 w-3 h-3 border-b-2 border-l-2 border-rose-500"></div>
                      <div className="absolute bottom-0 right-0 w-3 h-3 border-b-2 border-r-2 border-rose-500"></div>
                    </div>
                    <p className="text-[10px] font-mono text-slate-400 mt-2 tracking-widest uppercase">
                      Distress Feed Connected • Capturing Sensor Frames
                    </p>
                  </div>
                </>
              )}

              {/* Top Viewport Telemetry Bar */}
              <div className="relative z-10 flex items-center justify-between text-[11px] font-mono">
                <div className="bg-black/75 backdrop-blur-md px-2.5 py-1 rounded-lg border border-slate-700/60 text-emerald-400 flex items-center space-x-2">
                  <span className="w-2 h-2 rounded-full bg-rose-500 animate-pulse"></span>
                  <span className="font-bold text-white uppercase tracking-wider">
                    {liveFrame ? 'CAMERA ON' : 'REC LIVE'}
                  </span>
                  <span className="text-slate-500">|</span>
                  <span>{currentTimeStr}</span>
                </div>

                <div className="bg-black/75 backdrop-blur-md px-2.5 py-1 rounded-lg border border-slate-700/60 text-slate-300 flex items-center space-x-2">
                  <Radio className="w-3 h-3 text-emerald-400 animate-pulse" />
                  <span>{liveFrame ? 'Live Frame Stream' : '720p 30fps'}</span>
                  <span className="text-slate-500">•</span>
                  <span>Low Latency</span>
                </div>
              </div>

              {/* Autoplay Unmute Action if sound is blocked */}
              {fullRecordingUrl && (!isPlayingAudio || audioBlocked) && (
                <div className="relative z-20 my-auto self-center">
                  <button
                    onClick={handleStartAudio}
                    className="px-4 py-2.5 bg-rose-600 hover:bg-rose-500 text-white rounded-xl text-xs font-bold flex items-center space-x-2 shadow-2xl shadow-rose-900/60 border border-rose-400/40 animate-bounce transition-transform active:scale-95"
                  >
                    <Volume2 className="w-4 h-4 text-white" />
                    <span>🔊 CLICK TO LISTEN LIVE AUDIO</span>
                  </button>
                </div>
              )}

              {/* Bottom Audio VU & Device Info Bar */}
              <div className="relative z-10 flex flex-wrap items-center justify-between gap-2 text-xs text-slate-300 bg-black/75 backdrop-blur-md px-3.5 py-2 rounded-xl border border-slate-700/60">
                <div className="flex items-center space-x-3">
                  <button
                    onClick={() => {
                      if (isMuted || !isPlayingAudio) {
                        handleStartAudio();
                      } else {
                        setIsMuted(true);
                        liveAudioRef.current?.pause();
                        setIsPlayingAudio(false);
                      }
                    }}
                    className="flex items-center space-x-1.5 hover:text-white transition-colors"
                  >
                    {isMuted || !isPlayingAudio ? (
                      <VolumeX className="w-3.5 h-3.5 text-rose-400" />
                    ) : (
                      <Mic className="w-3.5 h-3.5 text-emerald-400 animate-pulse" />
                    )}
                    <span className="text-[11px] font-medium text-slate-200">
                      {isMuted || !isPlayingAudio ? 'Audio Muted (Click to Hear)' : 'Mic Live (Playing)'}
                    </span>
                  </button>

                  {/* Dynamic Equalizer */}
                  {(!isMuted || isPlayingAudio) && (
                    <div className="flex items-end space-x-1 h-5">
                      {audioLevels.map((h, i) => (
                        <div
                          key={i}
                          style={{ height: `${Math.min(h, 20)}px` }}
                          className="w-1 bg-gradient-to-t from-emerald-500 to-rose-500 rounded-full transition-all duration-100"
                        />
                      ))}
                    </div>
                  )}
                </div>

                <div className="flex items-center space-x-2 text-[11px] text-slate-400 font-mono">
                  <span>SFU Stream Relay Active</span>
                </div>
              </div>
            </div>
          </div>
        ) : (
          /* Case 3: Standby mode */
          <div className="text-center p-6 max-w-sm">
            <div className="w-12 h-12 rounded-full bg-slate-900 border border-slate-800 flex items-center justify-center mx-auto mb-3 text-slate-500">
              <ShieldAlert className="w-6 h-6" />
            </div>
            <p className="text-sm font-semibold text-white">Stream Standby</p>
            <p className="text-xs text-slate-400 mt-1">
              Awaiting emergency broadcast. Live camera and microphone will activate immediately when SOS is triggered.
            </p>
          </div>
        )}
      </div>

      {/* Evidence Audio/Video Recording Player Section (Active and Resolved) */}
      {fullRecordingUrl && (
        <div className="p-3 bg-slate-900/95 border-t border-slate-800 flex flex-col space-y-2">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold text-emerald-400 flex items-center space-x-1.5">
              <FileVideo className="w-3.5 h-3.5 text-emerald-400" />
              <span>Evidence Recording Clip Available</span>
            </span>
            <a
              href={fullRecordingUrl}
              download={`evidence_${sessionId}.mp4`}
              target="_blank"
              rel="noopener noreferrer"
              className="text-[11px] font-semibold text-rose-400 hover:text-rose-300 inline-flex items-center space-x-1"
            >
              <Download className="w-3 h-3" />
              <span>Store Evidence (.mp4)</span>
            </a>
          </div>
          <audio controls src={fullRecordingUrl} className="w-full h-8 rounded" />
        </div>
      )}
    </div>
  );
};
