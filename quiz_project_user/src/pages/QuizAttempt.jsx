import { useParams, useNavigate, useLocation } from 'react-router-dom';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { fetchAttemptById, submitAttempt, autoSubmitAttempt } from '../api/attemptService';
import { fetchQuizById } from '../api/quizService';
import { endProctoringSession } from '../api/proctoringService';
import Timer from '../components/quiz/Timer';
import QuestionNavigator from '../components/quiz/QuestionNavigator';
import ProctoringPanel from '../components/proctoring/ProctoringPanel';
import ReferenceCaptureGate from '../components/proctoring/ReferenceCaptureGate';
import useProctoring from '../hooks/useProctoring';
import Button from '../components/ui/Button';
import Modal from '../components/ui/Modal';
import ConfirmDialog from '../components/ui/ConfirmDialog';
import { useState, useEffect, useCallback, useRef } from 'react';
import { ChevronLeft, ChevronRight, Flag, RotateCcw, Send } from 'lucide-react';
import toast from 'react-hot-toast';
import { useBeforeUnload } from 'react-router-dom'; // simple prompt

export default function QuizAttempt() {
  const { attemptId } = useParams();
  const navigate = useNavigate();
  const location = useLocation();
  const queryClient = useQueryClient();
  const storedStartData = (() => {
    try {
      return JSON.parse(sessionStorage.getItem(`attempt:${attemptId}`) || 'null');
    } catch {
      return null;
    }
  })();
  const startData = location.state?.startData || storedStartData;
  
  // Load stored answers from sessionStorage
  const storedAnswers = (() => {
    try {
      return JSON.parse(sessionStorage.getItem(`answers:${attemptId}`) || '{}');
    } catch {
      return {};
    }
  })();
  
  const [currentIndex, setCurrentIndex] = useState(0);
  const [answers, setAnswers] = useState(storedAnswers);
  const [visited, setVisited] = useState({});
  const [markedForReview, setMarkedForReview] = useState({});
  const [responseTimes, setResponseTimes] = useState({});
  const [startTime, setStartTime] = useState(Date.now());
  const [showSubmitModal, setShowSubmitModal] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [tabWarning, setTabWarning] = useState(false);
  const [fullScreenLost, setFullScreenLost] = useState(false);
  const [isFullscreenActive, setIsFullscreenActive] = useState(() => Boolean(document.fullscreenElement));

  // Persist answers to sessionStorage whenever they change
  useEffect(() => {
    if (attemptId && Object.keys(answers).length > 0) {
      sessionStorage.setItem(`answers:${attemptId}`, JSON.stringify(answers));
    }
  }, [answers, attemptId]);

  const { data: attempt, isLoading } = useQuery({
    queryKey: ['attempt', attemptId],
    queryFn: () => fetchAttemptById(attemptId).then(res => res.data),
    initialData: startData
      ? {
          attemptId: startData.attemptId,
          quizId: startData.quizId,
          quizVersionId: startData.quizVersionId,
          startedAt: startData.startedAt,
        }
      : undefined,
  });

  const { data: quiz } = useQuery({
    queryKey: ['quiz', attempt?.quizId],
    queryFn: () => fetchQuizById(attempt?.quizId).then(res => res.data),
    enabled: !!attempt?.quizId,
  });

  const isProctoredCompetition = Boolean(quiz?.competitionMode);

  const proctoring = useProctoring(attemptId, isProctoredCompetition);

  const handleProctoringTerminated = useCallback(async () => {
    sessionStorage.removeItem(`answers:${attemptId}`);
    sessionStorage.removeItem(`attempt:${attemptId}`);
    queryClient.invalidateQueries({ queryKey: ['myAttempts'] });
    queryClient.invalidateQueries({ queryKey: ['userStats'] });
    toast.error('Quiz auto-submitted due to proctoring violations.');
    navigate('/dashboard', {
      state: {
        noticeType: 'proctoring',
        noticeMessage: 'Quiz was auto-submitted because prohibited activity was detected.',
        attemptId,
      },
    });
  }, [attemptId, navigate, queryClient]);

  useEffect(() => {
    if (isProctoredCompetition) {
      proctoring.setOnTerminated(handleProctoringTerminated);
    }
  }, [isProctoredCompetition, proctoring.setOnTerminated, handleProctoringTerminated]);

  const questions = startData?.questions || [];
  const totalQuestions = questions.length;

  const buildAnswersPayload = useCallback(() => {
    return Object.keys(answers).map(qId => ({
      questionId: qId,
      selectedOptionId: answers[qId],
      responseTimeMs: (responseTimes[qId] || 0) * 1000,
    }));
  }, [answers, responseTimes]);

  const duration = startData?.durationSeconds || quiz?.durationSeconds || 0;
  const handleTimeUp = useCallback(async () => {
    try {
      const payload = buildAnswersPayload();
      await autoSubmitAttempt(attemptId, payload);
      if (isProctoredCompetition) {
        try {
          await endProctoringSession(attemptId);
        } catch {
          // Session may already be ended
        }
      }
      // Clear stored answers after auto-submission
      sessionStorage.removeItem(`answers:${attemptId}`);
      sessionStorage.removeItem(`attempt:${attemptId}`);
      // Invalidate queries to refresh attempt counts on dashboard
      queryClient.invalidateQueries({ queryKey: ['myAttempts'] });
      queryClient.invalidateQueries({ queryKey: ['userStats'] });
      toast.error('Time is up! Your result was auto-submitted.');
      navigate('/dashboard', {
        state: {
          noticeType: 'time',
          noticeMessage: 'Time is up. Your result was auto-submitted.',
          attemptId,
        },
      });
    } catch (err) {
      toast.error('Auto-submit failed');
    }
  }, [attemptId, navigate, queryClient, isProctoredCompetition, buildAnswersPayload]);

  // Fullscreen enforcement
  useEffect(() => {
    if (quiz?.requireFullScreen) {
      const requestFs = async () => {
        try {
          await document.documentElement.requestFullscreen();
        } catch (err) {}
      };
      requestFs();

      const handleFsChange = () => {
        const active = Boolean(document.fullscreenElement);
        setIsFullscreenActive(active);
        if (!active) {
          setFullScreenLost(true);
          toast.error('Full screen is required! Please re-enter full screen.');
        } else {
          setFullScreenLost(false);
        }
      };
      setIsFullscreenActive(Boolean(document.fullscreenElement));
      document.addEventListener('fullscreenchange', handleFsChange);
      return () => document.removeEventListener('fullscreenchange', handleFsChange);
    }
  }, [quiz]);

  // Tab switch detection
  useEffect(() => {
    if (quiz?.preventTabSwitch) {
      const handleVisibility = () => {
        if (document.hidden) {
          setTabWarning(true);
          toast.error('Tab switching is not allowed!');
        }
      };
      document.addEventListener('visibilitychange', handleVisibility);
      return () => document.removeEventListener('visibilitychange', handleVisibility);
    }
  }, [quiz]);

  // Prevent accidental navigation
  useBeforeUnload(
    useCallback((e) => {
      e.preventDefault();
      e.returnValue = '';
    }, [])
  );

  // Mark question as visited when index changes
  useEffect(() => {
    setVisited(prev => ({ ...prev, [currentIndex]: true }));
  }, [currentIndex]);

  const handleAnswerSelect = (optionId) => {
    const questionId = questions[currentIndex]?.id;
    if (!questionId) return;
    setAnswers(prev => ({ ...prev, [questionId]: optionId }));
  };

  const clearAnswer = () => {
    const questionId = questions[currentIndex]?.id;
    if (!questionId) return;
    setAnswers(prev => {
      const copy = { ...prev };
      delete copy[questionId];
      return copy;
    });
  };

  const toggleMarkForReview = () => {
    setMarkedForReview(prev => ({
      ...prev,
      [currentIndex]: !prev[currentIndex],
    }));
  };

  const navigateQuestion = (index) => {
    if (index < 0 || index >= totalQuestions) return;
    // record response time for previous question
    const prevId = questions[currentIndex]?.id;
    if (prevId && totalQuestions > 0) {
      const elapsed = Math.round((Date.now() - startTime) / 1000);
      setResponseTimes(prev => ({ ...prev, [prevId]: elapsed }));
      setStartTime(Date.now());
    }
    setCurrentIndex(index);
  };

  const handleSubmit = async () => {
    setIsSubmitting(true);
    try {
      const payload = buildAnswersPayload();
      await submitAttempt(attemptId, payload);
      if (isProctoredCompetition) {
        try {
          await endProctoringSession(attemptId);
        } catch {
          // Session may already be ended
        }
      }
      // Clear stored answers after successful submission
      sessionStorage.removeItem(`answers:${attemptId}`);
      sessionStorage.removeItem(`attempt:${attemptId}`);
      // Invalidate queries to refresh attempt counts on dashboard
      queryClient.invalidateQueries({ queryKey: ['myAttempts'] });
      queryClient.invalidateQueries({ queryKey: ['userStats'] });
      toast.success('Quiz submitted!');
      navigate('/dashboard', {
        state: {
          noticeType: 'submitted',
          noticeMessage: 'Quiz submitted successfully.',
          attemptId,
        },
      });
    } catch (err) {
      toast.error('Submission failed');
    } finally {
      setIsSubmitting(false);
    }
  };

  if (isLoading || !attempt) return <div className="p-8 text-center">Loading attempt...</div>;

  if (isProctoredCompetition && proctoring.initializing) {
    return <div className="p-8 text-center">Initializing proctoring...</div>;
  }

  if (isProctoredCompetition && !proctoring.referenceCaptured && !proctoring.sessionTerminated) {
    return (
      <ReferenceCaptureGate
        videoRef={proctoring.videoRef}
        canvasRef={proctoring.canvasRef}
        maxLives={proctoring.maxLives}
        remainingLives={proctoring.remainingLives}
        statusMessages={proctoring.statusMessages}
        riskLevel={proctoring.riskLevel}
        verificationStatus={proctoring.verificationStatus}
        connected={proctoring.connected}
        cameraError={proctoring.cameraError}
        cameraReady={proctoring.cameraReady}
        capturingReference={proctoring.capturingReference}
        onCaptureReference={proctoring.captureReference}
      />
    );
  }

  if (isProctoredCompetition && proctoring.sessionTerminated) {
    return <div className="p-8 text-center">Quiz terminated. Redirecting...</div>;
  }

  if (quiz?.requireFullScreen && !isFullscreenActive) {
    return (
      <div className="min-h-[calc(100vh-120px)] flex items-center justify-center p-6">
        <div className="max-w-lg w-full rounded-2xl border border-amber-200 bg-white p-8 text-center shadow-xl dark:border-amber-900/40 dark:bg-gray-800">
          <h2 className="text-2xl font-bold text-amber-600 dark:text-amber-300">Full Screen Required</h2>
          <p className="mt-3 text-sm text-gray-600 dark:text-gray-300">
            This quiz can only be attended while the browser is in full screen mode.
            Re-enter full screen to continue the quiz.
          </p>
          <Button
            className="mt-6"
            onClick={async () => {
              try {
                await document.documentElement.requestFullscreen();
              } catch (err) {}
            }}
          >
            Re-enter Full Screen
          </Button>
        </div>
      </div>
    );
  }

  return (
    <div className={`flex flex-col ${isProctoredCompetition ? 'p-4' : ''} h-[calc(100vh-120px)]`}>
      {isProctoredCompetition && (
        <ProctoringPanel
          videoRef={proctoring.videoRef}
          canvasRef={proctoring.canvasRef}
          maxLives={proctoring.maxLives}
          remainingLives={proctoring.remainingLives}
          statusMessages={proctoring.statusMessages}
          riskLevel={proctoring.riskLevel}
          verificationStatus={proctoring.verificationStatus}
          connected={proctoring.connected}
          cameraError={proctoring.cameraError}
        />
      )}

      <div className="flex flex-col lg:flex-row gap-4 flex-1 min-h-0">
      {/* Left sidebar: question navigator & timer */}
      <div className="lg:w-56 xl:w-60 flex-shrink-0 bg-white dark:bg-gray-800 rounded-2xl p-4 shadow flex flex-col">
        <div className="mb-4">
          {duration > 0 ? (
            <Timer 
              initialSeconds={duration} 
              startedAt={startData?.startedAt || attempt?.startedAt}
              onTimeUp={handleTimeUp} 
            />
          ) : (
            <div className="font-semibold">Loading timer...</div>
          )}
        </div>
        <QuestionNavigator
          total={totalQuestions}
          current={currentIndex}
          answers={questions.reduce((acc, q, idx) => {
            if (answers[q.id]) acc[idx] = answers[q.id];
            return acc;
          }, {})}
          visited={visited}
          markedForReview={markedForReview}
          onNavigate={navigateQuestion}
        />
        <div className="mt-4 space-y-2 text-xs">
          <div className="flex items-center gap-2"><span className="w-3 h-3 rounded-full bg-green-500"></span> Answered</div>
          <div className="flex items-center gap-2"><span className="w-3 h-3 rounded-full bg-red-500"></span> Not Answered</div>
          <div className="flex items-center gap-2"><span className="w-3 h-3 rounded-full bg-yellow-500"></span> Marked for Review</div>
          <div className="flex items-center gap-2"><span className="w-3 h-3 rounded-full bg-orange-500"></span> Answered & Marked</div>
        </div>
        <Button onClick={() => setShowSubmitModal(true)} className="mt-auto w-full" variant="primary">
          <Send className="w-4 h-4" /> Submit
        </Button>
      </div>

      {/* Main question area */}
      <div className="flex-1 bg-white dark:bg-gray-800 rounded-2xl p-4 md:p-5 shadow flex flex-col overflow-auto">
        {totalQuestions > 0 ? (
          <>
            <div className="flex justify-between items-center mb-4">
              <h2 className="text-xl font-semibold">Question {currentIndex + 1} of {totalQuestions}</h2>
              <button
                onClick={toggleMarkForReview}
                className={`flex items-center gap-1 px-3 py-1 rounded-lg text-sm ${markedForReview[currentIndex] ? 'bg-yellow-100 text-yellow-800' : 'bg-gray-100 dark:bg-gray-700'}`}
              >
                <Flag className="w-4 h-4" /> {markedForReview[currentIndex] ? 'Marked' : 'Mark for Review'}
              </button>
            </div>

            {questions[currentIndex]?.imageUrl && (
              <div className="mb-3 flex justify-center">
                <img
                  src={questions[currentIndex].imageUrl}
                  alt="Question"
                  className="max-h-36 w-auto max-w-full rounded-xl object-contain sm:max-h-40 md:max-h-44"
                />
              </div>
            )}
            <div className="text-lg font-medium mb-3">{questions[currentIndex]?.stem || questions[currentIndex]?.text}</div>

            <div className="space-y-2 mb-3">
              {questions[currentIndex]?.options?.map(opt => (
                <button
                  key={opt.id}
                  onClick={() => handleAnswerSelect(opt.id)}
                  className={`w-full text-left px-4 py-3 rounded-xl border-2 transition ${answers[questions[currentIndex].id] === opt.id ? 'border-brand-500 bg-brand-50 dark:bg-brand-900/20' : 'border-gray-200 dark:border-gray-700 hover:border-gray-300'}`}
                >
                  {opt.text || opt.optionText}
                </button>
              ))}
            </div>

            <div className="flex justify-between mt-auto pt-1">
              <Button
                variant="secondary"
                onClick={() => navigateQuestion(currentIndex - 1)}
                disabled={currentIndex === 0}
              >
                <ChevronLeft className="w-5 h-5" /> Previous
              </Button>
              <Button variant="ghost" onClick={clearAnswer}>
                <RotateCcw className="w-4 h-4" /> Clear
              </Button>
              <Button
                onClick={() => navigateQuestion(currentIndex + 1)}
                disabled={currentIndex === totalQuestions - 1}
              >
                Next <ChevronRight className="w-5 h-5" />
              </Button>
            </div>
          </>
        ) : (
          <div className="flex items-center justify-center h-full text-gray-500">
            <p>No questions loaded yet. The backend must provide the question list.</p>
          </div>
        )}
      </div>
      </div>

      {/* Tab warning modal */}
      <Modal isOpen={tabWarning} onClose={() => setTabWarning(false)} title="Warning">
        <p>Tab switching is not allowed during this quiz. Please stay on this tab.</p>
      </Modal>

      {/* Fullscreen lost warning */}
      <Modal isOpen={fullScreenLost} onClose={() => setFullScreenLost(false)} title="Full Screen Required">
        <p>This quiz requires full screen mode. Please enable full screen to continue.</p>
        <Button className="mt-4" onClick={() => document.documentElement.requestFullscreen()}>Re-enter Full Screen</Button>
      </Modal>

      {/* Confirm submit */}
      <ConfirmDialog
        isOpen={showSubmitModal}
        onClose={() => setShowSubmitModal(false)}
        onConfirm={handleSubmit}
        title="Submit Quiz"
        message={`You have answered ${Object.keys(answers).length} out of ${totalQuestions} questions. Are you sure you want to submit?`}
        isLoading={isSubmitting}
      />
    </div>
  );
}
