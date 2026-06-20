import time

import cv2
import numpy as np
from deepface import DeepFace

from app.config import settings
from app.utils.logger import logger


class IdentityVerifier:
    def __init__(self):
        self.last_check_time = 0.0
        self.latest_match_score = 0.0
        self.latest_verification_status = "PENDING"
        self.reference_embedding = None

    def reset(self):
        self.last_check_time = 0.0
        self.latest_match_score = 0.0
        self.latest_verification_status = "PENDING"
        self.reference_embedding = None

    def set_reference_embedding(self, reference_face: np.ndarray) -> bool:
        """Extract and store a reference embedding from a single-face crop."""
        embedding = self._extract_embedding(reference_face)
        if embedding is None:
            logger.error("Failed to extract reference embedding.")
            return False

        self.reference_embedding = embedding
        self.latest_verification_status = "REFERENCE_READY"
        self.latest_match_score = 1.0
        logger.info("Reference embedding extracted successfully.")
        return True

    def verify(self, current_face: np.ndarray) -> dict:
        """
        Compare a current face crop with the stored reference embedding.
        Throttles heavy checks and returns the previous result between checks.
        """
        now = time.time()
        if now - self.last_check_time < settings.IDENTITY_CHECK_INTERVAL_SEC:
            return {
                "status": self.latest_verification_status,
                "match_score": self.latest_match_score,
                "confidence": self.latest_match_score,
                "cached": True,
            }
        self.last_check_time = now

        if self.reference_embedding is None:
            return {"status": "NO_REFERENCE", "match_score": 0.0, "confidence": 0.0, "cached": False}

        current_embedding = self._extract_embedding(current_face)
        if current_embedding is None:
            self.latest_verification_status = "FACE_NOT_FOUND"
            self.latest_match_score = 0.0
            return {"status": "FACE_NOT_FOUND", "match_score": 0.0, "confidence": 0.0, "cached": False}

        cosine_similarity = self._cosine_similarity(self.reference_embedding, current_embedding)
        distance = 1.0 - cosine_similarity
        threshold = settings.DEEPFACE_SIMILARITY_THRESHOLD
        normalized_score = max(0.0, min(1.0, 1.0 - (distance / max(threshold * 2, 1e-6))))

        if distance <= threshold:
            status = "MATCHED" if normalized_score >= settings.IDENTITY_MIN_CONFIDENCE else "LOW_CONFIDENCE"
        else:
            status = "NOT_MATCHED"

        self.latest_verification_status = status
        self.latest_match_score = normalized_score
        logger.info(
            "Identity check: similarity=%.3f distance=%.3f status=%s score=%.2f",
            cosine_similarity,
            distance,
            status,
            normalized_score,
        )
        return {
            "status": status,
            "match_score": normalized_score,
            "confidence": normalized_score,
            "cached": False,
        }

    def _extract_embedding(self, face_image: np.ndarray) -> np.ndarray | None:
        if face_image is None or getattr(face_image, "size", 0) == 0:
            return None

        try:
            rgb_image = cv2.cvtColor(face_image, cv2.COLOR_BGR2RGB)
            embeddings = DeepFace.represent(
                img_path=rgb_image,
                model_name=settings.DEEPFACE_MODEL_NAME,
                detector_backend="skip",
                enforce_detection=False,
                align=True,
            )
            if embeddings:
                return np.array(embeddings[0]["embedding"], dtype=np.float32)
        except Exception as exc:
            logger.error("Identity embedding extraction failed: %s", exc)
        return None

    def _cosine_similarity(self, embedding_a: np.ndarray, embedding_b: np.ndarray) -> float:
        denominator = np.linalg.norm(embedding_a) * np.linalg.norm(embedding_b)
        if denominator <= 1e-6:
            return 0.0
        return float(np.dot(embedding_a, embedding_b) / denominator)
