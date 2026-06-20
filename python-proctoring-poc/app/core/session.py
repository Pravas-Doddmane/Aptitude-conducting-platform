from dataclasses import dataclass, field
from typing import Optional
import time
import numpy as np
from app.config import settings
from app.utils.logger import logger

@dataclass
class ProctoringSession:
    reference_image: Optional[np.ndarray] = None
    lives: int = settings.MAX_LIVES
    violation_count: int = 0
    terminated: bool = False
    termination_reason: Optional[str] = None
    last_violation_time: dict = field(default_factory=dict)

    # New fields for streak logic
    identity_failure_streak: int = 0
    identity_match_streak: int = 0
    missing_face_streak: int = 0
    multiple_face_streak: int = 0
    last_identity_check_time: float = 0.0
    reference_captured_time: float = 0.0   # when reference was stored

    def reset_lives(self):
        self.lives = settings.MAX_LIVES
        self.violation_count = 0
        self.terminated = False
        self.termination_reason = None
        self.identity_failure_streak = 0
        self.identity_match_streak = 0
        self.missing_face_streak = 0
        self.multiple_face_streak = 0
        self.reference_captured_time = 0.0
        self.last_identity_check_time = 0.0
        self.last_violation_time.clear()

    def decrement_life(self, violation_type: str) -> bool:
        """Decrement a life if not in cooldown. Returns True if life was lost."""
        now = time.time()
        cooldown = settings.VIOLATION_COOLDOWN_SEC
        if violation_type == "FACE_MISSING":
            cooldown = settings.FACE_MISSING_COOLDOWN_SEC

        if violation_type in self.last_violation_time:
            if now - self.last_violation_time[violation_type] < cooldown:
                return False  # still in cooldown
        self.last_violation_time[violation_type] = now

        if self.lives > 0:
            self.lives -= 1
            self.violation_count += 1
            logger.warning(f"Violation: {violation_type}, lives left: {self.lives}")
            if self.lives == 0:
                self.terminated = True
                self.termination_reason = "LIFE_LIMIT_EXCEEDED"
            return True
        return False

    def set_reference_image(self, image: np.ndarray):
        self.reference_image = image
        self.reference_captured_time = time.time()
        # Reset identity failure streak on new reference
        self.identity_failure_streak = 0
        self.identity_match_streak = 0
