import { useState, useEffect, useRef, useCallback, useLayoutEffect } from 'react';
import {
  fetchProctoringConfig,
  getProctoringWsUrl,
  initializeProctoringSession,
  endProctoringSession,
} from '../api/proctoringService';
import { persistReferenceImage } from '../utils/referenceImagePersistence';
import { syncLifeDeductionToBackend, forceBackendAutoSubmitIfNeeded } from '../utils/proctoringSync';

const FRAME_INTERVAL_MS = 200;
const CANVAS_WIDTH = 640;
const CANVAS_HEIGHT = 480;

export default function useProctoring(attemptId, enabled) {
  const [config, setConfig] = useState(null);
  const [connected, setConnected] = useState(false);
  const [cameraReady, setCameraReady] = useState(false);
  const [cameraError, setCameraError] = useState(null);
  const [referenceCaptured, setReferenceCaptured] = useState(false);
  const [capturingReference, setCapturingReference] = useState(false);
  const [sessionReady, setSessionReady] = useState(false);
  const [maxLives, setMaxLives] = useState(null);
  const [remainingLives, setRemainingLives] = useState(null);
  const [statusMessages, setStatusMessages] = useState([]);
  const [verificationStatus, setVerificationStatus] = useState('N/A');
  const [faceDetected, setFaceDetected] = useState(false);
  const [riskLevel, setRiskLevel] = useState('LOW');
  const [sessionTerminated, setSessionTerminated] = useState(false);
  const [terminationReason, setTerminationReason] = useState(null);
  const [initializing, setInitializing] = useState(false);

  const videoRef = useRef(null);
  const canvasRef = useRef(null);
  const wsRef = useRef(null);
  const streamRef = useRef(null);
  const frameTimerRef = useRef(null);
  const captureReferenceRef = useRef(false);
  const syncingRef = useRef(false);
  const lastFrameRef = useRef(null);
  const capturedReferenceFrameRef = useRef(null);
  const onTerminatedRef = useRef(null);
  const initializedRef = useRef(false);

  const reattachStream = useCallback(() => {
    const video = videoRef.current;
    const stream = streamRef.current;
    if (video && stream && video.srcObject !== stream) {
      video.srcObject = stream;
      video.play().catch(() => {});
    }
  }, []);

  const stopFrameLoop = useCallback(() => {
    if (frameTimerRef.current) {
      clearTimeout(frameTimerRef.current);
      frameTimerRef.current = null;
    }
  }, []);

  const captureFrameBase64 = useCallback(() => {
    const video = videoRef.current;
    const canvas = canvasRef.current;
    if (!video || !canvas || video.readyState < 2) return null;
    const ctx = canvas.getContext('2d');
    ctx.drawImage(video, 0, 0, CANVAS_WIDTH, CANVAS_HEIGHT);
    const dataUrl = canvas.toDataURL('image/jpeg', 0.8);
    lastFrameRef.current = dataUrl;
    return dataUrl;
  }, []);

  const handlePythonMessage = useCallback(
    async (data) => {
      if (data.action === 'session_reset') {
        if (data.max_lives != null) setMaxLives(data.max_lives);
        return;
      }

      if (data.action === 'reference_captured') {
        setCapturingReference(false);
        if (data.success) {
          const frameUrl = capturedReferenceFrameRef.current || lastFrameRef.current;
          if (attemptId && frameUrl) {
            try {
              await persistReferenceImage(attemptId, frameUrl);
              setReferenceCaptured(true);
              setStatusMessages([data.message || 'Reference image captured.']);
            } catch (err) {
              setReferenceCaptured(false);
              const message =
                err.response?.data?.message ||
                err.message ||
                'Failed to save reference image. Please try again.';
              setStatusMessages([message]);
            }
          } else {
            setStatusMessages(['Reference capture failed: missing image data.']);
          }
          if (data.max_lives != null) setMaxLives(data.max_lives);
        } else {
          setStatusMessages([data.message || 'Reference capture failed.']);
        }
        capturedReferenceFrameRef.current = null;
        return;
      }

      if (data.max_lives != null) setMaxLives(data.max_lives);
      if (data.remaining_lives != null) setRemainingLives(data.remaining_lives);
      if (data.verification_status) setVerificationStatus(data.verification_status);
      if (data.face_detected != null) setFaceDetected(data.face_detected);
      if (data.risk_level) setRiskLevel(data.risk_level);
      if (Array.isArray(data.status_messages)) {
        setStatusMessages(data.status_messages);
      }

      if (data.life_deducted && data.violation_type && attemptId && !syncingRef.current) {
        syncingRef.current = true;
        try {
          const syncResult = await syncLifeDeductionToBackend(
            attemptId,
            data,
            lastFrameRef.current
          );
          if (syncResult?.remainingLivesAfter != null) {
            setRemainingLives(syncResult.remainingLivesAfter);
          }
        } catch {
          // Backend sync failure should not stop local monitoring
        } finally {
          syncingRef.current = false;
        }
      }

      if (data.session_terminated) {
        setSessionTerminated(true);
        setTerminationReason(data.termination_reason);
        stopFrameLoop();

        if (attemptId) {
          try {
            await forceBackendAutoSubmitIfNeeded(attemptId);
            await endProctoringSession(attemptId);
          } catch {
            // Session may already be terminated on backend
          }
          onTerminatedRef.current?.(data.termination_reason);
        }
      }
    },
    [attemptId, stopFrameLoop]
  );

  const sendFrame = useCallback(() => {
    const ws = wsRef.current;
    if (!ws || ws.readyState !== WebSocket.OPEN || sessionTerminated) return;

    const frameBase64 = captureFrameBase64();
    if (!frameBase64) {
      frameTimerRef.current = setTimeout(sendFrame, FRAME_INTERVAL_MS);
      return;
    }

    const payload = { frame: frameBase64 };
    if (captureReferenceRef.current) {
      payload.action = 'capture_reference';
      capturedReferenceFrameRef.current = frameBase64;
      captureReferenceRef.current = false;
    }

    ws.send(JSON.stringify(payload));
    frameTimerRef.current = setTimeout(sendFrame, FRAME_INTERVAL_MS);
  }, [captureFrameBase64, sessionTerminated]);

  const connectWebSocket = useCallback(() => {
    if (wsRef.current?.readyState === WebSocket.OPEN) return;

    const ws = new WebSocket(getProctoringWsUrl());
    wsRef.current = ws;

    ws.onopen = () => {
      setConnected(true);
      ws.send(JSON.stringify({ action: 'reset_session' }));
      sendFrame();
    };

    ws.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data);
        handlePythonMessage(data);
      } catch {
        // ignore malformed messages
      }
    };

    ws.onclose = () => setConnected(false);
    ws.onerror = () => setConnected(false);
  }, [handlePythonMessage, sendFrame]);

  const startCamera = useCallback(async () => {
    try {
      const stream = await navigator.mediaDevices.getUserMedia({
        video: { width: CANVAS_WIDTH, height: CANVAS_HEIGHT },
      });
      streamRef.current = stream;
      if (videoRef.current) {
        videoRef.current.srcObject = stream;
        await videoRef.current.play();
      }
      setCameraReady(true);
      setCameraError(null);
    } catch (err) {
      setCameraError('Camera access denied. Proctoring requires camera permission.');
      setCameraReady(false);
    }
  }, []);

  const initialize = useCallback(async () => {
    if (!enabled || !attemptId) return;
    setInitializing(true);
    try {
      const proctoringConfig = await fetchProctoringConfig();
      await initializeProctoringSession(attemptId);

      setConfig(proctoringConfig);
      if (proctoringConfig?.max_lives != null) {
        setMaxLives(proctoringConfig.max_lives);
        setRemainingLives(proctoringConfig.max_lives);
      }

      await startCamera();
      connectWebSocket();
      setSessionReady(true);
    } catch (err) {
      setCameraError(
        err.response?.data?.message ||
          err.message ||
          'Failed to initialize proctoring. Ensure the quiz attempt is active.'
      );
    } finally {
      setInitializing(false);
    }
  }, [attemptId, enabled, startCamera, connectWebSocket]);

  const captureReference = useCallback(() => {
    if (!connected || !cameraReady) return;
    setCapturingReference(true);
    captureReferenceRef.current = true;
    setStatusMessages(['Capturing reference image...']);
  }, [connected, cameraReady]);

  const setOnTerminated = useCallback((callback) => {
    onTerminatedRef.current = callback;
  }, []);

  useLayoutEffect(() => {
    reattachStream();
  });

  useEffect(() => {
    if (!enabled || !attemptId || initializedRef.current) return;
    initializedRef.current = true;
    initialize();

    return () => {
      initializedRef.current = false;
      stopFrameLoop();
      wsRef.current?.close();
      streamRef.current?.getTracks().forEach((t) => t.stop());
    };
  }, [enabled, attemptId, initialize, stopFrameLoop]);

  return {
    videoRef,
    canvasRef,
    config,
    connected,
    cameraReady,
    cameraError,
    referenceCaptured,
    capturingReference,
    sessionReady,
    initializing,
    maxLives,
    remainingLives,
    statusMessages,
    verificationStatus,
    faceDetected,
    riskLevel,
    sessionTerminated,
    terminationReason,
    captureReference,
    setOnTerminated,
  };
}
