import api from './axios';

export const raiseHand = async (sessionToken) => {
  const response = await api.post('/speaking-requests/raise-hand', null, {
    headers: {
      'X-Session-Token': sessionToken,
    },
  });
  return response.data;
};

export const getMyRequestStatus = async (sessionToken) => {
  const response = await api.get('/speaking-requests/my-status', {
    headers: {
      'X-Session-Token': sessionToken,
    },
  });
  return response.data;
};

export const getSpeakingQueue = async (meetingId) => {
  const response = await api.get(`/speaking-requests/meeting/${meetingId}`);
  return response.data;
};

export const approveSpeaker = async (requestId) => {
  const response = await api.post(`/speaking-requests/${requestId}/approve`);
  return response.data;
};

export const rejectSpeaker = async (requestId) => {
  const response = await api.post(`/speaking-requests/${requestId}/reject`);
  return response.data;
};

export const startSpeaking = async (sessionToken) => {
  const response = await api.post('/speaking-requests/start', null, {
    headers: {
      'X-Session-Token': sessionToken,
    },
  });
  return response.data;
};

export const stopSpeaking = async (sessionToken) => {
  const response = await api.post('/speaking-requests/stop', null, {
    headers: {
      'X-Session-Token': sessionToken,
    },
  });
  return response.data;
};

export const endCurrentSpeaker = async (meetingId) => {
  const response = await api.post(`/speaking-requests/meeting/${meetingId}/end-speaker`);
  return response.data;
};

export const reorderQueue = async (meetingId, orderedRequestIds) => {
  const response = await api.put(`/speaking-requests/meeting/${meetingId}/reorder`, {
    orderedRequestIds,
  });
  return response.data;
};
