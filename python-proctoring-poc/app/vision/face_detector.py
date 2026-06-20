from dataclasses import dataclass

import cv2
import mediapipe as mp
import numpy as np

from app.config import settings
from app.utils.logger import logger

mp_face_detection = mp.solutions.face_detection


@dataclass
class FaceDetectionResult:
    face_count: int
    boxes: list[dict]
    annotated_image: np.ndarray


class FaceDetector:
    def __init__(self):
        self.face_detection = mp_face_detection.FaceDetection(
            model_selection=settings.MEDIAPIPE_FACE_DETECTION_MODEL,
            min_detection_confidence=settings.FACE_DETECTION_CONFIDENCE,
        )

    def detect(self, image: np.ndarray) -> FaceDetectionResult:
        """Detect faces in a BGR image and return sanitized face metadata."""
        annotated = image.copy()
        rgb = cv2.cvtColor(image, cv2.COLOR_BGR2RGB)
        results = self.face_detection.process(rgb)
        height, width, _ = image.shape
        boxes: list[dict] = []

        if results.detections:
            for detection in results.detections:
                bbox = detection.location_data.relative_bounding_box
                confidence = float(detection.score[0]) if detection.score else 0.0
                sanitized = self._sanitize_box(bbox, width, height)
                if sanitized is None:
                    continue

                x, y, w, h = sanitized
                boxes.append(
                    {
                        "bbox": (x, y, w, h),
                        "confidence": confidence,
                        "area": w * h,
                        "width_ratio": w / width,
                        "height_ratio": h / height,
                    }
                )
                cv2.rectangle(annotated, (x, y), (x + w, y + h), (0, 255, 0), 2)

        boxes.sort(key=lambda item: item["area"], reverse=True)
        logger.debug("Faces detected: %s", len(boxes))
        return FaceDetectionResult(face_count=len(boxes), boxes=boxes, annotated_image=annotated)

    def extract_face_crop(self, image: np.ndarray, bbox: tuple[int, int, int, int], padding_ratio: float = 0.15) -> np.ndarray:
        """Crop the dominant face with a little padding for stable verification."""
        x, y, w, h = bbox
        pad_x = int(w * padding_ratio)
        pad_y = int(h * padding_ratio)
        x1 = max(0, x - pad_x)
        y1 = max(0, y - pad_y)
        x2 = min(image.shape[1], x + w + pad_x)
        y2 = min(image.shape[0], y + h + pad_y)
        return image[y1:y2, x1:x2].copy()

    def assess_face_quality(self, face_crop: np.ndarray, frame_shape: tuple[int, int, int]) -> dict:
        """Estimate whether the face crop is large and sharp enough to trust."""
        if face_crop.size == 0:
            return {"acceptable": False, "reason": "EMPTY_FACE_CROP", "blur_score": 0.0}

        frame_height, frame_width = frame_shape[:2]
        face_height, face_width = face_crop.shape[:2]
        width_ratio = face_width / frame_width
        height_ratio = face_height / frame_height

        gray = cv2.cvtColor(face_crop, cv2.COLOR_BGR2GRAY)
        blur_score = float(cv2.Laplacian(gray, cv2.CV_64F).var())
        acceptable = (
            width_ratio >= settings.MIN_FACE_WIDTH_RATIO
            and height_ratio >= settings.MIN_FACE_HEIGHT_RATIO
            and blur_score >= settings.FACE_BLUR_LAPLACIAN_THRESHOLD
        )

        reason = "OK"
        if width_ratio < settings.MIN_FACE_WIDTH_RATIO or height_ratio < settings.MIN_FACE_HEIGHT_RATIO:
            reason = "FACE_TOO_SMALL"
        elif blur_score < settings.FACE_BLUR_LAPLACIAN_THRESHOLD:
            reason = "FACE_TOO_BLURRY"

        return {
            "acceptable": acceptable,
            "reason": reason,
            "blur_score": round(blur_score, 2),
            "width_ratio": round(width_ratio, 3),
            "height_ratio": round(height_ratio, 3),
        }

    def _sanitize_box(self, bbox, frame_width: int, frame_height: int) -> tuple[int, int, int, int] | None:
        x = max(0, int(bbox.xmin * frame_width))
        y = max(0, int(bbox.ymin * frame_height))
        w = int(bbox.width * frame_width)
        h = int(bbox.height * frame_height)
        w = min(w, frame_width - x)
        h = min(h, frame_height - y)

        if w <= 0 or h <= 0:
            logger.debug("Skipping invalid face box: %s", (x, y, w, h))
            return None

        return x, y, w, h

    def release(self):
        self.face_detection.close()
