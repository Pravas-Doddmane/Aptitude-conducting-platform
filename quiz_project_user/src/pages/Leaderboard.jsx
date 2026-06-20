import { useQuery } from '@tanstack/react-query';
import { fetchMyAttempts } from '../api/attemptService';
import Card from '../components/ui/Card';
import Badge from '../components/ui/Badge';
import EmptyState from '../components/ui/EmptyState';
import SkeletonLoader from '../components/ui/SkeletonLoader';
import { Trophy, Medal } from 'lucide-react';
import { useMemo } from 'react';

export default function Leaderboard() {
  const { data: attempts, isLoading } = useQuery({
    queryKey: ['myAttempts'],
    queryFn: () => fetchMyAttempts().then(res => res.data),
  });

  const personalBests = useMemo(() => {
    if (!attempts) return [];
    const bestMap = {};
    attempts.forEach(a => {
      if (a.status === 'COMPLETED' && a.score != null && a.maxScore) {
        const pct = (a.score / a.maxScore) * 100;
        if (!bestMap[a.quizId] || pct > bestMap[a.quizId].percentage) {
          bestMap[a.quizId] = { quizTitle: a.quizTitle, percentage: pct, score: a.score, maxScore: a.maxScore };
        }
      }
    });
    return Object.values(bestMap).sort((a, b) => b.percentage - a.percentage);
  }, [attempts]);

  return (
    <div>
      <h1 className="text-3xl font-bold mb-8 flex items-center gap-3"><Trophy className="w-8 h-8 text-yellow-500" /> Leaderboard</h1>
      
      <div className="mb-8">
        <h2 className="text-xl font-semibold mb-4">Global Leaderboard</h2>
        <Card className="p-12 text-center text-gray-500">
          <Trophy className="w-16 h-16 mx-auto mb-4 text-gray-300" />
          <p>Global leaderboard is coming soon! Meanwhile, check your personal bests below.</p>
        </Card>
      </div>

      <div>
        <h2 className="text-xl font-semibold mb-4">Your Personal Bests</h2>
        {isLoading ? <SkeletonLoader className="h-48 rounded-2xl" /> : personalBests.length === 0 ? (
          <EmptyState message="No completed attempts yet" />
        ) : (
          <div className="space-y-4">
            {personalBests.map((item, idx) => (
              <Card key={idx} className="p-5 flex items-center justify-between">
                <div className="flex items-center gap-4">
                  <div className={`w-10 h-10 rounded-full flex items-center justify-center text-white font-bold ${idx === 0 ? 'bg-yellow-500' : idx === 1 ? 'bg-gray-400' : idx === 2 ? 'bg-amber-700' : 'bg-gray-300 dark:bg-gray-600'}`}>
                    {idx + 1}
                  </div>
                  <div>
                    <p className="font-semibold">{item.quizTitle}</p>
                    <p className="text-sm text-gray-500">{item.score}/{item.maxScore}</p>
                  </div>
                </div>
                <Badge color="green">{item.percentage.toFixed(1)}%</Badge>
              </Card>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}