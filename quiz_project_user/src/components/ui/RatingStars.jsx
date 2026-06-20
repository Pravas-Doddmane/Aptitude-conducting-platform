import { Star } from 'lucide-react';

export default function RatingStars({ rating, onRate, interactive = false, size = 5 }) {
  return (
    <div className="flex gap-1">
      {Array.from({ length: size }, (_, i) => (
        <Star
          key={i}
          className={`w-5 h-5 ${i < rating ? 'fill-yellow-400 text-yellow-400' : 'text-gray-300 dark:text-gray-600'} ${interactive ? 'cursor-pointer hover:scale-110 transition' : ''}`}
          onClick={() => interactive && onRate?.(i + 1)}
        />
      ))}
    </div>
  );
}