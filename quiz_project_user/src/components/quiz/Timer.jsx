import { useEffect, useState } from 'react';
import { Clock } from 'lucide-react';

export default function Timer({ initialSeconds, startedAt, onTimeUp }) {
  const calculateRemainingSeconds = () => {
    if (!startedAt || !initialSeconds) return initialSeconds;
    
    const startTime = new Date(startedAt).getTime();
    const now = Date.now();
    const elapsedSeconds = Math.floor((now - startTime) / 1000);
    const remaining = Math.max(0, initialSeconds - elapsedSeconds);
    
    return remaining;
  };

  const [seconds, setSeconds] = useState(calculateRemainingSeconds());
  const [isExpired, setIsExpired] = useState(false);

  useEffect(() => {
    setSeconds(calculateRemainingSeconds());
    setIsExpired(false);
  }, [initialSeconds, startedAt]);

  useEffect(() => {
    if (!initialSeconds || seconds <= 0) {
      if (!isExpired) {
        setIsExpired(true);
        if (initialSeconds) {
          onTimeUp?.();
        }
      }
      return;
    }
    const interval = setInterval(() => {
      setSeconds((prev) => {
        if (prev <= 1) {
          clearInterval(interval);
          return 0;
        }
        return prev - 1;
      });
    }, 1000);
    return () => clearInterval(interval);
  }, [initialSeconds, seconds, isExpired, onTimeUp]);

  const format = (s) => {
    const h = Math.floor(s / 3600);
    const m = Math.floor((s % 3600) / 60);
    const sec = s % 60;
    return `${h.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')}:${sec.toString().padStart(2, '0')}`;
  };

  return (
    <div className={`flex items-center gap-2 font-mono text-lg font-bold ${seconds < 60 ? 'text-red-500 animate-pulse' : ''}`}>
      <Clock className="w-5 h-5" />
      {format(seconds)}
    </div>
  );
}
