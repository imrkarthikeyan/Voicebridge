import api from './axios';

export const createMeeting = async (data) => {
  const response = await api.post('/meetings', data);
  return response.data;
};

export const getMyMeetings = async () => {
  const response = await api.get('/meetings');
  return response.data;
};

export const getMeetingByCode = async (code) => {
  const response = await api.get(`/meetings/${code}`);
  return response.data;
};

export const getMeetingJoinInfo = async (code) => {
  const response = await api.get(`/meetings/join/${code}`);
  return response.data;
};

export const closeMeeting = async (code) => {
  const response = await api.post(`/meetings/${code}/close`);
  return response.data;
};

export const getQrCodeUrl = (code) => {
  return `/api/meetings/${code}/qr`;
};
