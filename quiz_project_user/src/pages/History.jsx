import { useQuery } from '@tanstack/react-query';
import { fetchMyAttempts } from '../api/attemptService';
import { useLocation, useNavigate } from 'react-router-dom';
import Card from '../components/ui/Card';
import Badge from '../components/ui/Badge';
import Button from '../components/ui/Button';
import SkeletonLoader from '../components/ui/SkeletonLoader';
import EmptyState from '../components/ui/EmptyState';
import { formatDate } from '../utils/formatters';
import { getAttemptStatusColor } from '../utils/attemptStatus';
import { AlertTriangle, Eye, RotateCcw } from 'lucide-react';
import { useMemo, useState } from 'react';

export default function History() {
  const navigate = useNavigate();
  const location = useLocation();
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState('all');

  const notice = location.state?.noticeMessage || null;
  const noticeType = location.state?.noticeType || 'submitted';

  const { data: attempts, isLoading } = useQuery({
    queryKey: ['myAttempts'],
    queryFn: () => fetchMyAttempts().then(res => res.data),
  });

  const filtered = useMemo(() => {
    if (!attempts) return [];
    return attempts.filter((a) => {
      const matchSearch = a.quizTitle?.toLowerCase().includes(search.toLowerCase());
      const matchStatus = statusFilter === 'all' || a.status === statusFilter;
      return matchSearch && matchStatus;
    });
  }, [attempts, search, statusFilter]);

  const stats = useMemo(() => {
    if (!attempts) return null;
    const completed = attempts.filter((a) => a.resultAvailable && (a.status === 'SUBMITTED' || a.status === 'AUTO_SUBMITTED' || a.status === 'COMPLETED'));
    const scores = completed
      .filter((a) => a.score != null && a.maxScore)
      .map((a) => (a.score / a.maxScore) * 100);

    return {
      total: attempts.length,
      completed: completed.length,
      avgScore: scores.length ? (scores.reduce((a, b) => a + b, 0) / scores.length).toFixed(1) : 0,
      bestScore: scores.length ? Math.max(...scores).toFixed(1) : 0,
    };
  }, [attempts]);

  if (isLoading) {
    return (
      <div className="space-y-4">
        <SkeletonLoader className="h-32 rounded-2xl" />
        <SkeletonLoader className="h-32 rounded-2xl" />
      </div>
    );
  }

  const noticeClassName =
    noticeType === 'proctoring'
      ? 'border-red-200 bg-red-50 text-red-800 dark:border-red-900/40 dark:bg-red-900/20 dark:text-red-200'
      : 'border-emerald-200 bg-emerald-50 text-emerald-800 dark:border-emerald-900/40 dark:bg-emerald-900/20 dark:text-emerald-200';

  return (
    <div>
      <h1 className="text-3xl font-bold mb-6">Attempt History</h1>

      {/* Replace proctoring notice with a reload button */}
      {notice && noticeType === 'proctoring' ? (
        <div className="mb-6 flex items-center gap-4">
          <Button onClick={() => window.location.reload()}>
            Reload Page
          </Button>
          <span className="text-sm text-gray-500">
            Attempt was auto‑submitted. Reload to refresh the list.
          </span>
        </div>
      ) : notice && (
        <div className={`mb-6 flex items-start gap-3 rounded-2xl border px-4 py-3 ${noticeClassName}`}>
          <AlertTriangle className="mt-0.5 h-5 w-5 flex-shrink-0" />
          <p className="text-sm font-medium">{notice}</p>
        </div>
      )}

      {stats && (
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-8">
          <div className="p-4 bg-white dark:bg-gray-800 rounded-xl shadow"><p className="text-sm">Total</p><p className="text-2xl font-bold">{stats.total}</p></div>
          <div className="p-4 bg-white dark:bg-gray-800 rounded-xl shadow"><p className="text-sm">Completed</p><p className="text-2xl font-bold">{stats.completed}</p></div>
          <div className="p-4 bg-white dark:bg-gray-800 rounded-xl shadow"><p className="text-sm">Avg Score</p><p className="text-2xl font-bold">{stats.avgScore}%</p></div>
          <div className="p-4 bg-white dark:bg-gray-800 rounded-xl shadow"><p className="text-sm">Best</p><p className="text-2xl font-bold">{stats.bestScore}%</p></div>
        </div>
      )}

      <div className="flex gap-4 mb-6">
        <input
          type="text"
          placeholder="Search by quiz title"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          className="flex-1 rounded-lg border px-4 py-2 dark:bg-gray-800 dark:border-gray-700"
        />
        <select
          value={statusFilter}
          onChange={(e) => setStatusFilter(e.target.value)}
          className="rounded-lg border px-4 py-2 dark:bg-gray-800 dark:border-gray-700"
        >
          <option value="all">All</option>
          <option value="SUBMITTED">Submitted</option>
          <option value="AUTO_SUBMITTED">Auto Submitted</option>
          <option value="IN_PROGRESS">In Progress</option>
          <option value="ABANDONED">Abandoned</option>
        </select>
      </div>

      {filtered.length === 0 ? (
        <EmptyState message="No attempts found" />
      ) : (
        <div className="space-y-4">
          {filtered.map((attempt) => {
            const hasScore = attempt.resultAvailable && attempt.score != null && attempt.maxScore;
            const isTimeExpiredAutoSubmit = attempt.autoSubmitted && attempt.autoSubmitReason === 'TIME_EXPIRED';
            const isRestrictedAutoSubmit = attempt.autoSubmitted && !attempt.resultAvailable;

            return (
              <Card key={attempt.attemptId} className="p-6">
                <div className="flex flex-col md:flex-row justify-between gap-4">
                  <div>
                    <h3 className="text-xl font-semibold">{attempt.quizTitle}</h3>
                    <p className="text-sm text-gray-500">Started: {formatDate(attempt.startedAt)}</p>
                    {attempt.submittedAt && <p className="text-sm text-gray-500">Submitted: {formatDate(attempt.submittedAt)}</p>}
                    <div className="mt-2 flex flex-wrap items-center gap-3">
                      {hasScore ? (
                        <span className="font-medium">
                          {attempt.score}/{attempt.maxScore} ({((attempt.score / attempt.maxScore) * 100).toFixed(1)}%)
                        </span>
                      ) : isRestrictedAutoSubmit ? (
                        <span className="text-sm font-medium text-red-500">Auto-submitted due to proctoring activity. Result hidden.</span>
                      ) : (
                        <span className="text-sm font-medium text-amber-500">Result not available yet.</span>
                      )}
                      <Badge color={getAttemptStatusColor(attempt.status)}>{attempt.status}</Badge>
                      {isTimeExpiredAutoSubmit && <Badge color="bg-blue-100 text-blue-800 dark:bg-blue-900/30 dark:text-blue-300">Time Up</Badge>}
                    </div>
                  </div>

                  <div className="flex items-center gap-2 mt-2 md:mt-0">
                    {attempt.resultAvailable && (
                      <Button variant="ghost" onClick={() => navigate(`/attempts/${attempt.attemptId}/result`)}>
                        <Eye className="w-4 h-4" /> View
                      </Button>
                    )}
                    {(attempt.status === 'SUBMITTED' || attempt.status === 'AUTO_SUBMITTED' || attempt.status === 'COMPLETED') && (
                      <Button onClick={() => navigate(`/quizzes/${attempt.quizId}`)}>
                        <RotateCcw className="w-4 h-4" /> Reattempt
                      </Button>
                    )}
                  </div>
                </div>
              </Card>
            );
          })}
        </div>
      )}
    </div>
  );
}