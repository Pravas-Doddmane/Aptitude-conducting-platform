import { AlertTriangle } from 'lucide-react';

export default function ErrorState({ message = 'Something went wrong', onRetry }) {
  return (
    <div className="flex flex-col items-center justify-center p-12 text-red-500">
      <AlertTriangle className="w-16 h-16 mb-4" />
      <p className="text-lg mb-4">{message}</p>
      {onRetry && (
        <button onClick={onRetry} className="px-4 py-2 bg-red-100 dark:bg-red-900/30 text-red-600 rounded-lg hover:bg-red-200">
          Retry
        </button>
      )}
    </div>
  );
}