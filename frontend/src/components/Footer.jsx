import React from 'react';
import { Mic, ShieldCheck, Radio } from 'lucide-react';

export const Footer = () => {
  return (
    <footer className="border-t border-slate-800 bg-slate-950 py-8 text-slate-400 text-xs">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 flex flex-col md:flex-row items-center justify-between gap-4">
        <div className="flex items-center gap-2">
          <div className="w-6 h-6 rounded-md bg-blue-600/20 text-blue-400 flex items-center justify-center">
            <Mic className="w-3.5 h-3.5" />
          </div>
          <span className="font-heading font-semibold text-slate-200 text-sm">VoiceBridge</span>
          <span className="text-slate-600">|</span>
          <span>QR-Based Smart Audience Microphone System</span>
        </div>

        <div className="flex items-center gap-6 text-slate-400">
          <span className="flex items-center gap-1">
            <Radio className="w-3.5 h-3.5 text-blue-400" /> WebRTC Audio Engine
          </span>
          <span className="flex items-center gap-1">
            <ShieldCheck className="w-3.5 h-3.5 text-emerald-400" /> Peer-to-Peer Encrypted
          </span>
        </div>

        <div className="text-slate-500 text-center md:text-right">
          &copy; {new Date().getFullYear()} VoiceBridge. Built for Seminars & Placement Talks.
        </div>
      </div>
    </footer>
  );
};
