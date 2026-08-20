import React from 'react';
import { Link } from 'react-router-dom';
import { Navbar } from '../components/Navbar';
import { Footer } from '../components/Footer';
import { AlertCircle, Home } from 'lucide-react';

export const NotFoundPage = () => {
  return (
    <div className="min-h-screen flex flex-col bg-slate-950 text-slate-100">
      <Navbar />

      <main className="flex-1 flex items-center justify-center p-4 py-12">
        <div className="w-full max-w-md text-center">
          <div className="p-8 rounded-2xl glass-panel border border-slate-800 shadow-2xl">
            <div className="w-16 h-16 rounded-full bg-blue-500/10 text-blue-400 border border-blue-500/20 mx-auto flex items-center justify-center mb-4">
              <AlertCircle className="w-8 h-8" />
            </div>

            <h1 className="text-4xl font-extrabold font-heading text-white">404</h1>
            <h2 className="text-xl font-bold font-heading text-slate-200 mt-2">Page Not Found</h2>
            <p className="text-slate-400 text-sm mt-2">
              The page or meeting link you are looking for does not exist.
            </p>

            <div className="mt-8">
              <Link
                to="/"
                className="inline-flex items-center justify-center gap-2 px-6 py-3 rounded-xl bg-blue-600 hover:bg-blue-500 text-white font-semibold text-sm shadow-lg transition-all"
              >
                <Home className="w-4 h-4" />
                <span>Go Back Home</span>
              </Link>
            </div>
          </div>
        </div>
      </main>

      <Footer />
    </div>
  );
};
