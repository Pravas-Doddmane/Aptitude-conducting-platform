import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import Card from '../ui/Card';
import Badge from '../ui/Badge';
import Button from '../ui/Button';
import { Clock, HelpCircle, Award, ChevronDown, ChevronUp, Monitor, Camera, Mic, MapPin, ShieldAlert } from 'lucide-react';
import { formatDuration, truncateText } from '../../utils/formatters';

export default function QuizCard({ quiz, attemptCount, maxAttempts }) {
  const [expanded, setExpanded] = useState(false);
  const navigate = useNavigate();
  const categoryLabel = quiz.categoryNames?.length ? quiz.categoryNames.join(', ') : quiz.categoryName;

  const isUnlimited = maxAttempts == null || maxAttempts <= 0;
  const attemptsLeft = isUnlimited ? Infinity : Math.max(0, maxAttempts - (attemptCount || 0));
  const canAttempt = isUnlimited || attemptsLeft > 0;
  const isCurrentlyAvailable = quiz.currentlyAvailable !== false;
  const inProgress = false; // you may check attempt status from attempt history

  return (
    <Card className="p-6 flex flex-col">
      <div className="flex justify-between items-start">
        <h3 className="text-xl font-bold">{quiz.title}</h3>
        <Badge color="purple">{categoryLabel}</Badge>
      </div>
      <p className="text-gray-600 dark:text-gray-300 mt-2">{truncateText(quiz.description, 120)}</p>

      <div className="mt-4 grid grid-cols-2 gap-3 text-sm">
        <div className="flex items-center gap-1"><Clock className="w-4 h-4" /> {formatDuration(quiz.durationSeconds)}</div>
        <div className="flex items-center gap-1"><HelpCircle className="w-4 h-4" /> {quiz.questionCount} Qs</div>
        {quiz.certificateEnabled && <div className="flex items-center gap-1"><Award className="w-4 h-4" /> Certificate</div>}
        <div className="flex items-center gap-1">
          <Monitor className="w-4 h-4" /> {quiz.requireFullScreen ? 'Fullscreen' : 'Normal'}
        </div>
      </div>

      <div className="mt-3 flex flex-wrap gap-2">
        {quiz.competitionMode && <Badge color="red"><ShieldAlert className="w-3 h-3 inline mr-1" />Competition</Badge>}
        {quiz.requireFullScreen && <Badge color="yellow">Fullscreen req.</Badge>}
        {quiz.preventTabSwitch && <Badge color="yellow">No tab switch</Badge>}
        {quiz.requireCamera && <Badge color="yellow"><Camera className="w-3 h-3 inline mr-1" />Camera</Badge>}
        {quiz.requireMicrophone && <Badge color="yellow"><Mic className="w-3 h-3 inline mr-1" />Mic</Badge>}
        {quiz.requireLocation && <Badge color="yellow"><MapPin className="w-3 h-3 inline mr-1" />Location</Badge>}
      </div>

      <div className="mt-4 flex items-center justify-between">
        <div className="text-sm">
          {isUnlimited ? (
            <span className="text-green-600 font-medium">Unlimited attempts</span>
          ) : (
            <span>{attemptsLeft} attempt{attemptsLeft !== 1 ? 's' : ''} left</span>
          )}
        </div>
        <Button onClick={() => navigate(`/quizzes/${quiz.id}`)} disabled={!canAttempt || !isCurrentlyAvailable}>
          {inProgress ? 'Continue' : 'Start'}
        </Button>
      </div>

      <button
        onClick={() => setExpanded(!expanded)}
        className="mt-4 flex items-center text-sm text-brand-600 hover:underline"
      >
        {expanded ? <ChevronUp className="w-4 h-4" /> : <ChevronDown className="w-4 h-4" />}
        {expanded ? 'Less details' : 'More details'}
      </button>

      {expanded && (
        <div className="mt-4 text-sm border-t dark:border-gray-700 pt-4 space-y-2">
          <p><strong>Categories:</strong> {categoryLabel}</p>
          <p><strong>Questions:</strong> {quiz.questionCount} (Easy: {quiz.easyQuestionCount}, Medium: {quiz.mediumQuestionCount}, Hard: {quiz.hardQuestionCount})</p>
          <p><strong>Certificate:</strong> {quiz.certificateEnabled ? `Yes (${quiz.certificateTemplateName || 'Default'})` : 'No'}</p>
          <p><strong>Competition Mode:</strong> {quiz.competitionMode ? 'Yes' : 'No'}</p>
          <p><strong>Requirements:</strong></p>
          <ul className="list-disc list-inside">
            {quiz.requireFullScreen && <li>Full screen required</li>}
            {quiz.preventTabSwitch && <li>Tab switching not allowed</li>}
            {quiz.requireCamera && <li>Camera access required</li>}
            {quiz.requireMicrophone && <li>Microphone access required</li>}
            {quiz.requireLocation && <li>Location access required</li>}
          </ul>
          {quiz.availableFrom && <p><strong>Available from:</strong> {new Date(quiz.availableFrom).toLocaleString()}</p>}
          {quiz.availableTo && <p><strong>Available to:</strong> {new Date(quiz.availableTo).toLocaleString()}</p>}
          <p><strong>Status:</strong> {isCurrentlyAvailable ? 'Available now' : 'Not available right now'}</p>
        </div>
      )}
    </Card>
  );
}
