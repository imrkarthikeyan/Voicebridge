import React, { useState } from 'react';
import { Presentation, Play, Trash2, FileText, Plus, Clock, CheckCircle2 } from 'lucide-react';
import { removePresentation } from '../../api/presentations';

export const PresentationSelector = ({
  presentations = [],
  activePresentationId,
  isPresenting,
  onSelectPresentation,
  onStartPresentation,
  onOpenUpload,
  onRefresh,
}) => {
  const [deletingId, setDeletingId] = useState(null);
  const [confirmDeleteId, setConfirmDeleteId] = useState(null);
  const [error, setError] = useState(null);

  const handleDelete = async (id) => {
    try {
      setDeletingId(id);
      setError(null);
      await removePresentation(id);
      setConfirmDeleteId(null);
      setDeletingId(null);
      onRefresh?.();
    } catch (err) {
      console.error('Failed to delete presentation:', err);
      setError(err.response?.data?.message || 'Failed to delete presentation');
      setDeletingId(null);
    }
  };

  return (
    <div className="bg-slate-900/80 border border-slate-800 rounded-2xl p-5 space-y-4">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div className="flex items-center space-x-2.5">
          <div className="p-2 bg-indigo-500/10 text-indigo-400 rounded-xl">
            <Presentation className="w-5 h-5" />
          </div>
          <div>
            <h3 className="text-base font-semibold text-white">Presentations</h3>
            <p className="text-xs text-slate-400">Uploaded slides for this meeting</p>
          </div>
        </div>
        <button
          type="button"
          onClick={onOpenUpload}
          className="flex items-center space-x-1.5 px-3 py-1.5 text-xs font-medium bg-indigo-600 hover:bg-indigo-500 text-white rounded-xl transition-all shadow-sm shadow-indigo-600/20"
        >
          <Plus className="w-4 h-4" />
          <span>Upload File</span>
        </button>
      </div>

      {error && (
        <div className="p-3 bg-rose-500/10 border border-rose-500/20 text-rose-400 text-xs rounded-xl">
          {error}
        </div>
      )}

      {/* List */}
      {presentations.length === 0 ? (
        <div className="text-center py-8 border border-dashed border-slate-800 rounded-xl space-y-2">
          <FileText className="w-8 h-8 text-slate-600 mx-auto" />
          <p className="text-sm text-slate-400 font-medium">No presentations uploaded yet</p>
          <p className="text-xs text-slate-500">Upload a PPTX or PDF file to present slides during your meeting.</p>
        </div>
      ) : (
        <div className="space-y-2.5">
          {presentations.map((item) => {
            const isActive = item.id === activePresentationId;

            return (
              <div
                key={item.id}
                className={`flex items-center justify-between p-3.5 rounded-xl border transition-all ${
                  isActive
                    ? 'bg-indigo-500/10 border-indigo-500/40 shadow-sm'
                    : 'bg-slate-800/40 border-slate-800 hover:border-slate-700'
                }`}
              >
                <div className="flex items-center space-x-3 min-w-0">
                  <div
                    className={`p-2.5 rounded-xl ${
                      isActive ? 'bg-indigo-500 text-white' : 'bg-slate-800 text-slate-400'
                    }`}
                  >
                    <FileText className="w-5 h-5 flex-shrink-0" />
                  </div>

                  <div className="min-w-0">
                    <div className="flex items-center space-x-2">
                      <span className="font-medium text-white text-sm truncate">{item.fileName}</span>
                      {isActive && isPresenting && (
                        <span className="px-2 py-0.5 text-[10px] font-semibold bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 rounded-full flex items-center space-x-1">
                          <CheckCircle2 className="w-3 h-3" />
                          <span>LIVE</span>
                        </span>
                      )}
                    </div>
                    <div className="flex items-center space-x-3 text-xs text-slate-400 mt-0.5">
                      <span>{item.totalSlides} slides</span>
                      <span>•</span>
                      <span className="flex items-center space-x-1">
                        <Clock className="w-3 h-3 text-slate-500" />
                        <span>{new Date(item.uploadedAt).toLocaleDateString()}</span>
                      </span>
                    </div>
                  </div>
                </div>

                {/* Actions */}
                <div className="flex items-center space-x-2">
                  <button
                    type="button"
                    onClick={() => {
                      onSelectPresentation(item);
                      if (!isPresenting || !isActive) {
                        onStartPresentation(item.id);
                      }
                    }}
                    className={`flex items-center space-x-1.5 px-3 py-1.5 text-xs font-medium rounded-xl transition-all ${
                      isActive && isPresenting
                        ? 'bg-emerald-600 hover:bg-emerald-500 text-white'
                        : 'bg-indigo-600/20 hover:bg-indigo-600 text-indigo-300 hover:text-white border border-indigo-500/30'
                    }`}
                  >
                    <Play className="w-3.5 h-3.5" />
                    <span>{isActive && isPresenting ? 'Presenting' : 'Open Slide'}</span>
                  </button>

                  {confirmDeleteId === item.id ? (
                    <div className="flex items-center space-x-1 animate-in fade-in duration-150">
                      <button
                        type="button"
                        onClick={() => handleDelete(item.id)}
                        disabled={deletingId === item.id}
                        className="px-2 py-1 text-xs bg-rose-600 hover:bg-rose-500 text-white rounded-lg font-medium"
                      >
                        Confirm
                      </button>
                      <button
                        type="button"
                        onClick={() => setConfirmDeleteId(null)}
                        className="px-2 py-1 text-xs bg-slate-800 text-slate-400 hover:text-white rounded-lg"
                      >
                        Cancel
                      </button>
                    </div>
                  ) : (
                    <button
                      type="button"
                      onClick={() => setConfirmDeleteId(item.id)}
                      title="Delete Presentation"
                      className="p-1.5 text-slate-400 hover:text-rose-400 hover:bg-rose-500/10 rounded-lg transition-colors"
                    >
                      <Trash2 className="w-4 h-4" />
                    </button>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
};
