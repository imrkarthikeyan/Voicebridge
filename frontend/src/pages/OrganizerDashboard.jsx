import React, { useState, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { Navbar } from '../components/Navbar';
import { Footer } from '../components/Footer';
import { createMeeting, getMyMeetings } from '../api/meetings';
import { Plus, Radio, ArrowRight, Clock } from 'lucide-react';

export const OrganizerDashboard = () => {
  const [meetings, setMeetings] = useState([]);
  const [loading, setLoading] = useState(true);
  const [isCreating, setIsCreating] = useState(false);
  const [newTitle, setNewTitle] = useState('');
  const [createError, setCreateError] = useState('');

  const navigate = useNavigate();
  const token = localStorage.getItem('vb_token');

  useEffect(() => {
    if (!token) {
      navigate('/login');
      return;
    }

    fetchMeetings();
  }, [token, navigate]);

  const fetchMeetings = async () => {
    try {
      setLoading(true);
      const data = await getMyMeetings();
      setMeetings(data);
    } catch (err) {
      console.error('Failed to fetch meetings:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleCreateMeeting = async (e) => {
    e.preventDefault();
    if (!newTitle.trim()) {
      setCreateError('Please enter a session title');
      return;
    }

    try {
      setCreateError('');
      const meeting = await createMeeting({ title: newTitle.trim() });
      setIsCreating(false);
      setNewTitle('');
      navigate(`/room/${meeting.meetingCode}`);
    } catch (err) {
      console.error('Create meeting error:', err);
      setCreateError(err.response?.data?.message || 'Failed to create meeting session');
    }
  };

  return (
    <div className="min-h-screen flex flex-col bg-slate-950 text-slate-100">
      <Navbar />

      <main className="flex-1 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 w-full">
        {/* Header Title & Create CTA */}
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-8">
          <div>
            <h1 className="text-3xl font-bold font-heading text-white tracking-tight">Organizer Dashboard</h1>
            <p className="text-slate-400 text-sm mt-1">Manage and launch live audience microphone sessions</p>
          </div>

          <button
            onClick={() => setIsCreating(true)}
            className="flex items-center justify-center gap-2 px-5 py-3 rounded-xl bg-blue-600 hover:bg-blue-500 text-white font-semibold text-sm shadow-lg shadow-blue-600/30 transition-all cursor-pointer"
          >
            <Plus className="w-5 h-5" />
            <span>Create New Meeting</span>
          </button>
        </div>

        {/* Create Meeting Modal */}
        {isCreating && (
          <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-sm">
            <div className="bg-slate-900 border border-slate-800 rounded-2xl max-w-md w-full p-6 shadow-2xl relative glow-blue">
              <h2 className="text-xl font-bold font-heading text-white mb-1">Create Meeting Session</h2>
              <p className="text-slate-400 text-xs mb-6">Generates a unique QR Code for your auditorium audience</p>

              {createError && (
                <div className="mb-4 p-3 rounded-xl bg-red-500/10 border border-red-500/20 text-red-400 text-xs">
                  {createError}
                </div>
              )}

              <form onSubmit={handleCreateMeeting} className="space-y-4">
                <div>
                  <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-1.5">
                    Meeting / Seminar Title
                  </label>
                  <input
                    type="text"
                    required
                    placeholder="e.g. Placement Talk Q&A / CSE Seminar"
                    value={newTitle}
                    onChange={(e) => setNewTitle(e.target.value)}
                    className="w-full bg-slate-950 border border-slate-700 focus:border-blue-500 rounded-xl px-4 py-2.5 text-white text-sm focus:outline-none transition-colors"
                  />
                </div>

                <div className="flex items-center justify-end gap-3 pt-4 border-t border-slate-800">
                  <button
                    type="button"
                    onClick={() => setIsCreating(false)}
                    className="px-4 py-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs font-semibold"
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    className="px-5 py-2.5 rounded-xl bg-blue-600 hover:bg-blue-500 text-white text-xs font-semibold shadow-md shadow-blue-600/30"
                  >
                    Launch Session
                  </button>
                </div>
              </form>
            </div>
          </div>
        )}

        {/* Meeting List */}
        {loading ? (
          <div className="flex flex-col items-center justify-center py-16 text-slate-400">
            <div className="w-8 h-8 border-2 border-blue-500 border-t-transparent rounded-full animate-spin mb-4" />
            <span>Loading sessions...</span>
          </div>
        ) : meetings.length === 0 ? (
          <div className="text-center py-16 px-4 rounded-2xl border border-dashed border-slate-800 bg-slate-900/40">
            <Radio className="w-12 h-12 text-slate-600 mx-auto mb-3" />
            <h3 className="text-lg font-bold text-white font-heading">No Active Meetings Yet</h3>
            <p className="text-slate-400 text-sm max-w-md mx-auto mt-1 mb-6">
              Create your first session to display the audience QR Code and receive live microphone audio streams.
            </p>
            <button
              onClick={() => setIsCreating(true)}
              className="inline-flex items-center gap-2 px-4 py-2.5 rounded-xl bg-blue-600 hover:bg-blue-500 text-white text-xs font-semibold"
            >
              <Plus className="w-4 h-4" />
              <span>Create Session</span>
            </button>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {meetings.map((m) => (
              <div
                key={m.id}
                className="p-6 rounded-2xl glass-panel border border-slate-800 hover:border-blue-500/50 transition-all flex flex-col justify-between gap-4 group"
              >
                <div>
                  <div className="flex items-center justify-between gap-2 mb-3">
                    <span
                      className={`inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-semibold border ${
                        m.status === 'ACTIVE'
                          ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20'
                          : 'bg-slate-800 text-slate-400 border-slate-700'
                      }`}
                    >
                      <span className={`w-1.5 h-1.5 rounded-full ${m.status === 'ACTIVE' ? 'bg-emerald-400 animate-pulse' : 'bg-slate-500'}`} />
                      {m.status}
                    </span>
                    <span className="font-mono text-xs font-bold text-blue-400 tracking-wider">#{m.meetingCode}</span>
                  </div>

                  <h3 className="text-lg font-bold text-white font-heading group-hover:text-blue-300 transition-colors">
                    {m.title}
                  </h3>

                  <div className="flex items-center gap-2 text-xs text-slate-400 mt-2">
                    <Clock className="w-3.5 h-3.5" />
                    <span>{new Date(m.createdAt).toLocaleString()}</span>
                  </div>
                </div>

                <div className="pt-4 border-t border-slate-800/80 flex items-center justify-between">
                  <span className="text-xs text-slate-400">
                    {m.status === 'ACTIVE' ? 'Live Session Ready' : 'Closed'}
                  </span>
                  <Link
                    to={`/room/${m.meetingCode}`}
                    className="flex items-center gap-1.5 text-xs font-semibold px-3 py-2 rounded-lg bg-slate-800 hover:bg-blue-600 text-white transition-colors"
                  >
                    <span>{m.status === 'ACTIVE' ? 'Enter Control Room' : 'View Summary'}</span>
                    <ArrowRight className="w-3.5 h-3.5" />
                  </Link>
                </div>
              </div>
            ))}
          </div>
        )}
      </main>

      <Footer />
    </div>
  );
};
