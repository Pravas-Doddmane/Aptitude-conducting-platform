export default function LivesDisplay({ maxLives, remainingLives }) {
  if (maxLives == null) {
    return (
      <div className="flex items-center gap-1 text-sm text-gray-500" aria-label="Loading lives">
        <span className="animate-pulse">...</span>
      </div>
    );
  }

  const remaining = remainingLives ?? maxLives;

  return (
    <div className="flex items-center gap-1 select-none" aria-label={`${remaining} of ${maxLives} lives remaining`}>
      <span className="text-xs font-semibold uppercase tracking-[0.24em] text-gray-500 dark:text-gray-400">
        Lives
      </span>
      <div className="flex items-center gap-0.5 text-lg">
        {Array.from({ length: maxLives }, (_, i) => (
          <span key={i} role="img" aria-hidden="true">
            {i < remaining ? '\u2764' : '\u2661'}
          </span>
        ))}
      </div>
    </div>
  );
}
