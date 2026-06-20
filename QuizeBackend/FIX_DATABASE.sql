-- ================================================================
-- QUICK FIX: Run this SQL script in your PostgreSQL database
-- ================================================================
-- This fixes the "column contains null values" error
-- Run this BEFORE starting the Spring Boot application
-- ================================================================

-- Fix quizzes table
ALTER TABLE quizzes ADD COLUMN IF NOT EXISTS proctoring_enabled BOOLEAN;
ALTER TABLE quizzes ADD COLUMN IF NOT EXISTS initial_lives INTEGER;

UPDATE quizzes SET proctoring_enabled = FALSE WHERE proctoring_enabled IS NULL;
UPDATE quizzes SET initial_lives = 3 WHERE initial_lives IS NULL;

ALTER TABLE quizzes ALTER COLUMN proctoring_enabled SET NOT NULL;
ALTER TABLE quizzes ALTER COLUMN proctoring_enabled SET DEFAULT FALSE;

-- Fix quiz_attempts table
ALTER TABLE quiz_attempts ADD COLUMN IF NOT EXISTS auto_submit_reason VARCHAR(64);
ALTER TABLE quiz_attempts ADD COLUMN IF NOT EXISTS proctoring_enabled BOOLEAN;

UPDATE quiz_attempts SET proctoring_enabled = FALSE WHERE proctoring_enabled IS NULL;

ALTER TABLE quiz_attempts ALTER COLUMN proctoring_enabled SET NOT NULL;
ALTER TABLE quiz_attempts ALTER COLUMN proctoring_enabled SET DEFAULT FALSE;

-- Create proctoring_sessions table
CREATE TABLE IF NOT EXISTS proctoring_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    quiz_attempt_id UUID NOT NULL UNIQUE,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    started_at TIMESTAMP NOT NULL,
    ended_at TIMESTAMP,
    initial_lives INTEGER NOT NULL DEFAULT 3,
    remaining_lives INTEGER NOT NULL DEFAULT 3,
    total_violations INTEGER NOT NULL DEFAULT 0,
    critical_violations INTEGER NOT NULL DEFAULT 0,
    warning_violations INTEGER NOT NULL DEFAULT 0,
    risk_score INTEGER NOT NULL DEFAULT 0,
    risk_level VARCHAR(32) NOT NULL DEFAULT 'LOW',
    proctoring_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    session_token VARCHAR(500),
    python_service_url VARCHAR(500),
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_proctoring_session_attempt FOREIGN KEY (quiz_attempt_id) 
        REFERENCES quiz_attempts(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_proctoring_session_attempt ON proctoring_sessions(quiz_attempt_id);
CREATE INDEX IF NOT EXISTS idx_proctoring_session_status ON proctoring_sessions(status);
CREATE INDEX IF NOT EXISTS idx_proctoring_session_risk ON proctoring_sessions(risk_level);

-- Create proctoring_events table
CREATE TABLE IF NOT EXISTS proctoring_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    proctoring_session_id UUID NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    severity VARCHAR(32) NOT NULL DEFAULT 'INFO',
    event_timestamp TIMESTAMP NOT NULL,
    description TEXT,
    frame_data_url VARCHAR(2000),
    confidence_score DOUBLE PRECISION,
    metadata TEXT,
    lives_deducted INTEGER NOT NULL DEFAULT 0,
    remaining_lives_snapshot INTEGER,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_proctoring_event_session FOREIGN KEY (proctoring_session_id) 
        REFERENCES proctoring_sessions(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_proctoring_event_session ON proctoring_events(proctoring_session_id);
CREATE INDEX IF NOT EXISTS idx_proctoring_event_timestamp ON proctoring_events(event_timestamp);
CREATE INDEX IF NOT EXISTS idx_proctoring_event_type ON proctoring_events(event_type);

-- Create proctoring_violations table
CREATE TABLE IF NOT EXISTS proctoring_violations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    proctoring_session_id UUID NOT NULL,
    violation_type VARCHAR(64) NOT NULL,
    violation_timestamp TIMESTAMP NOT NULL,
    description TEXT,
    snapshot_url VARCHAR(2000),
    evidence_data TEXT,
    lives_deducted INTEGER NOT NULL DEFAULT 1,
    remaining_lives_after INTEGER NOT NULL,
    auto_flagged BOOLEAN NOT NULL DEFAULT TRUE,
    reviewed BOOLEAN NOT NULL DEFAULT FALSE,
    reviewed_at TIMESTAMP,
    reviewer_notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_proctoring_violation_session FOREIGN KEY (proctoring_session_id) 
        REFERENCES proctoring_sessions(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_proctoring_violation_session ON proctoring_violations(proctoring_session_id);
CREATE INDEX IF NOT EXISTS idx_proctoring_violation_type ON proctoring_violations(violation_type);
CREATE INDEX IF NOT EXISTS idx_proctoring_violation_timestamp ON proctoring_violations(violation_timestamp);

-- Done!
SELECT 'Database migration completed successfully!' AS status;
