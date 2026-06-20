import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { fetchAttemptById } from '../api/attemptService';
import { fetchQuizById } from '../api/quizService';
import { downloadCertificate } from '../api/certificateService';
import Card from '../components/ui/Card';
import Badge from '../components/ui/Badge';
import Button from '../components/ui/Button';
import SkeletonLoader from '../components/ui/SkeletonLoader';
import EmptyState from '../components/ui/EmptyState';
import { formatDate, formatDuration } from '../utils/formatters';
import { getAttemptStatusColor } from '../utils/attemptStatus';
import { Award, Download, RotateCcw, Eye, EyeOff } from 'lucide-react';
import Modal from '../components/ui/Modal';
import Input from '../components/ui/Input';
import QuizFeedback from '../components/quiz/QuizFeedback';
import toast from 'react-hot-toast';

export default function AttemptResult() {
  const { attemptId } = useParams();
  const navigate = useNavigate();
  const [certName, setCertName] = useState('');
  const [showCertModal, setShowCertModal] = useState(false);
  const [showAnswers, setShowAnswers] = useState(false);

  const { data: attempt, isLoading } = useQuery({
    queryKey: ['attempt', attemptId],
    queryFn: () => fetchAttemptById(attemptId).then((res) => res.data),
  });

  const { data: quiz } = useQuery({
    queryKey: ['quiz', attempt?.quizId],
    queryFn: () => fetchQuizById(attempt?.quizId).then((res) => res.data),
    enabled: !!attempt?.quizId,
  });

  useEffect(() => {
    if (!attempt || isLoading) return;
    if (attempt.resultAvailable) return;

    navigate('/history', {
      replace: true,
      state: {
        noticeType: 'proctoring',
        noticeMessage: 'This attempt was auto-submitted because of prohibited activity. Result is not available.',
        attemptId,
      },
    });
  }, [attempt, attemptId, isLoading, navigate]);

  if (isLoading) return <SkeletonLoader className="h-96 rounded-2xl" />;
  if (!attempt) return <EmptyState message="Attempt not found" />;
  if (!attempt.resultAvailable) return null;

  const scorePercent = attempt.maxScore > 0 ? ((attempt.score / attempt.maxScore) * 100).toFixed(1) : 0;
  const passingScore = attempt.passingScore || 0;
  const hasPassed = parseFloat(scorePercent) >= passingScore;
  const isTimeExpiredAutoSubmit = attempt.autoSubmitted && attempt.autoSubmitReason === 'TIME_EXPIRED';

  const certificateAvailable =
    quiz?.certificateEnabled &&
    quiz?.certificateTemplateId &&
    attempt.submittedAt &&
    hasPassed;

  const handleDownload = async (format) => {
    if (!certName.trim()) {
      toast.error('Please enter your name for the certificate');
      return;
    }

    try {
      const response = await downloadCertificate(attemptId, format, certName);
      const url = window.URL.createObjectURL(new Blob([response.data]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `certificate_${attemptId}.${format}`);
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
      toast.success('Certificate downloaded');
      setShowCertModal(false);
    } catch (err) {
      console.error('Download error:', err);
      toast.error('Download failed. Please try again.');
    }
  };

  return (
    <div className="max-w-4xl mx-auto space-y-8">
      <Card className="p-8">
        <div className="flex items-center justify-between">
          <h1 className="text-3xl font-bold">{quiz?.title || 'Quiz'} Result</h1>
          <Badge color={getAttemptStatusColor(attempt.autoSubmitted ? 'AUTO_SUBMITTED' : 'SUBMITTED')} className="text-sm">
            {attempt.autoSubmitted ? 'AUTO_SUBMITTED' : 'SUBMITTED'}
          </Badge>
        </div>

        {isTimeExpiredAutoSubmit && (
          <div className="mt-4 rounded-xl border border-blue-200 bg-blue-50 px-4 py-3 text-sm font-medium text-blue-800 dark:border-blue-900/40 dark:bg-blue-900/20 dark:text-blue-200">
            Time is up. Your result was auto-submitted successfully.
          </div>
        )}

        <div className="mt-6 grid grid-cols-2 md:grid-cols-4 gap-4">
          <div><span className="text-sm text-gray-500">Score</span><p className="text-2xl font-bold">{attempt.score}/{attempt.maxScore}</p></div>
          <div><span className="text-sm text-gray-500">Percentage</span><p className="text-2xl font-bold">{scorePercent}%</p></div>
          <div><span className="text-sm text-gray-500">Correct</span><p className="text-2xl font-bold text-green-600">{attempt.correctCount}</p></div>
          <div><span className="text-sm text-gray-500">Wrong</span><p className="text-2xl font-bold text-red-600">{attempt.wrongCount}</p></div>
        </div>
        <div className="mt-4 text-sm text-gray-500 space-y-1">
          <p>Started: {formatDate(attempt.startedAt)}</p>
          <p>Submitted: {formatDate(attempt.submittedAt)}</p>
          <p>Duration: {formatDuration(attempt.elapsedSeconds)}</p>
        </div>
        {attempt.autoSubmitted && <Badge color="red" className="mt-2">Auto Submitted</Badge>}
      </Card>

      <div className="flex justify-end">
        <Button variant="secondary" onClick={() => setShowAnswers(!showAnswers)}>
          {showAnswers ? <EyeOff className="w-4 h-4 mr-2" /> : <Eye className="w-4 h-4 mr-2" />}
          {showAnswers ? 'Hide Answers' : 'View Answers'}
        </Button>
      </div>

      {showAnswers && (
        <Card className="p-6">
          <h2 className="text-xl font-semibold mb-4">Answer Review</h2>
          {attempt.detailedAnswers && attempt.detailedAnswers.length > 0 ? (
            <div className="space-y-6">
              {attempt.detailedAnswers.map((detail, idx) => (
                <div key={idx} className="border rounded-lg p-4 dark:border-gray-700">
                  <div className="flex items-start justify-between mb-3">
                    <div className="flex items-center gap-2">
                      <span className="font-semibold text-lg">Q{idx + 1}.</span>
                      {detail.isCorrect ? (
                        <Badge color="green" className="text-xs">Correct</Badge>
                      ) : detail.selectedOptionId ? (
                        <Badge color="red" className="text-xs">Wrong</Badge>
                      ) : (
                        <Badge color="gray" className="text-xs">Unanswered</Badge>
                      )}
                    </div>
                    {detail.responseTimeMs && (
                      <span className="text-xs text-gray-500">
                        {(detail.responseTimeMs / 1000).toFixed(1)}s
                      </span>
                    )}
                  </div>

                  {detail.questionImageUrl && (
                    <img
                      src={detail.questionImageUrl}
                      alt="Question"
                      className="mb-3 max-h-48 rounded-lg object-contain border dark:border-gray-700"
                    />
                  )}

                  <p className="font-medium text-gray-900 dark:text-gray-100 mb-4">
                    {detail.questionText}
                  </p>

                  <div className="space-y-2 mb-3">
                    {detail.allOptions.map((option) => {
                      const isSelected = option.optionId === detail.selectedOptionId;
                      const isCorrectOption = option.isCorrect;

                      let optionClass = 'p-3 rounded-lg border-2 ';
                      if (isCorrectOption) {
                        optionClass += 'bg-green-50 border-green-500 dark:bg-green-900/20 dark:border-green-600';
                      } else if (isSelected && !isCorrectOption) {
                        optionClass += 'bg-red-50 border-red-500 dark:bg-red-900/20 dark:border-red-600';
                      } else {
                        optionClass += 'bg-gray-50 border-gray-200 dark:bg-gray-800 dark:border-gray-700';
                      }

                      return (
                        <div key={option.optionId} className={optionClass}>
                          <div className="flex items-center justify-between">
                            <span className="text-sm">{option.optionText}</span>
                            <div className="flex items-center gap-2">
                              {isSelected && (
                                <span className="text-xs text-gray-600 dark:text-gray-400">Your Answer</span>
                              )}
                              {isCorrectOption && (
                                <span className="text-xs font-semibold text-green-700 dark:text-green-400">Correct</span>
                              )}
                            </div>
                          </div>
                        </div>
                      );
                    })}
                  </div>

                  {detail.explanation && (
                    <div className="mt-3 p-3 bg-blue-50 dark:bg-blue-900/20 rounded-lg border border-blue-200 dark:border-blue-800">
                      <p className="text-sm font-semibold text-blue-900 dark:text-blue-200 mb-1">Explanation:</p>
                      <p className="text-sm text-blue-800 dark:text-blue-300">{detail.explanation}</p>
                    </div>
                  )}
                </div>
              ))}
            </div>
          ) : (
            <p className="text-gray-500">Detailed answer review will be available when backend provides full question details.</p>
          )}
        </Card>
      )}

      <Card className="p-6">
        <QuizFeedback quizId={attempt.quizId} />
      </Card>

      {certificateAvailable && (
        <div className="flex justify-end gap-3">
          <Button onClick={() => setShowCertModal(true)}><Award className="w-5 h-5" /> Certificate</Button>
        </div>
      )}

      <Modal isOpen={showCertModal} onClose={() => setShowCertModal(false)} title="Download Certificate">
        <p className="mb-4">Enter your name as you want it to appear on the certificate.</p>
        <Input
          value={certName}
          onChange={(e) => setCertName(e.target.value)}
          placeholder="Your full name"
          className="mb-4"
        />
        <div className="flex gap-3">
          <Button onClick={() => handleDownload('png')} disabled={!certName.trim()}>
            <Download className="w-4 h-4" /> Download
          </Button>
        </div>
        {!certName.trim() && (
          <p className="text-xs text-red-500 mt-2">Please enter your name to download the certificate.</p>
        )}
      </Modal>

      <div className="flex justify-between">
        <Button variant="secondary" onClick={() => navigate('/history')}>Back to History</Button>
        {(!quiz?.maxAttemptsPerUser || quiz.maxAttemptsPerUser === 0) && (
          <Button onClick={() => navigate(`/quizzes/${attempt.quizId}`)}><RotateCcw className="w-4 h-4" /> Reattempt</Button>
        )}
      </div>
    </div>
  );
}
