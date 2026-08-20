import React from 'react';
import { useParams, Link } from 'react-router-dom';
import { Navbar } from '../components/Navbar';
import { Footer } from '../components/Footer';
import { CheckCircle2, Home } from 'lucide-react';

export const MeetingClosedPage = () => {
  const { meetingCode } = useParams();

  return (
    <div className="min-h-screen flex flex-col bg-slate-950 text-slate-100">
      <Navbar />

      <main className="flex-1 flex items-center justify-center p-4 py-12">
        <div className="w-full max-w-md text-center">
          <div className="p-8 rounded-2xl glass-panel border border-slate-800 shadow-2xl glow-blue">
            <div className="w-16 h-16 rounded-full bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 mx-auto flex items-center justify-center mb-4">
              <CheckCircle2 className="w-8 h-8" />
            </div>

            <span className="text-xs uppercase font-mono text-slate-400 font-semibold px-2 py-1 rounded bg-slate-800">
              Code: {meetingCode?.toUpperCase()}
            </span>

            <h1 className="text-2xl font-bold font-heading text-white mt-3">Meeting Closed</h1>
            <p className="text-slate-400 text-sm mt-2 leading-relaxed">
              This Q&A session has been concluded by the organizer. Thank you for participating!
            </p>

            <div className="mt-8">
              <Link
                to="/"
                className="inline-flex items-center justify-center gap-2 px-6 py-3 rounded-xl bg-blue-600 hover:bg-blue-500 text-white font-semibold text-sm shadow-lg shadow-blue-600/30 transition-all"
              >
                <Home className="w-4 h-4" />
                <span>Return to Home</span>
              </Link>
            </div>
          </div>
        </div>
      </main>

      <Footer />
    </div>
  );
};
