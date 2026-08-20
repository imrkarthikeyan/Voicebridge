import React, { useState } from 'react';
import { QRCodeSVG } from 'qrcode.react';
import { AudioWaveform } from '../AudioWaveform';
import {
  Mic,
  MicOff,
  Radio,
  UserCheck,
  Users,
  CheckCircle,
  XCircle,
  ArrowUp,
  ArrowDown,
  Copy,
  Check,
  Clock,
} from 'lucide-react';

export const MeetingControlSidebar = ({
  meeting,
  participants = [],
  queue = [],
  activeSpeakerRequest = null,
  isReceivingAudio = false,
  audioLevel = 0,
  onApprove,
  onReject,
  onEndSpeaker,
  onReorder,
  onOpenQrModal,
}) => {
  const [copied, setCopied] = useState(false);

  const getDynamicJoinUrl = () => {
    if (meeting?.joinUrl && !meeting.joinUrl.includes('localhost')) {
      return meeting.joinUrl;
    }
    return `${window.location.origin}/join/${meeting?.meetingCode}`;
  };

  const fullJoinUrl = getDynamicJoinUrl();

  const copyToClipboard = () => {
    navigator.clipboard.writeText(fullJoinUrl);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const waitingQueue = queue.filter((q) => q.status === 'WAITING');

  return (
    <div className="flex flex-col space-y-4 w-full">
      {/* 1. Embedded QR Code Card */}
      <div className="bg-slate-900/90 border border-slate-800 rounded-2xl p-4 shadow-xl flex flex-col items-center text-center">
        <div className="flex items-center justify-between w-full mb-3">
          <span className="text-[11px] uppercase tracking-wider font-bold text-indigo-400 bg-indigo-500/10 px-2.5 py-0.5 rounded-full border border-indigo-500/20">
            Scan to Join
          </span>
          <span className="font-mono text-xs font-bold text-white bg-slate-800 px-2 py-0.5 rounded border border-slate-700">
            {meeting?.meetingCode}
          </span>
        </div>

        <div
          onClick={onOpenQrModal}
          className="bg-white p-3.5 rounded-xl cursor-pointer hover:scale-[1.02] transition-transform shadow-md my-1"
          title="Click to enlarge QR code"
        >
          <QRCodeSVG value={fullJoinUrl} size={130} level="M" includeMargin={false} />
        </div>

        <div className="mt-3 flex items-center space-x-2 w-full">
          <input
            type="text"
            readOnly
            value={fullJoinUrl}
            className="flex-1 bg-slate-950 text-slate-300 text-[11px] font-mono px-2.5 py-1.5 rounded-lg border border-slate-800 truncate focus:outline-none"
          />
          <button
            type="button"
            onClick={copyToClipboard}
            className="flex items-center space-x-1 px-2.5 py-1.5 bg-indigo-600 hover:bg-indigo-500 text-white text-xs font-semibold rounded-lg transition-colors shadow-sm"
          >
            {copied ? <Check className="w-3.5 h-3.5" /> : <Copy className="w-3.5 h-3.5" />}
            <span>{copied ? 'Copied' : 'Copy'}</span>
          </button>
        </div>
      </div>

      {/* 2. Active Speaker Card */}
      <div className="bg-slate-900/90 border border-slate-800 rounded-2xl p-4 shadow-xl space-y-3">
        <div className="flex items-center justify-between">
          <div className="flex items-center space-x-2">
            <div
              className={`p-1.5 rounded-lg ${
                activeSpeakerRequest ? 'bg-rose-500/20 text-rose-400 animate-pulse' : 'bg-slate-800 text-slate-500'
              }`}
            >
              <Mic className="w-4 h-4" />
            </div>
            <div>
              <h4 className="text-xs font-bold uppercase tracking-wider text-slate-300">Current Speaker</h4>
              <p className="text-[10px] text-slate-500">1 active speaker limit</p>
            </div>
          </div>

          {activeSpeakerRequest && (
            <button
              type="button"
              onClick={onEndSpeaker}
              className="flex items-center space-x-1 px-2.5 py-1.5 bg-rose-600 hover:bg-rose-500 text-white text-xs font-semibold rounded-lg shadow-sm transition-colors"
            >
              <MicOff className="w-3.5 h-3.5" />
              <span>End Speaker</span>
            </button>
          )}
        </div>

        {activeSpeakerRequest ? (
          <div className="space-y-3 pt-1">
            <div className="bg-slate-950 p-3 rounded-xl border border-slate-800/80 flex items-center justify-between">
              <div>
                <div className="text-sm font-bold text-white">{activeSpeakerRequest.participantName}</div>
                <div className="text-[10px] text-emerald-400 font-medium flex items-center space-x-1 mt-0.5">
                  <Radio className="w-3 h-3 animate-pulse" />
                  <span>WebRTC P2P Audio Connected</span>
                </div>
              </div>
              <span className="px-2 py-0.5 text-[10px] font-bold bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 rounded-full">
                {activeSpeakerRequest.status}
              </span>
            </div>

            <AudioWaveform
              isLive={isReceivingAudio || activeSpeakerRequest.status === 'SPEAKING'}
              audioLevel={audioLevel}
              label="Speaker Audio Input Level"
            />
          </div>
        ) : (
          <div className="py-5 text-center border border-dashed border-slate-800 rounded-xl bg-slate-950/40">
            <MicOff className="w-6 h-6 text-slate-600 mx-auto mb-1.5" />
            <p className="text-xs text-slate-400 font-medium">No Active Speaker</p>
            <p className="text-[10px] text-slate-500">Approve a participant request below</p>
          </div>
        )}
      </div>

      {/* 3. Speaking Queue Card */}
      <div className="bg-slate-900/90 border border-slate-800 rounded-2xl p-4 shadow-xl space-y-3">
        <div className="flex items-center justify-between">
          <div className="flex items-center space-x-2">
            <div className="p-1.5 bg-indigo-500/10 text-indigo-400 rounded-lg">
              <UserCheck className="w-4 h-4" />
            </div>
            <h4 className="text-xs font-bold uppercase tracking-wider text-slate-300">Speaking Queue</h4>
          </div>
          <span className="px-2 py-0.5 text-[10px] font-bold bg-indigo-500/10 text-indigo-400 border border-indigo-500/20 rounded-full">
            {waitingQueue.length} Waiting
          </span>
        </div>

        {waitingQueue.length === 0 ? (
          <div className="py-5 text-center border border-dashed border-slate-800 rounded-xl bg-slate-950/40">
            <p className="text-xs text-slate-500">No hands raised currently</p>
          </div>
        ) : (
          <div className="space-y-2 max-h-[260px] overflow-y-auto pr-0.5">
            {waitingQueue.map((req, idx) => (
              <div
                key={req.id}
                className="bg-slate-950 p-2.5 rounded-xl border border-slate-800 flex items-center justify-between text-xs"
              >
                <div className="flex items-center space-x-2.5 min-w-0">
                  <span className="w-5 h-5 bg-slate-800 text-slate-300 font-mono text-[10px] font-bold rounded flex items-center justify-center flex-shrink-0">
                    #{idx + 1}
                  </span>
                  <div className="truncate">
                    <div className="font-semibold text-white truncate">{req.participantName}</div>
                    <div className="text-[10px] text-slate-500 flex items-center space-x-1">
                      <Clock className="w-2.5 h-2.5" />
                      <span>{new Date(req.requestedAt).toLocaleTimeString()}</span>
                    </div>
                  </div>
                </div>

                <div className="flex items-center space-x-1 flex-shrink-0">
                  {/* Reorder Up / Down */}
                  <button
                    type="button"
                    onClick={() => onReorder(idx, 'up')}
                    disabled={idx === 0}
                    title="Move Up"
                    className="p-1 bg-slate-800 hover:bg-slate-700 text-slate-400 hover:text-white rounded disabled:opacity-30"
                  >
                    <ArrowUp className="w-3 h-3" />
                  </button>
                  <button
                    type="button"
                    onClick={() => onReorder(idx, 'down')}
                    disabled={idx === waitingQueue.length - 1}
                    title="Move Down"
                    className="p-1 bg-slate-800 hover:bg-slate-700 text-slate-400 hover:text-white rounded disabled:opacity-30"
                  >
                    <ArrowDown className="w-3 h-3" />
                  </button>

                  {/* Approve */}
                  <button
                    type="button"
                    onClick={() => onApprove(req.id)}
                    disabled={!!activeSpeakerRequest}
                    title={activeSpeakerRequest ? 'End current speaker first' : 'Approve Speaker'}
                    className="flex items-center space-x-1 px-2 py-1 bg-emerald-600 hover:bg-emerald-500 text-white rounded font-medium text-[11px] disabled:opacity-40 disabled:hover:bg-emerald-600"
                  >
                    <CheckCircle className="w-3 h-3" />
                    <span>Approve</span>
                  </button>

                  {/* Reject */}
                  <button
                    type="button"
                    onClick={() => onReject(req.id)}
                    title="Reject Request"
                    className="p-1 bg-slate-800 hover:bg-rose-600 text-slate-400 hover:text-white rounded transition-colors"
                  >
                    <XCircle className="w-3.5 h-3.5" />
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* 4. Joined Audience List Card */}
      <div className="bg-slate-900/90 border border-slate-800 rounded-2xl p-4 shadow-xl space-y-3">
        <div className="flex items-center justify-between">
          <div className="flex items-center space-x-2">
            <div className="p-1.5 bg-cyan-500/10 text-cyan-400 rounded-lg">
              <Users className="w-4 h-4" />
            </div>
            <h4 className="text-xs font-bold uppercase tracking-wider text-slate-300">Joined Audience</h4>
          </div>
          <span className="px-2 py-0.5 text-[10px] font-bold bg-cyan-500/10 text-cyan-400 border border-cyan-500/20 rounded-full">
            {participants.length} Online
          </span>
        </div>

        <div className="max-h-[180px] overflow-y-auto space-y-1.5 pr-0.5">
          {participants.length === 0 ? (
            <p className="text-slate-500 text-[11px] text-center py-4">Scan QR code to join meeting</p>
          ) : (
            participants.map((p) => (
              <div
                key={p.id}
                className="bg-slate-950 p-2 rounded-xl border border-slate-800/80 flex items-center justify-between text-xs"
              >
                <div className="flex items-center space-x-2 truncate">
                  <div className="w-5 h-5 rounded-full bg-slate-800 text-indigo-400 font-bold text-[10px] flex items-center justify-center flex-shrink-0">
                    {p.name.charAt(0).toUpperCase()}
                  </div>
                  <span className="text-slate-200 truncate text-[11px] font-medium">{p.name}</span>
                </div>
                <span className="w-2 h-2 rounded-full bg-emerald-400 flex-shrink-0" title="Connected" />
              </div>
            ))
          )}
        </div>
      </div>
    </div>
  );
};
