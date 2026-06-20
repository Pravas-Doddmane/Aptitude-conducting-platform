import numpy as np

from app.core.session import ProctoringSession
from app.services.proctoring_service import ProctoringService
from app.vision.face_detector import FaceDetectionResult


class FakeFaceDetector:
    def __init__(self, detections=None, quality=None):
        self.detections = list(detections or [])
        self.quality = quality or {"acceptable": True, "reason": "OK", "blur_score": 90.0}

    def detect(self, frame):
        if self.detections:
            return self.detections.pop(0)
        return FaceDetectionResult(face_count=0, boxes=[], annotated_image=frame)

    def extract_face_crop(self, frame, bbox, padding_ratio=0.15):
        return np.ones((160, 160, 3), dtype=np.uint8)

    def assess_face_quality(self, face_crop, frame_shape):
        return self.quality

    def release(self):
        return None


class FakeIdentityVerifier:
    def __init__(self, results=None):
        self.results = list(results or [])
        self.reference_embedding = None

    def set_reference_embedding(self, reference_face):
        self.reference_embedding = np.array([1.0, 0.0], dtype=np.float32)
        return True

    def verify(self, current_face):
        if self.results:
            return self.results.pop(0)
        return {"status": "MATCHED", "match_score": 0.92, "confidence": 0.92, "cached": False}

    def reset(self):
        self.reference_embedding = None


def _service_with_fakes(face_detector, identity_verifier=None):
    session = ProctoringSession()
    service = ProctoringService(session)
    service.face_detector = face_detector
    service.identity_verifier = identity_verifier or FakeIdentityVerifier()
    return service, session


def test_session_life_decrement_has_cooldown():
    session = ProctoringSession()
    assert session.lives == 3
    session.decrement_life("MULTIPLE_FACES")
    assert session.lives == 2
    session.decrement_life("MULTIPLE_FACES")
    assert session.lives == 2


def test_session_termination_after_three_distinct_violations():
    session = ProctoringSession()
    session.decrement_life("A")
    session.decrement_life("B")
    session.decrement_life("C")
    assert session.lives == 0
    assert session.terminated is True
    assert session.termination_reason == "LIFE_LIMIT_EXCEEDED"


def test_reference_capture_requires_single_clear_face():
    blurry_quality = {"acceptable": False, "reason": "FACE_TOO_BLURRY", "blur_score": 5.0}
    detector = FakeFaceDetector(
        detections=[FaceDetectionResult(face_count=1, boxes=[{"bbox": (0, 0, 10, 10)}], annotated_image=np.zeros((10, 10, 3), dtype=np.uint8))],
        quality=blurry_quality,
    )
    service, session = _service_with_fakes(detector)

    success, message = service.capture_reference(np.zeros((480, 640, 3), dtype=np.uint8))

    assert success is False
    assert "Hold still" in message
    assert session.reference_image is None


def test_multiple_faces_only_deducts_after_streak():
    detections = [
        FaceDetectionResult(face_count=2, boxes=[{"bbox": (0, 0, 10, 10)}, {"bbox": (20, 20, 10, 10)}], annotated_image=np.zeros((10, 10, 3), dtype=np.uint8)),
        FaceDetectionResult(face_count=2, boxes=[{"bbox": (0, 0, 10, 10)}, {"bbox": (20, 20, 10, 10)}], annotated_image=np.zeros((10, 10, 3), dtype=np.uint8)),
    ]
    service, session = _service_with_fakes(FakeFaceDetector(detections=detections))
    session.set_reference_image(np.ones((160, 160, 3), dtype=np.uint8))
    service.identity_verifier.reference_embedding = np.array([1.0, 0.0], dtype=np.float32)

    first = service.process_frame(np.zeros((480, 640, 3), dtype=np.uint8))
    second = service.process_frame(np.zeros((480, 640, 3), dtype=np.uint8))

    assert first["remaining_lives"] == 3
    assert second["remaining_lives"] == 2
    assert second["violation_type"] == "MULTIPLE_FACES"


def test_identity_failure_streak_deducts_one_life():
    detections = [
        FaceDetectionResult(face_count=1, boxes=[{"bbox": (0, 0, 120, 120)}], annotated_image=np.zeros((10, 10, 3), dtype=np.uint8)),
        FaceDetectionResult(face_count=1, boxes=[{"bbox": (0, 0, 120, 120)}], annotated_image=np.zeros((10, 10, 3), dtype=np.uint8)),
        FaceDetectionResult(face_count=1, boxes=[{"bbox": (0, 0, 120, 120)}], annotated_image=np.zeros((10, 10, 3), dtype=np.uint8)),
    ]
    verifier = FakeIdentityVerifier(
        results=[
            {"status": "NOT_MATCHED", "match_score": 0.2, "confidence": 0.2, "cached": False},
            {"status": "NOT_MATCHED", "match_score": 0.2, "confidence": 0.2, "cached": False},
            {"status": "NOT_MATCHED", "match_score": 0.2, "confidence": 0.2, "cached": False},
        ]
    )
    service, session = _service_with_fakes(FakeFaceDetector(detections=detections), verifier)
    session.set_reference_image(np.ones((160, 160, 3), dtype=np.uint8))
    session.reference_captured_time = 0.0
    verifier.reference_embedding = np.array([1.0, 0.0], dtype=np.float32)

    service.process_frame(np.zeros((480, 640, 3), dtype=np.uint8))
    service.process_frame(np.zeros((480, 640, 3), dtype=np.uint8))
    third = service.process_frame(np.zeros((480, 640, 3), dtype=np.uint8))

    assert third["remaining_lives"] == 2
    assert third["violation_type"] == "IDENTITY_FAIL"
    assert "Identity verification failed." in third["status_messages"]


def test_terminated_session_short_circuits_processing():
    session = ProctoringSession()
    session.lives = 0
    session.terminated = True
    service = ProctoringService(session)
    service.face_detector = FakeFaceDetector()
    frame = np.zeros((480, 640, 3), dtype=np.uint8)

    resp = service.process_frame(frame)

    assert resp["session_terminated"] is True
    assert resp["verification_status"] == "TERMINATED"
