import { FileQuestion } from 'lucide-react';

export default function EmptyState({ message = 'No data found', icon: Icon = FileQuestion }) {
  return (
    <div className="flex flex-col items-center justify-center p-12 text-gray-400 dark:text-gray-500">
      <Icon className="w-16 h-16 mb-4" />
      <p className="text-lg">{message}</p>
    </div>
  );
}