import React from 'react';
import {
  ChevronLeft,
  ChevronRight,
  ChevronsLeft,
  ChevronsRight,
  Maximize2,
  Minimize2,
  Play,
  Square,
} from 'lucide-react';

export const PresentationControls = ({
  currentSlide = 1,
  totalSlides = 1,
  isPresenting = false,
  isFullscreen = false,
  isOrganizer = true,
  onFirstSlide,
  onPreviousSlide,
  onNextSlide,
  onLastSlide,
  onJumpToSlide,
  onStartPresentation,
  onStopPresentation,
  onToggleFullscreen,
}) => {
  const handleInputChange = (e) => {
    const val = parseInt(e.target.value, 10);
    if (!isNaN(val) && val >= 1 && val <= totalSlides) {
      onJumpToSlide(val);
    }
  };

  return (
    <div className="flex flex-wrap items-center justify-between gap-3 px-4 py-3 bg-slate-900/90 border border-slate-800 rounded-2xl shadow-xl backdrop-blur-md">
      {/* Navigation buttons */}
      <div className="flex items-center space-x-1">
        {isOrganizer && (
          <>
            <button
              type="button"
              onClick={onFirstSlide}
              disabled={currentSlide <= 1}
              title="First Slide (Home)"
              className="p-2 text-slate-300 hover:text-white hover:bg-slate-800 rounded-xl transition-all disabled:opacity-40 disabled:hover:bg-transparent"
            >
              <ChevronsLeft className="w-4 h-4" />
            </button>

            <button
              type="button"
              onClick={onPreviousSlide}
              disabled={currentSlide <= 1}
              title="Previous Slide (Left Arrow / PageUp)"
              className="flex items-center space-x-1 px-3 py-1.5 text-xs font-medium text-slate-300 hover:text-white hover:bg-slate-800 rounded-xl transition-all disabled:opacity-40 disabled:hover:bg-transparent"
            >
              <ChevronLeft className="w-4 h-4" />
              <span className="hidden sm:inline">Prev</span>
            </button>
          </>
        )}

        {/* Slide Counter / Input */}
        <div className="flex items-center space-x-1.5 px-3 py-1 bg-slate-800/80 border border-slate-700/50 rounded-xl text-xs">
          {isOrganizer ? (
            <input
              type="number"
              min={1}
              max={totalSlides}
              value={currentSlide}
              onChange={handleInputChange}
              className="w-10 bg-slate-900 text-center font-bold text-indigo-400 border border-slate-700 rounded-lg py-0.5 focus:outline-none focus:border-indigo-500"
            />
          ) : (
            <span className="font-bold text-indigo-400">{currentSlide}</span>
          )}
          <span className="text-slate-500 font-medium">/</span>
          <span className="text-slate-300 font-medium">{totalSlides}</span>
        </div>

        {isOrganizer && (
          <>
            <button
              type="button"
              onClick={onNextSlide}
              disabled={currentSlide >= totalSlides}
              title="Next Slide (Right Arrow / PageDown)"
              className="flex items-center space-x-1 px-3 py-1.5 text-xs font-medium text-slate-300 hover:text-white hover:bg-slate-800 rounded-xl transition-all disabled:opacity-40 disabled:hover:bg-transparent"
            >
              <span className="hidden sm:inline">Next</span>
              <ChevronRight className="w-4 h-4" />
            </button>

            <button
              type="button"
              onClick={onLastSlide}
              disabled={currentSlide >= totalSlides}
              title="Last Slide (End)"
              className="p-2 text-slate-300 hover:text-white hover:bg-slate-800 rounded-xl transition-all disabled:opacity-40 disabled:hover:bg-transparent"
            >
              <ChevronsRight className="w-4 h-4" />
            </button>
          </>
        )}
      </div>

      {/* Right Controls */}
      <div className="flex items-center space-x-2">
        {isOrganizer && (
          <button
            type="button"
            onClick={isPresenting ? onStopPresentation : onStartPresentation}
            className={`flex items-center space-x-1.5 px-3.5 py-1.5 text-xs font-semibold rounded-xl transition-all shadow-md ${
              isPresenting
                ? 'bg-rose-600/90 hover:bg-rose-600 text-white shadow-rose-600/20'
                : 'bg-emerald-600/90 hover:bg-emerald-600 text-white shadow-emerald-600/20'
            }`}
          >
            {isPresenting ? (
              <>
                <Square className="w-3.5 h-3.5 fill-current" />
                <span>Exit Presentation</span>
              </>
            ) : (
              <>
                <Play className="w-3.5 h-3.5 fill-current" />
                <span>Start Presentation</span>
              </>
            )}
          </button>
        )}

        <button
          type="button"
          onClick={onToggleFullscreen}
          title={isFullscreen ? 'Exit Fullscreen' : 'Enter Fullscreen'}
          className="p-2 text-slate-300 hover:text-white hover:bg-slate-800 rounded-xl transition-all"
        >
          {isFullscreen ? <Minimize2 className="w-4 h-4" /> : <Maximize2 className="w-4 h-4" />}
        </button>
      </div>
    </div>
  );
};
