import api from './axios';

export const loginOrganizer = async (credentials) => {
  const response = await api.post('/auth/login', credentials);
  return response.data;
};

export const registerOrganizer = async (data) => {
  const response = await api.post('/auth/register', data);
  return response.data;
};

export const getOrganizerProfile = async () => {
  const response = await api.get('/auth/me');
  return response.data;
};
