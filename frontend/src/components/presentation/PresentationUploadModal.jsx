import React, { useState, useRef } from 'react';
import { uploadPresentation } from '../../api/presentations';
import { Upload, X, FileText, AlertCircle, Loader2 } from 'lucide-react';

export const PresentationUploadModal = ({ meetingId, isOpen, onClose, onUploadSuccess }) => {
  const [selectedFile, setSelectedFile] = useState(null);
  const [isUploading, setIsUploading] = useState(false);
  const [uploadProgress, setUploadProgress] = useState(0);
  const [error, setError] = useState(null);
  const [isDragOver, setIsDragOver] = useState(false);

  const fileInputRef = useRef(null);

  if (!isOpen) return null;

  const validateFile = (file) => {
    if (!file) return false;
    const name = file.name.toLowerCase();
    if (!name.endsWith('.pptx') && !name.endsWith('.pdf')) {
      setError('Unsupported file format. Only PPTX and PDF files are allowed.');
      return false;
    }
    // 50MB size limit
    if (file.size > 50 * 1024 * 1024) {
      setError('File size exceeds maximum allowed limit of 50MB.');
      return false;
    }
    setError(null);
    return true;
  };

  const handleFileChange = (e) => {
    const file = e.target.files?.[0];
    if (file && validateFile(file)) {
      setSelectedFile(file);
    }
  };

  const handleDrop = (e) => {
    e.preventDefault();
    setIsDragOver(false);
    const file = e.dataTransfer.files?.[0];
    if (file && validateFile(file)) {
      setSelectedFile(file);
    }
  };

  const handleDragOver = (e) => {
    e.preventDefault();
    setIsDragOver(true);
  };

  const handleDragLeave = (e) => {
    e.preventDefault();
    setIsDragOver(false);
  };

  const handleUpload = async () => {
    if (!selectedFile) return;

    try {
      setIsUploading(true);
      setError(null);
      setUploadProgress(0);

      const result = await uploadPresentation(meetingId, selectedFile, (progressEvent) => {
        if (progressEvent.total) {
          const percent = Math.round((progressEvent.loaded * 100) / progressEvent.total);
          setUploadProgress(percent);
        }
      });

      setSelectedFile(null);
      setIsUploading(false);
      onUploadSuccess?.(result);
      onClose();
    } catch (err) {
      console.error('Failed to upload presentation:', err);
      setError(err.response?.data?.message || 'Failed to upload and process presentation slides.');
      setIsUploading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-xs p-4 animate-in fade-in duration-200">
      <div className="bg-slate-900 border border-slate-800 rounded-2xl w-full max-w-lg shadow-2xl overflow-hidden">
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-slate-800">
          <div className="flex items-center space-x-3">
            <div className="p-2 bg-indigo-500/10 rounded-xl text-indigo-400">
              <Upload className="w-5 h-5" />
            </div>
            <h3 className="text-lg font-semibold text-white">Upload Presentation</h3>
          </div>
          <button
            onClick={onClose}
            disabled={isUploading}
            className="p-1 text-slate-400 hover:text-white rounded-lg transition-colors disabled:opacity-50"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Content */}
        <div className="p-6 space-y-4">
          {error && (
            <div className="flex items-center space-x-3 p-3.5 bg-rose-500/10 border border-rose-500/20 text-rose-400 text-sm rounded-xl">
              <AlertCircle className="w-5 h-5 flex-shrink-0" />
              <span>{error}</span>
            </div>
          )}

          {/* Dropzone */}
          <div
            onDrop={handleDrop}
            onDragOver={handleDragOver}
            onDragLeave={handleDragLeave}
            onClick={() => fileInputRef.current?.click()}
            className={`border-2 border-dashed rounded-2xl p-8 text-center cursor-pointer transition-all duration-200 ${
              isDragOver
                ? 'border-indigo-500 bg-indigo-500/10 scale-[0.99]'
                : selectedFile
                ? 'border-emerald-500/50 bg-emerald-500/5'
                : 'border-slate-700 hover:border-indigo-500/50 hover:bg-slate-800/50'
            }`}
          >
            <input
              ref={fileInputRef}
              type="file"
              accept=".pptx,.pdf"
              onChange={handleFileChange}
              className="hidden"
            />

            {selectedFile ? (
              <div className="flex flex-col items-center space-y-2">
                <div className="p-3 bg-emerald-500/10 text-emerald-400 rounded-2xl">
                  <FileText className="w-8 h-8" />
                </div>
                <div className="font-medium text-white text-base">{selectedFile.name}</div>
                <div className="text-xs text-slate-400">
                  {(selectedFile.size / (1024 * 1024)).toFixed(2)} MB
                </div>
                <span className="text-xs text-indigo-400 hover:underline pt-2">Click to change file</span>
              </div>
            ) : (
              <div className="flex flex-col items-center space-y-3">
                <div className="p-3 bg-slate-800 text-slate-400 rounded-2xl">
                  <Upload className="w-8 h-8" />
                </div>
                <div>
                  <p className="text-sm font-medium text-slate-200">
                    Drag & Drop presentation file here, or{' '}
                    <span className="text-indigo-400 hover:underline">browse</span>
                  </p>
                  <p className="text-xs text-slate-500 mt-1">Supports PowerPoint (.pptx) or PDF (.pdf) up to 50MB</p>
                </div>
              </div>
            )}
          </div>

          {/* Progress Bar */}
          {isUploading && (
            <div className="space-y-2 pt-2">
              <div className="flex justify-between text-xs text-slate-400 font-medium">
                <span className="flex items-center space-x-2">
                  <Loader2 className="w-3.5 h-3.5 animate-spin text-indigo-400" />
                  <span>Processing presentation slides...</span>
                </span>
                <span>{uploadProgress}%</span>
              </div>
              <div className="w-full bg-slate-800 rounded-full h-2 overflow-hidden">
                <div
                  className="bg-indigo-500 h-full rounded-full transition-all duration-300 ease-out"
                  style={{ width: `${uploadProgress}%` }}
                />
              </div>
            </div>
          )}
        </div>

        {/* Footer */}
        <div className="flex justify-end space-x-3 px-6 py-4 bg-slate-900/50 border-t border-slate-800">
          <button
            type="button"
            onClick={onClose}
            disabled={isUploading}
            className="px-4 py-2 text-sm font-medium text-slate-400 hover:text-white rounded-xl transition-colors disabled:opacity-50"
          >
            Cancel
          </button>
          <button
            type="button"
            onClick={handleUpload}
            disabled={!selectedFile || isUploading}
            className="flex items-center space-x-2 px-5 py-2 text-sm font-medium bg-indigo-600 hover:bg-indigo-500 text-white rounded-xl shadow-lg shadow-indigo-600/20 transition-all disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {isUploading ? (
              <>
                <Loader2 className="w-4 h-4 animate-spin" />
                <span>Processing...</span>
              </>
            ) : (
              <>
                <Upload className="w-4 h-4" />
                <span>Upload & Process</span>
              </>
            )}
          </button>
        </div>
      </div>
    </div>
  );
};
