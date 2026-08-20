import React from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Mic, LogOut, Shield } from 'lucide-react';

export const Navbar = ({ isConnected }) => {
  const navigate = useNavigate();
  const token = localStorage.getItem('vb_token');
  const organizerStr = localStorage.getItem('vb_organizer');
  const organizer = organizerStr ? JSON.parse(organizerStr) : null;

  const handleLogout = () => {
    localStorage.removeItem('vb_token');
    localStorage.removeItem('vb_organizer');
    navigate('/login');
  };

  return (
    <header className="sticky top-0 z-40 w-full border-b border-slate-800 bg-slate-900/80 backdrop-blur-md">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
        {/* Brand Logo */}
        <Link to="/" className="flex items-center gap-3 group">
          <div className="w-10 h-10 rounded-xl bg-gradient-to-tr from-blue-600 via-indigo-600 to-cyan-400 p-0.5 shadow-lg group-hover:shadow-blue-500/25 transition-all">
            <div className="w-full h-full bg-slate-950 rounded-[10px] flex items-center justify-center">
              <Mic className="w-5 h-5 text-blue-400 group-hover:scale-110 transition-transform" />
            </div>
          </div>
          <div>
            <div className="flex items-center gap-2">
              <span className="font-heading text-xl font-bold text-white tracking-tight">VoiceBridge</span>
              <span className="text-[10px] uppercase tracking-wider px-2 py-0.5 rounded-full bg-blue-500/10 text-blue-400 font-semibold border border-blue-500/20">
                Live Mic
              </span>
            </div>
          </div>
        </Link>

        {/* Status Indicator & Navigation */}
        <div className="flex items-center gap-4">
          {isConnected !== undefined && (
            <div className="hidden sm:flex items-center gap-2 px-3 py-1 rounded-full bg-slate-800/80 border border-slate-700/50 text-xs">
              <span className={`w-2 h-2 rounded-full ${isConnected ? 'bg-emerald-400 animate-pulse' : 'bg-amber-400'}`} />
              <span className="text-slate-300 font-medium">{isConnected ? 'STOMP Connected' : 'Connecting WS...'}</span>
            </div>
          )}

          {token ? (
            <div className="flex items-center gap-3">
              <Link
                to="/dashboard"
                className="flex items-center gap-2 text-sm font-medium text-slate-300 hover:text-white px-3 py-2 rounded-lg hover:bg-slate-800 transition-colors"
              >
                <Shield className="w-4 h-4 text-blue-400" />
                <span>Dashboard</span>
              </Link>

              <div className="h-4 w-px bg-slate-800 hidden sm:block" />

              <span className="hidden md:inline-block text-sm text-slate-400 font-medium">
                {organizer?.name || 'Organizer'}
              </span>

              <button
                onClick={handleLogout}
                className="flex items-center gap-1.5 text-xs font-semibold px-3 py-2 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 hover:text-white transition-all border border-slate-700"
                title="Logout"
              >
                <LogOut className="w-3.5 h-3.5" />
                <span className="hidden sm:inline">Logout</span>
              </button>
            </div>
          ) : (
            <div className="flex items-center gap-2">
              <Link
                to="/login"
                className="text-sm font-medium text-slate-300 hover:text-white px-3 py-2 rounded-lg hover:bg-slate-800 transition-colors"
              >
                Organizer Login
              </Link>
              <Link
                to="/register"
                className="text-sm font-semibold px-4 py-2 rounded-lg bg-blue-600 hover:bg-blue-500 text-white shadow-md shadow-blue-600/20 transition-all"
              >
                Register
              </Link>
            </div>
          )}
        </div>
      </div>
    </header>
  );
};
