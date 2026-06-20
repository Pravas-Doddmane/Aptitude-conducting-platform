import { recordProctoringViolation, recordProctoringEvent, getProctoringSession } from '../api/proctoringService';
import { recordVerificationAttempt } from '../api/identityVerificationService';

const VIOLATION_TYPE_MAP = {
  MULTIPLE_FACES: 'MULTIPLE_FACES',
  FACE_MISSING: 'FACE_NOT_VISIBLE',
  IDENTITY_FAIL: 'IDENTITY_MISMATCH',
};

const VERIFICATION_RESULT_MAP = {
  MATCHED: 'MATCHED',
  NOT_MATCHED: 'NOT_MATCHED',
  LOW_CONFIDENCE: 'LOW_CONFIDENCE',
  FACE_NOT_FOUND: 'NOT_MATCHED',
};

export async function syncLifeDeductionToBackend(attemptId, pythonPayload, frameDataUrl) {
  const { violation_type: violationType, match_score: matchScore, verification_status: verificationStatus } =
    pythonPayload;

  if (!violationType) return null;

  const timestamp = new Date().toISOString();
  const snapshotUrl = frameDataUrl || null;

  if (violationType === 'IDENTITY_FAIL') {
    const result = VERIFICATION_RESULT_MAP[verificationStatus] || 'NOT_MATCHED';
    return recordVerificationAttempt(attemptId, {
      capturedImageUrl: snapshotUrl || 'proctoring-frame',
      verificationTimestamp: timestamp,
      verificationResult: result,
      matchScore: matchScore ?? 0,
      confidenceLevel: matchScore ?? 0,
      processingTimeMs: 0,
      details: `Python proctoring violation: ${violationType}`,
      evidenceUrl: snapshotUrl,
    }).then((res) => res.data);
  }

  const backendViolationType = VIOLATION_TYPE_MAP[violationType] || violationType;
  return recordProctoringViolation(attemptId, {
    violationType: backendViolationType,
    violationTimestamp: timestamp,
    description: `Proctoring violation: ${violationType}`,
    snapshotUrl,
    evidenceUrl: snapshotUrl,
    evidenceData: JSON.stringify({
      source: 'python-proctoring-service',
      verificationStatus,
      matchScore,
    }),
    livesDeducted: 1,
  }).then((res) => res.data);
}

export async function syncStatusEventToBackend(attemptId, eventType, description, severity = 'INFO') {
  return recordProctoringEvent(attemptId, {
    eventType,
    severity,
    eventTimestamp: new Date().toISOString(),
    description,
    livesDeducted: 0,
  }).then((res) => res.data);
}

export async function forceBackendAutoSubmitIfNeeded(attemptId) {
  const sessionRes = await getProctoringSession(attemptId);
  const remaining = sessionRes.data?.remainingLives ?? 0;
  if (remaining <= 0) return sessionRes.data;

  return recordProctoringViolation(attemptId, {
    violationType: 'SUSPICIOUS_ACTIVITY',
    violationTimestamp: new Date().toISOString(),
    description: 'Proctoring session terminated — life limit exceeded',
    livesDeducted: remaining,
  }).then((res) => res.data);
}
