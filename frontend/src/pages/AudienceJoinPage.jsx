import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Navbar } from '../components/Navbar';
import { Footer } from '../components/Footer';
import { getMeetingJoinInfo } from '../api/meetings';
import { joinMeeting } from '../api/participants';
import { Mic, User, ArrowRight, AlertCircle, ShieldCheck } from 'lucide-react';

export const AudienceJoinPage = () => {
  const { meetingCode } = useParams();
  const navigate = useNavigate();

  const [name, setName] = useState('');
  const [meetingInfo, setMeetingInfo] = useState(null);
  const [loading, setLoading] = useState(true);
  const [joining, setJoining] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (!meetingCode) {
      navigate('/');
      return;
    }

    const fetchInfo = async () => {
      try {
        setLoading(true);
        const data = await getMeetingJoinInfo(meetingCode.toUpperCase());
        setMeetingInfo(data);
        if (data.status === 'CLOSED') {
          navigate(`/closed/${meetingCode}`);
        }
      } catch (err) {
        console.error('Failed to get meeting info:', err);
        setError('Invalid or expired meeting code');
      } finally {
        setLoading(false);
      }
    };

    fetchInfo();
  }, [meetingCode, navigate]);

  const handleJoin = async (e) => {
    e.preventDefault();
    if (!name.trim()) {
      setError('Please enter your name');
      return;
    }

    try {
      setJoining(true);
      setError('');
      const participant = await joinMeeting(meetingCode.toUpperCase(), name.trim());

      localStorage.setItem('vb_session_token', participant.sessionToken);
      localStorage.setItem('vb_participant_name', participant.name);
      localStorage.setItem('vb_meeting_code', meetingCode.toUpperCase());

      navigate(`/waiting/${meetingCode.toUpperCase()}`);
    } catch (err) {
      console.error('Join meeting error:', err);
      const msg = err.response?.data?.message || 'Failed to join meeting';
      setError(msg);
    } finally {
      setJoining(false);
    }
  };

  if (loading) {
    return (
      <div className="min-h-screen flex flex-col bg-slate-950 text-slate-100">
        <Navbar />
        <div className="flex-1 flex items-center justify-center">
          <div className="w-8 h-8 border-2 border-blue-500 border-t-transparent rounded-full animate-spin" />
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen flex flex-col bg-slate-950 text-slate-100">
      <Navbar />

      <main className="flex-1 flex items-center justify-center p-4 py-12">
        <div className="w-full max-w-md">
          <div className="p-8 rounded-2xl glass-panel border border-slate-800 shadow-2xl glow-blue">
            <div className="text-center mb-6">
              <div className="w-12 h-12 rounded-xl bg-blue-500/10 text-blue-400 border border-blue-500/20 mx-auto flex items-center justify-center mb-3">
                <Mic className="w-6 h-6" />
              </div>
              <span className="text-xs uppercase font-mono text-blue-400 font-semibold px-2.5 py-0.5 rounded-full bg-blue-500/10 border border-blue-500/20">
                Code: {meetingCode.toUpperCase()}
              </span>
              <h1 className="text-2xl font-bold font-heading text-white mt-2">
                {meetingInfo?.title || 'Audience Session'}
              </h1>
              <p className="text-slate-400 text-xs mt-1">Hosted by {meetingInfo?.organizerName || 'Organizer'}</p>
            </div>

            {error && (
              <div className="mb-6 p-3 rounded-xl bg-red-500/10 border border-red-500/20 flex items-center gap-2 text-red-400 text-xs">
                <AlertCircle className="w-4 h-4 shrink-0" />
                <span>{error}</span>
              </div>
            )}

            <form onSubmit={handleJoin} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-1.5">
                  Enter Your Name
                </label>
                <div className="relative">
                  <User className="w-4 h-4 text-slate-500 absolute left-3.5 top-1/2 -translate-y-1/2" />
                  <input
                    type="text"
                    required
                    placeholder="e.g. John Doe / Student ID"
                    value={name}
                    onChange={(e) => {
                      setName(e.target.value);
                      setError('');
                    }}
                    className="w-full bg-slate-900 border border-slate-700 focus:border-blue-500 rounded-xl pl-10 pr-4 py-3 text-white text-sm focus:outline-none transition-colors"
                  />
                </div>
              </div>

              <button
                type="submit"
                disabled={joining}
                className="w-full py-3 rounded-xl bg-gradient-to-r from-blue-600 to-indigo-600 hover:from-blue-500 hover:to-indigo-500 text-white font-semibold text-sm shadow-lg shadow-blue-600/30 flex items-center justify-center gap-2 transition-all disabled:opacity-50 mt-2"
              >
                {joining ? (
                  <span>Joining Session...</span>
                ) : (
                  <>
                    <span>Join Session</span>
                    <ArrowRight className="w-4 h-4" />
                  </>
                )}
              </button>
            </form>

            <div className="mt-6 pt-4 border-t border-slate-800 text-center text-xs text-slate-500 flex items-center justify-center gap-1.5">
              <ShieldCheck className="w-3.5 h-3.5 text-emerald-400" />
              <span>No account or app download required</span>
            </div>
          </div>
        </div>
      </main>

      <Footer />
    </div>
  );
};
