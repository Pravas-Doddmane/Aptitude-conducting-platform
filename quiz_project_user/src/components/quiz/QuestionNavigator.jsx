export default function QuestionNavigator({
  total,
  current,
  answers,
  visited,
  markedForReview,
  onNavigate,
}) {
  const getStatusClass = (index) => {
    const answered = answers[index] !== undefined;
    const marked = markedForReview[index];
    const seen = visited[index];

    if (answered && marked) return 'bg-orange-500 text-white';
    if (!answered && marked) return 'bg-yellow-500 text-white';
    if (answered) return 'bg-green-500 text-white';
    if (seen && !answered) return 'bg-red-500 text-white';
    return 'bg-gray-200 dark:bg-gray-700 text-gray-700 dark:text-gray-300';
  };

  return (
    <div className="flex flex-wrap gap-3">
      {Array.from({ length: total }, (_, i) => (
        <button
          key={i}
          onClick={() => onNavigate(i)}
          className={`w-10 h-10 rounded-lg text-sm font-medium transition ${getStatusClass(i)} ${
            current === i ? 'ring-2 ring-brand-500' : ''
          }`}
        >
          {i + 1}
        </button>
      ))}
    </div>
  );
}