import { useEffect, useRef, useState, useCallback } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

export const useStompWebSocket = (meetingCode) => {
  const stompClientRef = useRef(null);
  const [isConnected, setIsConnected] = useState(false);
  const [lastEvent, setLastEvent] = useState(null);
  const [signalMessage, setSignalMessage] = useState(null);

  useEffect(() => {
    if (!meetingCode) return;

    // Create STOMP client over SockJS
    const client = new Client({
      webSocketFactory: () => new SockJS('/ws'),
      reconnectDelay: 3000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      debug: (_str) => {
        if (import.meta.env.DEV) {
          // console.log('[STOMP]', _str);
        }
      },
      onConnect: () => {
        setIsConnected(true);

        // Subscribe to meeting events (join, leave, raise hand, approved, speaker start, speaker end, closed)
        client.subscribe(`/topic/meetings/${meetingCode}`, (message) => {
          try {
            const body = JSON.parse(message.body);
            setLastEvent(body);
          } catch (e) {
            console.error('Failed to parse meeting event:', e);
          }
        });

        // Subscribe to WebRTC signaling messages
        client.subscribe(`/topic/meetings/${meetingCode}/signal`, (message) => {
          try {
            const body = JSON.parse(message.body);
            setSignalMessage(body);
          } catch (e) {
            console.error('Failed to parse signaling message:', e);
          }
        });
      },
      onDisconnect: () => {
        setIsConnected(false);
      },
      onStompError: (frame) => {
        console.error('STOMP Error:', frame.headers['message']);
      },
    });

    client.activate();
    stompClientRef.current = client;

    return () => {
      if (stompClientRef.current) {
        stompClientRef.current.deactivate();
      }
    };
  }, [meetingCode]);

  const sendSignal = useCallback((signalData) => {
    if (stompClientRef.current && stompClientRef.current.connected && meetingCode) {
      stompClientRef.current.publish({
        destination: `/app/meetings/${meetingCode}/signal`,
        body: JSON.stringify(signalData),
      });
    }
  }, [meetingCode]);

  return {
    isConnected,
    lastEvent,
    signalMessage,
    sendSignal,
  };
};
