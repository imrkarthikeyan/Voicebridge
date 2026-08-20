import api from './axios';

export const uploadPresentation = async (meetingId, file, onUploadProgress) => {
  const formData = new FormData();
  formData.append('file', file);

  const response = await api.post(`/meetings/${meetingId}/presentations`, formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
    onUploadProgress,
  });
  return response.data.data;
};

export const getPresentations = async (meetingId) => {
  const response = await api.get(`/meetings/${meetingId}/presentations`);
  return response.data.data;
};

export const getPresentation = async (presentationId) => {
  const response = await api.get(`/presentations/${presentationId}`);
  return response.data.data;
};

export const deletePresentation = async (presentationId) => {
  const response = await api.get(`/presentations/${presentationId}`);
  return response.data.data;
};

export const removePresentation = async (presentationId) => {
  const response = await api.delete(`/presentations/${presentationId}`);
  return response.data.data;
};

export const startPresentation = async (presentationId) => {
  const response = await api.post(`/presentations/${presentationId}/start`);
  return response.data.data;
};

export const stopPresentation = async (presentationId) => {
  const response = await api.post(`/presentations/${presentationId}/stop`);
  return response.data.data;
};

export const changeSlide = async (presentationId, slideNumber) => {
  const response = await api.put(`/presentations/${presentationId}/slide`, { slideNumber });
  return response.data.data;
};

export const getPresentationSession = async (presentationId) => {
  const response = await api.get(`/presentations/${presentationId}/session`);
  return response.data.data;
};

export const getMeetingPresentationSession = async (meetingId) => {
  const response = await api.get(`/meetings/${meetingId}/presentation-session`);
  return response.data.data;
};

export const getSlideImageUrl = (presentationId, slideNumber) => {
  if (!presentationId || !slideNumber) return '';
  return `/api/presentations/${presentationId}/slides/${slideNumber}`;
};
