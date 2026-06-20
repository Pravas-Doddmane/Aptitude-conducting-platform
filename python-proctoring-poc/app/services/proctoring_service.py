import time

import numpy as np

from app.config import settings
from app.core.session import ProctoringSession
from app.utils.logger import logger
from app.vision.face_detector import FaceDetectionResult, FaceDetector
from app.vision.identity_verifier import IdentityVerifier


class ProctoringService:
    def __init__(self, session: ProctoringSession):
        self.session = session
        self.face_detector = FaceDetector()
        self.identity_verifier = IdentityVerifier()
        self.last_life_deducted = False
        self.last_violation_type = None

        if session.reference_image is not None:
            success = self.identity_verifier.set_reference_embedding(session.reference_image)
            if not success:
                logger.warning("Could not extract embedding from stored reference image, will retry later.")

    def process_frame(self, frame: np.ndarray) -> dict:
        if self.session.terminated:
            return self._terminated_response()

        self.last_life_deducted = False
        self.last_violation_type = None
        status_msgs: list[str] = []
        risk_factors: list[str] = []
        face_quality: dict | None = None

        detection = self.face_detector.detect(frame)
        face_detected = detection.face_count > 0
        proctoring_active = self.session.reference_image is not None

        if detection.face_count == 1:
            self.session.missing_face_streak = 0
            self.session.multiple_face_streak = 0
            status_msgs.append("Face detected successfully.")
        elif detection.face_count == 0:
            self.session.missing_face_streak += 1
            self.session.multiple_face_streak = 0
            status_msgs.append("No face detected. Please return to the camera.")
            risk_factors.append("FACE_MISSING")
            if proctoring_active and self.session.missing_face_streak >= settings.FACE_MISSING_STREAK_NEEDED:
                self._apply_violation("FACE_MISSING")
        else:
            self.session.multiple_face_streak += 1
            self.session.missing_face_streak = 0
            status_msgs.append("Multiple faces detected.")
            risk_factors.append("MULTIPLE_FACES")
            if proctoring_active and self.session.multiple_face_streak >= settings.MULTIPLE_FACE_STREAK_NEEDED:
                self._apply_violation("MULTIPLE_FACES")

        primary_face = self._get_primary_face(frame, detection)
        if primary_face is not None:
            face_quality = self.face_detector.assess_face_quality(primary_face, frame.shape)
            if not face_quality["acceptable"]:
                status_msgs.append(self._quality_message(face_quality["reason"]))
                risk_factors.append(face_quality["reason"])

        identity_status = "NO_REFERENCE" if self.session.reference_image is None else "PENDING"
        match_score = 0.0
        now = time.time()
        in_grace_period = (
            self.session.reference_captured_time > 0
            and (now - self.session.reference_captured_time) < settings.IDENTITY_GRACE_PERIOD_SEC
        )

        if self.session.reference_image is not None and detection.face_count == 1:
            if in_grace_period:
                identity_status = "GRACE_PERIOD"
                status_msgs.append("Calibrating reference face. Please hold still.")
            elif primary_face is None or (face_quality and not face_quality["acceptable"]):
                identity_status = "FACE_QUALITY_LOW"
            else:
                identity_result = self.identity_verifier.verify(primary_face)
                identity_status = identity_result["status"]
                match_score = identity_result["match_score"]
                self._update_identity_state(identity_status, status_msgs, risk_factors)

        if self.last_life_deducted:
            status_msgs.append("1 life deducted.")

        response = {
            "face_detected": face_detected,
            "face_count": detection.face_count,
            "match_score": round(match_score, 3),
            "verification_status": identity_status,
            "risk_level": self._calculate_risk_level(risk_factors),
            "risk_score": min(100, self.session.violation_count * 30 + len(risk_factors) * 10),
            "max_lives": settings.MAX_LIVES,
            "remaining_lives": self.session.lives,
            "violation_count": self.session.violation_count,
            "session_terminated": self.session.terminated,
            "termination_reason": self.session.termination_reason,
            "status_messages": status_msgs,
            "life_deducted": self.last_life_deducted,
            "violation_type": self.last_violation_type,
            "face_quality": face_quality,
        }
        return response

    def capture_reference(self, frame: np.ndarray) -> tuple[bool, str]:
        """Capture a reference only when one clear face is present."""
        detection = self.face_detector.detect(frame)
        if detection.face_count != 1:
            logger.warning("Cannot capture reference: %s faces detected.", detection.face_count)
            return False, "Ensure exactly one face is visible."

        primary_face = self._get_primary_face(frame, detection)
        if primary_face is None:
            return False, "Could not isolate the face. Please try again."

        quality = self.face_detector.assess_face_quality(primary_face, frame.shape)
        if not quality["acceptable"]:
            logger.warning("Reference capture rejected because face quality was too low: %s", quality["reason"])
            return False, self._quality_message(quality["reason"])

        success = self.identity_verifier.set_reference_embedding(primary_face)
        if not success:
            logger.error("Reference capture failed during embedding extraction.")
            return False, "Face detected, but identity features could not be extracted."

        self.session.set_reference_image(primary_face)
        logger.info("Reference face captured and stored.")
        return True, "Reference face captured successfully."

    def reset_session(self):
        self.session.reset_lives()
        self.session.reference_image = None
        self.identity_verifier.reset()

    def _update_identity_state(self, identity_status: str, status_msgs: list[str], risk_factors: list[str]):
        if identity_status == "MATCHED":
            self.session.identity_match_streak += 1
            self.session.identity_failure_streak = 0
            if self.session.identity_match_streak >= settings.IDENTITY_MATCH_CONFIRMATIONS:
                status_msgs.append("Identity verified.")
            else:
                status_msgs.append("Confirming identity...")
        elif identity_status == "LOW_CONFIDENCE":
            self.session.identity_failure_streak = 0
            self.session.identity_match_streak = 0
            status_msgs.append("Face found, but match confidence is low.")
            risk_factors.append("LOW_CONFIDENCE")
        elif identity_status in {"NOT_MATCHED", "FACE_NOT_FOUND"}:
            self.session.identity_failure_streak += 1
            self.session.identity_match_streak = 0
            status_msgs.append(
                f"Identity mismatch ({self.session.identity_failure_streak}/{settings.IDENTITY_FAILURE_STREAK_NEEDED})."
            )
            risk_factors.append("IDENTITY_FAIL")
            if self.session.identity_failure_streak >= settings.IDENTITY_FAILURE_STREAK_NEEDED:
                status_msgs.append("Identity verification failed.")
                self._apply_violation("IDENTITY_FAIL")
                self.session.identity_failure_streak = 0
        else:
            self.session.identity_match_streak = 0

    def _get_primary_face(self, frame: np.ndarray, detection: FaceDetectionResult) -> np.ndarray | None:
        if detection.face_count != 1:
            return None
        return self.face_detector.extract_face_crop(frame, detection.boxes[0]["bbox"])

    def _apply_violation(self, violation_type: str):
        if self.session.terminated:
            return
        if self.session.decrement_life(violation_type):
            self.last_life_deducted = True
            self.last_violation_type = violation_type

    def _quality_message(self, reason: str) -> str:
        messages = {
            "FACE_TOO_SMALL": "Move closer to the camera.",
            "FACE_TOO_BLURRY": "Hold still so the face is clearer.",
            "EMPTY_FACE_CROP": "Face could not be read clearly.",
        }
        return messages.get(reason, "Face quality is too low.")

    def _calculate_risk_level(self, risk_factors: list[str]) -> str:
        if self.session.lives == 0:
            return "CRITICAL"
        if self.session.violation_count >= 2 or "IDENTITY_FAIL" in risk_factors:
            return "HIGH"
        if risk_factors:
            return "MEDIUM"
        return "LOW"

    def _terminated_response(self):
        return {
            "face_detected": False,
            "face_count": 0,
            "match_score": 0.0,
            "verification_status": "TERMINATED",
            "risk_level": "CRITICAL",
            "risk_score": 100,
            "max_lives": settings.MAX_LIVES,
            "remaining_lives": 0,
            "violation_count": self.session.violation_count,
            "session_terminated": True,
            "termination_reason": self.session.termination_reason,
            "status_messages": ["EXAM TERMINATED"],
            "life_deducted": False,
            "violation_type": None,
            "face_quality": None,
        }

    def release(self):
        self.face_detector.release()
