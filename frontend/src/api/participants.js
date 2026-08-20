import api from './axios';

export const joinMeeting = async (meetingCode, name) => {
  const response = await api.post(`/participants/join/${meetingCode}`, { name });
  return response.data;
};

export const getParticipantBySession = async (sessionToken) => {
  const response = await api.get(`/participants/session/${sessionToken}`);
  return response.data;
};

export const listParticipants = async (meetingId) => {
  const response = await api.get(`/participants/meeting/${meetingId}`);
  return response.data;
};

export const leaveMeeting = async (sessionToken) => {
  const response = await api.post(`/participants/leave`, null, {
    headers: {
      'X-Session-Token': sessionToken,
    },
  });
  return response.data;
};
