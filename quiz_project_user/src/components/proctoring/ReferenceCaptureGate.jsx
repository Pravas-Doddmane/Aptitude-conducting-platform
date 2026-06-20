import Button from '../ui/Button';
import ProctoringPanel from './ProctoringPanel';

export default function ReferenceCaptureGate({
  videoRef,
  canvasRef,
  maxLives,
  remainingLives,
  statusMessages,
  riskLevel,
  verificationStatus,
  connected,
  cameraError,
  cameraReady,
  capturingReference,
  onCaptureReference,
}) {
  return (
    <div className="min-h-screen flex flex-col items-center justify-center p-6 bg-gray-50 dark:bg-gray-900">
      <div className="max-w-lg w-full bg-white dark:bg-gray-800 rounded-2xl shadow-xl p-8">
        <h2 className="text-2xl font-bold text-center mb-2">Identity Verification</h2>
        <p className="text-center text-gray-500 dark:text-gray-400 mb-6 text-sm">
          Position your face in the camera frame and capture your reference photo.
          The quiz cannot begin until your identity is verified.
        </p>

        <ProctoringPanel
          videoRef={videoRef}
          canvasRef={canvasRef}
          maxLives={maxLives}
          remainingLives={remainingLives}
          statusMessages={statusMessages}
          riskLevel={riskLevel}
          verificationStatus={verificationStatus}
          connected={connected}
          cameraError={cameraError}
        />

        <div className="flex flex-col items-center gap-3 mt-4">
          <Button
            onClick={onCaptureReference}
            disabled={!cameraReady || !connected || capturingReference}
            isLoading={capturingReference}
            className="w-full max-w-xs"
          >
            Capture Reference Photo
          </Button>
          <p className="text-xs text-gray-400 text-center">
            Ensure only one face is visible and lighting is adequate.
          </p>
        </div>
      </div>
    </div>
  );
}
