const RISK_STYLES = {
  LOW: 'bg-green-50 text-green-800 border-green-200 dark:bg-green-900/20 dark:text-green-300 dark:border-green-800',
  MEDIUM: 'bg-yellow-50 text-yellow-800 border-yellow-200 dark:bg-yellow-900/20 dark:text-yellow-300 dark:border-yellow-800',
  HIGH: 'bg-orange-50 text-orange-800 border-orange-200 dark:bg-orange-900/20 dark:text-orange-300 dark:border-orange-800',
  CRITICAL: 'bg-red-50 text-red-800 border-red-200 dark:bg-red-900/20 dark:text-red-300 dark:border-red-800',
};

export default function ProctoringStatusBar({ messages = [], riskLevel = 'LOW', verificationStatus, connected }) {
  const displayMessage =
    messages.length > 0
      ? messages.join(' ')
      : connected
        ? 'Monitoring...'
        : 'Connecting to proctoring service...';

  const style = RISK_STYLES[riskLevel] || RISK_STYLES.LOW;

  return (
    <div className={`w-full rounded-lg border px-4 py-2 text-sm ${style}`}>
      <p className="font-medium">{displayMessage}</p>
      {verificationStatus && verificationStatus !== 'N/A' && (
        <p className="text-xs opacity-75 mt-0.5">Verification: {verificationStatus}</p>
      )}
    </div>
  );
}
