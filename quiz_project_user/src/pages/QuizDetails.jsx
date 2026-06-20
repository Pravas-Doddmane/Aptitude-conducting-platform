import { useParams, useNavigate } from 'react-router-dom';
import { useQuery, useMutation } from '@tanstack/react-query';
import { fetchQuizById } from '../api/quizService';
import { startAttempt } from '../api/attemptService';
import { useAuth } from '../context/AuthContext';
import RequirementsChecklist from '../components/quiz/RequirementsChecklist';
import Button from '../components/ui/Button';
import Card from '../components/ui/Card';
import Badge from '../components/ui/Badge';
import SkeletonLoader from '../components/ui/SkeletonLoader';
import ErrorState from '../components/ui/ErrorState';
import { formatDuration } from '../utils/formatters';
import { Clock, HelpCircle, Award, AlertTriangle, ShieldAlert } from 'lucide-react';
import toast from 'react-hot-toast';
import { useState, useCallback } from 'react';

export default function QuizDetails() {
  const { quizId } = useParams();
  const navigate = useNavigate();
  const { isAuthenticated } = useAuth();
  const [permissions, setPermissions] = useState({});

  const { data: quiz, isLoading, error } = useQuery({
    queryKey: ['quiz', quizId],
    queryFn: () => fetchQuizById(quizId).then(res => res.data),
  });

  const startMutation = useMutation({
    mutationFn: () => startAttempt(quizId),
    onSuccess: (res) => {
      const startData = res.data;
      const attemptId = startData.attemptId;
      sessionStorage.setItem(`attempt:${attemptId}`, JSON.stringify(startData));
      
      // Check if this is a resumed attempt by comparing startedAt time
      const startedAt = new Date(startData.startedAt).getTime();
      const now = Date.now();
      const elapsedSeconds = Math.floor((now - startedAt) / 1000);
      
      if (elapsedSeconds > 5) {
        // This is a resumed attempt (more than 5 seconds have passed)
        toast.success('Resuming your quiz attempt!');
      } else {
        // This is a new attempt
        toast.success('Attempt started!');
      }
      
      navigate(`/attempts/${attemptId}/play`, { state: { startData } });
    },
    onError: (err) => {
      toast.error(err.response?.data?.message || 'Quiz is not available right now');
    },
  });

  const requestPermission = useCallback(async (type) => {
    try {
      switch (type) {
        case 'requireFullScreen':
          await document.documentElement.requestFullscreen();
          setPermissions(prev => ({ ...prev, requireFullScreen: true }));
          break;
        case 'requireCamera':
          await navigator.mediaDevices.getUserMedia({ video: true });
          setPermissions(prev => ({ ...prev, requireCamera: true }));
          break;
        case 'requireMicrophone':
          await navigator.mediaDevices.getUserMedia({ audio: true });
          setPermissions(prev => ({ ...prev, requireMicrophone: true }));
          break;
        case 'requireLocation':
          await new Promise((resolve, reject) =>
            navigator.geolocation.getCurrentPosition(resolve, reject)
          );
          setPermissions(prev => ({ ...prev, requireLocation: true }));
          break;
        default:
          break;
      }
    } catch (err) {
      toast.error(`Permission denied: ${type}`);
    }
  }, []);

  const allRequirementsMet = () => {
    const reqs = ['requireFullScreen', 'requireCamera', 'requireMicrophone', 'requireLocation'];
    return reqs.every(r => !quiz[r] || permissions[r]);
  };

  if (isLoading) return <SkeletonLoader className="h-96 rounded-2xl" />;
  if (error) return <ErrorState message="Failed to load quiz" onRetry={() => window.location.reload()} />;
  if (!quiz) return <ErrorState message="Quiz not found" />;
  const categoryLabel = quiz.categoryNames?.length ? quiz.categoryNames.join(', ') : quiz.categoryName;
  const isCurrentlyAvailable = quiz.currentlyAvailable !== false;

  return (
    <div className="max-w-4xl mx-auto">
      <Card className="p-8">
        <div className="flex flex-wrap justify-between items-start gap-4">
          <div>
            <h1 className="text-3xl font-bold">{quiz.title}</h1>
            <p className="text-gray-500 mt-1">{categoryLabel}</p>
          </div>
          <Badge color="purple" className="text-sm">{quiz.status}</Badge>
        </div>

        <p className="mt-6 text-gray-600 dark:text-gray-300">{quiz.description}</p>

        <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mt-8">
          <div className="flex items-center gap-2"><Clock className="w-5 h-5 text-brand-600" /> {formatDuration(quiz.durationSeconds)}</div>
          <div className="flex items-center gap-2"><HelpCircle className="w-5 h-5 text-brand-600" /> {quiz.questionCount} Questions</div>
          <div className="flex items-center gap-2"><ShieldAlert className="w-5 h-5 text-brand-600" /> {quiz.competitionMode ? 'Competition' : 'Standard'}</div>
          <div className="flex items-center gap-2"><Award className="w-5 h-5 text-brand-600" /> {quiz.certificateEnabled ? 'Certificate' : 'No cert'}</div>
        </div>

        {!isCurrentlyAvailable && (
          <div className="mt-6 rounded-xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-800 dark:border-amber-900/40 dark:bg-amber-900/20 dark:text-amber-200">
            This quiz is not available right now based on the admin schedule.
          </div>
        )}

        {quiz.competitionMode && (
          <div className="mt-8">
            <RequirementsChecklist
              quiz={quiz}
              permissions={permissions}
              requestPermissions={requestPermission}
            />
          </div>
        )}

        <div className="mt-8 flex justify-end">
          <Button
            onClick={() => startMutation.mutate()}
            isLoading={startMutation.isPending}
            disabled={!isAuthenticated || !isCurrentlyAvailable || (quiz.competitionMode && !allRequirementsMet())}
          >
            {isAuthenticated ? 'Start Quiz' : 'Login to Start'}
          </Button>
        </div>
      </Card>
    </div>
  );
}
