ALTER TABLE quizzes
ADD COLUMN IF NOT EXISTS available_from timestamptz NULL;

ALTER TABLE quizzes
ADD COLUMN IF NOT EXISTS available_to timestamptz NULL;
