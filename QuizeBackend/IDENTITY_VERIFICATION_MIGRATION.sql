-- ================================================================
-- IDENTITY VERIFICATION & EVIDENCE STORAGE MIGRATION
-- ================================================================
-- This SQL script adds Identity Verification and Evidence Storage features
-- Run this in your PostgreSQL database AFTER running FIX_DATABASE.sql
-- ================================================================

-- ================================================================
-- STEP 1: Add evidence_url column to proctoring_violations table
-- ================================================================

-- Add evidence_url column as nullable first
ALTER TABLE proctoring_violations 
ADD COLUMN IF NOT EXISTS evidence_url VARCHAR(2000);

-- Create index for better query performance
CREATE INDEX IF NOT EXISTS idx_proctoring_violation_evidence 
ON proctoring_violations(evidence_url) 
WHERE evidence_url IS NOT NULL;

-- ================================================================
-- STEP 2: Create identity_verifications table
-- ================================================================

CREATE TABLE IF NOT EXISTS identity_verifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    proctoring_session_id UUID NOT NULL UNIQUE,
    user_id UUID NOT NULL,
    reference_image_url VARCHAR(2000) NOT NULL,
    reference_captured_at TIMESTAMP NOT NULL,
    verification_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    verified_at TIMESTAMP,
    total_verifications INTEGER NOT NULL DEFAULT 0,
    successful_verifications INTEGER NOT NULL DEFAULT 0,
    failed_verifications INTEGER NOT NULL DEFAULT 0,
    average_match_score DOUBLE PRECISION,
    lowest_match_score DOUBLE PRECISION,
    last_verification_at TIMESTAMP,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_identity_verification_session FOREIGN KEY (proctoring_session_id) 
        REFERENCES proctoring_sessions(id) ON DELETE CASCADE,
    CONSTRAINT fk_identity_verification_user FOREIGN KEY (user_id) 
        REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_identity_verification_status 
        CHECK (verification_status IN ('PENDING', 'IN_PROGRESS', 'VERIFIED', 'FAILED', 'REJECTED'))
);

-- Create indexes for identity_verifications
CREATE INDEX IF NOT EXISTS idx_identity_verification_session 
ON identity_verifications(proctoring_session_id);

CREATE INDEX IF NOT EXISTS idx_identity_verification_user 
ON identity_verifications(user_id);

CREATE INDEX IF NOT EXISTS idx_identity_verification_status 
ON identity_verifications(verification_status);

CREATE INDEX IF NOT EXISTS idx_identity_verification_captured_at 
ON identity_verifications(reference_captured_at);

-- ================================================================
-- STEP 3: Create verification_attempts table
-- ================================================================

CREATE TABLE IF NOT EXISTS verification_attempts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    identity_verification_id UUID NOT NULL,
    captured_image_url VARCHAR(2000) NOT NULL,
    verification_timestamp TIMESTAMP NOT NULL,
    verification_result VARCHAR(32) NOT NULL,
    match_score DOUBLE PRECISION,
    confidence_level DOUBLE PRECISION,
    processing_time_ms INTEGER,
    details TEXT,
    evidence_url VARCHAR(2000),
    triggered_violation BOOLEAN NOT NULL DEFAULT FALSE,
    violation_id UUID,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_verification_attempt_verification FOREIGN KEY (identity_verification_id) 
        REFERENCES identity_verifications(id) ON DELETE CASCADE,
    CONSTRAINT fk_verification_attempt_violation FOREIGN KEY (violation_id) 
        REFERENCES proctoring_violations(id) ON DELETE SET NULL,
    CONSTRAINT chk_verification_result 
        CHECK (verification_result IN ('MATCHED', 'NOT_MATCHED', 'LOW_CONFIDENCE', 
                                        'FACE_NOT_FOUND', 'MULTIPLE_FACES_FOUND', 
                                        'PROCESSING_ERROR', 'TIMEOUT_ERROR'))
);

-- Create indexes for verification_attempts
CREATE INDEX IF NOT EXISTS idx_verification_attempt_verification 
ON verification_attempts(identity_verification_id);

CREATE INDEX IF NOT EXISTS idx_verification_attempt_timestamp 
ON verification_attempts(verification_timestamp);

CREATE INDEX IF NOT EXISTS idx_verification_attempt_result 
ON verification_attempts(verification_result);

CREATE INDEX IF NOT EXISTS idx_verification_attempt_violation 
ON verification_attempts(violation_id) 
WHERE violation_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_verification_attempt_triggered 
ON verification_attempts(triggered_violation) 
WHERE triggered_violation = TRUE;

-- ================================================================
-- STEP 4: Add helpful comments
-- ================================================================

COMMENT ON TABLE identity_verifications IS 
'Stores reference images and verification statistics for identity verification during proctored quizzes';

COMMENT ON COLUMN identity_verifications.reference_image_url IS 
'URL to the reference image captured before quiz start - supports local storage, S3, CDN';

COMMENT ON COLUMN identity_verifications.verification_status IS 
'Current verification status: PENDING, IN_PROGRESS, VERIFIED, FAILED, REJECTED';

COMMENT ON TABLE verification_attempts IS 
'Stores individual face comparison attempts during quiz with match scores and results';

COMMENT ON COLUMN verification_attempts.verification_result IS 
'Result of face comparison: MATCHED, NOT_MATCHED, LOW_CONFIDENCE, FACE_NOT_FOUND, etc.';

COMMENT ON COLUMN verification_attempts.evidence_url IS 
'URL to evidence image showing the verification result - used for identity mismatch violations';

COMMENT ON COLUMN verification_attempts.triggered_violation IS 
'TRUE if this verification attempt triggered an identity mismatch violation';

COMMENT ON COLUMN proctoring_violations.evidence_url IS 
'URL to evidence file supporting this violation - supplements snapshot_url for identity verification cases';

-- ================================================================
-- STEP 5: Verification and summary
-- ================================================================

-- Verify tables exist
SELECT 
    table_name, 
    (SELECT COUNT(*) FROM information_schema.columns WHERE table_name = t.table_name) as column_count
FROM information_schema.tables t
WHERE table_schema = 'public' 
AND table_name IN ('identity_verifications', 'verification_attempts', 'proctoring_violations')
ORDER BY table_name;

-- Verify foreign keys
SELECT 
    tc.table_name, 
    kcu.column_name, 
    ccu.table_name AS foreign_table_name,
    ccu.column_name AS foreign_column_name
FROM information_schema.table_constraints AS tc 
JOIN information_schema.key_column_usage AS kcu
  ON tc.constraint_name = kcu.constraint_name
JOIN information_schema.constraint_column_usage AS ccu
  ON ccu.constraint_name = tc.constraint_name
WHERE tc.constraint_type = 'FOREIGN KEY' 
AND tc.table_name IN ('identity_verifications', 'verification_attempts')
ORDER BY tc.table_name;

-- Summary
SELECT 'Identity Verification Migration Completed Successfully!' AS status;
SELECT 'Tables created: identity_verifications, verification_attempts' AS tables;
SELECT 'Column added: proctoring_violations.evidence_url' AS columns;
SELECT 'Ready for Identity Verification and Evidence Storage features' AS next_steps;

