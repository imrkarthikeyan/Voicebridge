import React from 'react';
import { QRCodeSVG } from 'qrcode.react';
import { X, Copy, Check, ExternalLink, Download } from 'lucide-react';
import { useState } from 'react';

export const QrCodeModal = ({ isOpen, onClose, meetingCode, title, joinUrl }) => {
  const [copied, setCopied] = useState(false);

  if (!isOpen) return null;

  const getDynamicJoinUrl = () => {
    if (joinUrl && !joinUrl.includes('localhost')) {
      return joinUrl;
    }
    return `${window.location.origin}/join/${meetingCode}`;
  };

  const fullJoinUrl = getDynamicJoinUrl();

  const copyToClipboard = () => {
    navigator.clipboard.writeText(fullJoinUrl);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const downloadQrCode = () => {
    const svgElement = document.getElementById('meeting-qr-code');
    if (!svgElement) return;

    const svgData = new XMLSerializer().serializeToString(svgElement);
    const canvas = document.createElement('canvas');
    const ctx = canvas.getContext('2d');
    const img = new Image();

    img.onload = () => {
      canvas.width = img.width + 40;
      canvas.height = img.height + 40;
      ctx.fillStyle = '#ffffff';
      ctx.fillRect(0, 0, canvas.width, canvas.height);
      ctx.drawImage(img, 20, 20);

      const pngFile = canvas.toDataURL('image/png');
      const downloadLink = document.createElement('a');
      downloadLink.download = `VoiceBridge-QR-${meetingCode}.png`;
      downloadLink.href = pngFile;
      downloadLink.click();
    };

    img.src = 'data:image/svg+xml;base64,' + btoa(unescape(encodeURIComponent(svgData)));
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-sm animate-fade-in">
      <div className="bg-slate-900 border border-slate-800 rounded-2xl max-w-md w-full p-6 shadow-2xl relative glow-blue">
        <button
          onClick={onClose}
          className="absolute top-4 right-4 p-2 text-slate-400 hover:text-white rounded-lg hover:bg-slate-800 transition-colors"
        >
          <X className="w-5 h-5" />
        </button>

        <div className="text-center mb-6">
          <span className="text-xs uppercase tracking-wider font-semibold text-blue-400 px-3 py-1 rounded-full bg-blue-500/10 border border-blue-500/20">
            Join Audience Microphone Session
          </span>
          <h2 className="text-2xl font-bold font-heading text-white mt-2">{title || 'Audience Session'}</h2>
          <p className="text-slate-400 text-sm mt-1">Scan QR Code to join on your mobile phone</p>
        </div>

        {/* QR Code Canvas Card */}
        <div className="bg-white p-6 rounded-2xl flex flex-col items-center justify-center shadow-inner mx-auto max-w-[260px]">
          <QRCodeSVG
            id="meeting-qr-code"
            value={fullJoinUrl}
            size={200}
            level="H"
            includeMargin={true}
          />
          <div className="mt-2 text-center">
            <span className="text-xs text-slate-500 font-mono">Code:</span>
            <span className="ml-1 font-mono font-bold text-slate-900 text-lg tracking-widest">{meetingCode}</span>
          </div>
        </div>

        {/* Shareable Link Box */}
        <div className="mt-6 flex flex-col gap-3">
          <div className="flex items-center gap-2 bg-slate-950 p-2.5 rounded-xl border border-slate-800">
            <input
              type="text"
              readOnly
              value={fullJoinUrl}
              className="bg-transparent text-slate-300 text-xs font-mono w-full focus:outline-none px-2"
            />
            <button
              onClick={copyToClipboard}
              className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-blue-600 hover:bg-blue-500 text-white text-xs font-semibold shrink-0 transition-colors"
            >
              {copied ? <Check className="w-3.5 h-3.5" /> : <Copy className="w-3.5 h-3.5" />}
              <span>{copied ? 'Copied!' : 'Copy'}</span>
            </button>
          </div>

          <div className="flex items-center gap-2">
            <button
              onClick={downloadQrCode}
              className="w-full flex items-center justify-center gap-2 py-2.5 px-4 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-medium border border-slate-700 transition-colors"
            >
              <Download className="w-4 h-4 text-blue-400" />
              <span>Download QR Image</span>
            </button>

            <a
              href={fullJoinUrl}
              target="_blank"
              rel="noreferrer"
              className="w-full flex items-center justify-center gap-2 py-2.5 px-4 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-medium border border-slate-700 transition-colors"
            >
              <ExternalLink className="w-4 h-4 text-indigo-400" />
              <span>Open Join Link</span>
            </a>
          </div>
        </div>
      </div>
    </div>
  );
};
