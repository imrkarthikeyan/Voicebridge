import React from 'react';

export const AudioWaveform = ({ isLive, audioLevel = 0, label = 'Audio Signal' }) => {
  // Generate 16 dynamic waveform bars based on audioLevel
  const barCount = 16;
  const bars = Array.from({ length: barCount }, (_, i) => {
    if (!isLive) return 10;
    // Calculate pseudo-random height scaled by real audio level
    const factor = Math.sin((i / barCount) * Math.PI) * 0.8 + 0.2;
    const levelHeight = Math.max(12, Math.min(100, audioLevel * factor * 1.5));
    return levelHeight;
  });

  return (
    <div className="w-full bg-slate-950/80 rounded-xl p-4 border border-slate-800 flex flex-col items-center justify-center gap-3">
      <div className="flex items-center justify-between w-full text-xs text-slate-400 font-medium">
        <span className="flex items-center gap-2">
          <span className={`w-2 h-2 rounded-full ${isLive ? 'bg-red-500 animate-ping' : 'bg-slate-600'}`} />
          {label}
        </span>
        <span className="font-mono text-slate-300">{isLive ? `${audioLevel}% Vol` : 'Idle'}</span>
      </div>

      <div className="h-16 w-full flex items-end justify-center gap-1.5 px-4 py-2 bg-slate-900/60 rounded-lg border border-slate-800/80">
        {bars.map((height, idx) => (
          <div
            key={idx}
            className={`w-1.5 rounded-full transition-all duration-75 ${
              isLive
                ? 'bg-gradient-to-t from-blue-600 via-indigo-400 to-emerald-400'
                : 'bg-slate-750/50 h-2'
            }`}
            style={{
              height: isLive ? `${height}%` : '8px',
              minHeight: '4px',
            }}
          />
        ))}
      </div>
    </div>
  );
};
