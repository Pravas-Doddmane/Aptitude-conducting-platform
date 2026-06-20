import { useQuery } from '@tanstack/react-query';
import { fetchUserStats } from '../api/userService';
import { useAuth } from '../context/AuthContext';
import Card from '../components/ui/Card';
import SkeletonLoader from '../components/ui/SkeletonLoader';
import { User, Mail, BarChart3, Award, Target, Clock } from 'lucide-react';
import StatCard from '../components/ui/StatCard';
import { useState } from 'react';
import Input from '../components/ui/Input';
import Button from '../components/ui/Button';
import toast from 'react-hot-toast';

export default function Profile() {
  const { user } = useAuth();
  const { data: stats, isLoading } = useQuery({
    queryKey: ['userStats'],
    queryFn: () => fetchUserStats().then(res => res.data),
  });

  const [firstName, setFirstName] = useState(user?.firstName || '');
  const [lastName, setLastName] = useState(user?.lastName || '');

  const handleSave = () => {
    // Backend update not available, store locally
    localStorage.setItem('profile', JSON.stringify({ firstName, lastName }));
    toast.success('Profile name saved locally (backend update not implemented).');
  };

  return (
    <div className="max-w-4xl mx-auto space-y-8">
      <Card className="p-8">
        <div className="flex items-center gap-6">
          <div className="w-20 h-20 rounded-full bg-gradient-to-br from-brand-500 to-purple-600 flex items-center justify-center text-white text-2xl font-bold">
            {user?.email?.[0]?.toUpperCase() || 'U'}
          </div>
          <div>
            <h1 className="text-2xl font-bold">{firstName || user?.email}</h1>
            <p className="text-gray-500 flex items-center gap-1"><Mail className="w-4 h-4" /> {user?.email}</p>
          </div>
        </div>
        <div className="mt-6 grid grid-cols-1 md:grid-cols-2 gap-4">
          <Input label="First Name" value={firstName} onChange={(e) => setFirstName(e.target.value)} />
          <Input label="Last Name" value={lastName} onChange={(e) => setLastName(e.target.value)} />
        </div>
        <Button className="mt-4" onClick={handleSave}>Save (locally)</Button>
      </Card>

      <h2 className="text-2xl font-bold">Your Stats</h2>
      {isLoading ? (
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          {Array.from({ length: 4 }).map((_, i) => <SkeletonLoader key={i} className="h-24 rounded-xl" />)}
        </div>
      ) : stats ? (
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          <StatCard label="Total Attempts" value={stats.totalAttempts} icon={BarChart3} color="brand" />
          <StatCard label="Completed" value={stats.completedAttempts} icon={Target} color="green" />
          <StatCard label="Avg Score" value={`${stats.averageScore?.toFixed(1)}%`} icon={Award} color="yellow" />
          <StatCard label="Best Score" value={`${stats.bestScore?.toFixed(1)}%`} icon={Award} color="purple" />
          {stats.accuracy && <StatCard label="Accuracy" value={`${stats.accuracy}%`} icon={Target} color="brand" />}
          {stats.totalTimeSpentSeconds && <StatCard label="Total Time" value={formatDuration(stats.totalTimeSpentSeconds)} icon={Clock} color="green" />}
        </div>
      ) : null}
    </div>
  );
}

// formatDuration already defined, import from utils
import { formatDuration } from '../utils/formatters';