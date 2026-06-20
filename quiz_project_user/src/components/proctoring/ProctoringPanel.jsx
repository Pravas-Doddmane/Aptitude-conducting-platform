import LivesDisplay from './LivesDisplay';
import ProctoringStatusBar from './ProctoringStatusBar';

export default function ProctoringPanel({
  videoRef,
  canvasRef,
  maxLives,
  remainingLives,
  statusMessages,
  riskLevel,
  verificationStatus,
  connected,
  cameraError,
}) {
  return (
    <div className="w-full mb-4">
      <div className="relative flex justify-center items-start min-h-[132px] sm:min-h-[148px]">
        <div className="relative w-32 h-32 sm:w-36 sm:h-36 rounded-xl overflow-hidden border-2 border-gray-200 dark:border-gray-600 bg-black shadow-lg flex-shrink-0">
          {cameraError ? (
            <div className="flex items-center justify-center h-full p-3 text-center text-xs text-red-400">
              {cameraError}
            </div>
          ) : (
            <video
              ref={videoRef}
              autoPlay
              muted
              playsInline
              className="w-full h-full object-cover mirror"
              style={{ transform: 'scaleX(-1)' }}
            />
          )}
          {!connected && !cameraError && (
            <div className="absolute inset-0 flex items-center justify-center bg-black/50 text-white text-xs">
              Connecting...
            </div>
          )}
        </div>

        <div className="absolute top-0 left-0 bg-white/90 dark:bg-gray-800/90 rounded-xl px-3 py-2 shadow border border-gray-200 dark:border-gray-600">
          <LivesDisplay maxLives={maxLives} remainingLives={remainingLives} />
        </div>
      </div>

      <canvas ref={canvasRef} width={640} height={480} className="hidden" />

      <div className="mt-3 max-w-2xl mx-auto">
        <ProctoringStatusBar
          messages={statusMessages}
          riskLevel={riskLevel}
          verificationStatus={verificationStatus}
          connected={connected}
        />
      </div>
    </div>
  );
}
