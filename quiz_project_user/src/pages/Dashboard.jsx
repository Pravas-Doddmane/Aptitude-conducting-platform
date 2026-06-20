import { useQueries, useQuery } from '@tanstack/react-query';
import { fetchCategories, fetchQuizzesByCategory } from '../api/quizService';
import { fetchMyAttempts } from '../api/attemptService';
import { fetchUserStats } from '../api/userService';
import QuizCard from '../components/quiz/QuizCard';
import StatCard from '../components/ui/StatCard';
import SkeletonLoader from '../components/ui/SkeletonLoader';
import EmptyState from '../components/ui/EmptyState';
import ErrorState from '../components/ui/ErrorState';
import { BarChart3, Award, Clock, Target } from 'lucide-react';
import { useState } from 'react';

export default function Dashboard() {
  const [search, setSearch] = useState('');
  const [selectedCategory, setSelectedCategory] = useState('all');

  const { data: categoriesData, isLoading: catLoading } = useQuery({
    queryKey: ['categories'],
    queryFn: () => fetchCategories().then(res => res.data),
  });

  const { data: attemptsData } = useQuery({
    queryKey: ['myAttempts'],
    queryFn: () => fetchMyAttempts().then(res => res.data),
  });

  const { data: stats, isLoading: statsLoading } = useQuery({
    queryKey: ['userStats'],
    queryFn: () => fetchUserStats().then(res => res.data),
  });

  const categories = categoriesData || [];

  const quizQueries = useQueries({
    queries: categories.map((cat) => ({
      queryKey: ['quizzes', cat.id],
      queryFn: () => fetchQuizzesByCategory(cat.id).then(res => res.data),
      enabled: !!cat.id,
    })),
  });

  const allQuizzes = quizQueries
    .flatMap((q) => q.data || [])
    .filter(q => q.status === 'PUBLISHED')
    .reduce((unique, quiz) => {
      // Only add if not already in the array (deduplicate by quiz ID)
      if (!unique.some(q => q.id === quiz.id)) {
        unique.push(quiz);
      }
      return unique;
    }, []);
  const isLoadingQuizzes = quizQueries.some(q => q.isLoading);

  const attemptCounts = {};
  attemptsData
    ?.filter(a => a.status === 'SUBMITTED' || a.status === 'AUTO_SUBMITTED')
    .forEach(a => {
      attemptCounts[a.quizId] = (attemptCounts[a.quizId] || 0) + 1;
    });

  const filtered = allQuizzes.filter(q => {
    const matchSearch = q.title.toLowerCase().includes(search.toLowerCase());
    const matchCat = selectedCategory === 'all' || q.categoryId.toString() === selectedCategory;
    return matchSearch && matchCat;
  });

  return (
    <div>
      <h1 className="text-3xl font-bold mb-2">Dashboard</h1>
      

      
      {/* Stats */}
   {/*   
    <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-8">
        {statsLoading ? (
          Array.from({ length: 4 }).map((_, i) => <SkeletonLoader key={i} className="h-24 rounded-xl" />)
        ) : stats ? (
          <>
            <StatCard label="Total Attempts" value={stats.totalAttempts} icon={BarChart3} color="brand" />
            <StatCard label="Completed" value={stats.completedAttempts} icon={Target} color="green" />
            <StatCard label="Avg Score" value={`${stats.averageScore?.toFixed(1)}%`} icon={Award} color="yellow" />
            <StatCard label="Best Score" value={`${stats.bestScore?.toFixed(1)}%`} icon={Award} color="purple" />
          </>
        ) : null}
      </div>
   */}
      

      {/* Filters */}
      <div className="flex flex-col sm:flex-row gap-4 mb-6">
        <input
          type="text"
          placeholder="Search quizzes..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          className="flex-1 rounded-lg border px-4 py-2 dark:bg-gray-800 dark:border-gray-700"
        />
        <select
          value={selectedCategory}
          onChange={(e) => setSelectedCategory(e.target.value)}
          className="rounded-lg border px-4 py-2 dark:bg-gray-800 dark:border-gray-700"
        >
          <option value="all">All Categories</option>
          {categories.map(cat => (
            <option key={cat.id} value={cat.id.toString()}>{cat.name}</option>
          ))}
        </select>
      </div>

      {/* Quiz list */}
      {isLoadingQuizzes ? (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {Array.from({ length: 6 }).map((_, i) => <SkeletonLoader key={i} className="h-64 rounded-2xl" />)}
        </div>
      ) : filtered.length === 0 ? (
        <EmptyState message="No quizzes available" />
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {filtered.map(quiz => (
            <QuizCard
              key={quiz.id}
              quiz={quiz}
              attemptCount={attemptCounts[quiz.id] || 0}
              maxAttempts={quiz.maxAttemptsPerUser}
            />
          ))}
        </div>
      )}
    </div>
  );
}