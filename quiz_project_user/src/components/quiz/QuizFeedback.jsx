import { useState } from 'react';
import RatingStars from '../ui/RatingStars';
import Button from '../ui/Button';
import { useMutation } from '@tanstack/react-query';
import { submitQuizFeedback, submitQuestionFeedback } from '../../api/feedbackService';
import toast from 'react-hot-toast';

export default function QuizFeedback({ quizId, questionId, onSuccess }) {
  const [rating, setRating] = useState(0);
  const [message, setMessage] = useState('');

  const mutation = useMutation({
    mutationFn: () =>
      questionId
        ? submitQuestionFeedback(quizId, questionId, rating, message)
        : submitQuizFeedback(quizId, rating, message),
    onSuccess: () => {
      toast.success('Feedback submitted!');
      setRating(0);
      setMessage('');
      onSuccess?.();
    },
  });

  return (
    <div className="space-y-4">
      <h4 className="font-semibold">{questionId ? 'Question Feedback' : 'Quiz Feedback'}</h4>
      <RatingStars rating={rating} onRate={setRating} interactive />
      <textarea
        value={message}
        onChange={(e) => setMessage(e.target.value)}
        placeholder="Share your thoughts (optional)"
        className="w-full rounded-lg border p-3 dark:bg-gray-800 dark:border-gray-700"
        rows={3}
      />
      <Button onClick={() => mutation.mutate()} isLoading={mutation.isPending} disabled={rating === 0}>
        Submit Feedback
      </Button>
    </div>
  );
}