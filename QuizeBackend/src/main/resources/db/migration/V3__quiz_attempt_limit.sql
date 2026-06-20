ALTER TABLE quizzes
ADD COLUMN IF NOT EXISTS max_attempts_per_user INTEGER NOT NULL DEFAULT 0;

UPDATE quizzes
SET max_attempts_per_user = 0
WHERE max_attempts_per_user IS NULL;
