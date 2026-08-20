import { useEffect, useRef, useState, useCallback } from 'react';

export const useSpeechRecognition = (isActive, onTranscript) => {
  const recognitionRef = useRef(null);
  const [transcript, setTranscript] = useState('');
  const [isListening, setIsListening] = useState(false);
  const [isSupported, setIsSupported] = useState(true);

  useEffect(() => {
    const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
    if (!SpeechRecognition) {
      setIsSupported(false);
      console.warn('Web Speech API is not supported in this browser.');
      return;
    }

    const recognition = new SpeechRecognition();
    recognition.continuous = true;
    recognition.interimResults = true;
    recognition.lang = 'en-US';

    recognition.onresult = (event) => {
      let currentText = '';
      let isFinalResult = false;

      for (let i = event.resultIndex; i < event.results.length; i++) {
        const result = event.results[i];
        currentText += result[0].transcript;
        if (result.isFinal) {
          isFinalResult = true;
        }
      }

      setTranscript(currentText);

      if (onTranscript && currentText.trim()) {
        onTranscript({
          text: currentText,
          isFinal: isFinalResult,
        });
      }
    };

    recognition.onerror = (event) => {
      console.warn('Speech recognition error:', event.error);
      if (event.error === 'not-allowed' || event.error === 'service-not-allowed') {
        setIsListening(false);
      }
    };

    recognition.onend = () => {
      setIsListening(false);
      // Restart if still active
      if (recognitionRef.current && isActive) {
        try {
          recognitionRef.current.start();
          setIsListening(true);
        } catch {
          // Ignore already started error
        }
      }
    };

    recognitionRef.current = recognition;

    return () => {
      if (recognitionRef.current) {
        try {
          recognitionRef.current.stop();
        } catch {}
        recognitionRef.current = null;
      }
    };
  }, [isActive, onTranscript]);

  const startListening = useCallback(() => {
    if (recognitionRef.current && !isListening) {
      try {
        recognitionRef.current.start();
        setIsListening(true);
      } catch (err) {
        console.warn('SpeechRecognition start error:', err);
      }
    }
  }, [isListening]);

  const stopListening = useCallback(() => {
    if (recognitionRef.current && isListening) {
      try {
        recognitionRef.current.stop();
        setIsListening(false);
      } catch (err) {
        console.warn('SpeechRecognition stop error:', err);
      }
    }
  }, [isListening]);

  useEffect(() => {
    if (isActive) {
      startListening();
    } else {
      stopListening();
    }
  }, [isActive, startListening, stopListening]);

  return {
    transcript,
    isListening,
    isSupported,
    startListening,
    stopListening,
  };
};
