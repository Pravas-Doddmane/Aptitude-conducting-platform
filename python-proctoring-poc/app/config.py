from pydantic import ConfigDict
from pydantic_settings import BaseSettings

class Settings(BaseSettings):
    model_config = ConfigDict(env_file=".env", env_file_encoding="utf-8")

    APP_NAME: str = "Proctoring POC"
    HOST: str = "0.0.0.0"
    PORT: int = 8000

    # Face Detection
    MEDIAPIPE_FACE_DETECTION_MODEL: int = 0        # 0 = close range, 1 = far range
    FACE_DETECTION_CONFIDENCE: float = 0.8          # increased for stability
    MIN_FACE_WIDTH_RATIO: float = 0.12
    MIN_FACE_HEIGHT_RATIO: float = 0.12
    FACE_BLUR_LAPLACIAN_THRESHOLD: float = 45.0

    # Identity Verification
    DEEPFACE_MODEL_NAME: str = "Facenet512"         # more accurate than Facenet
    DEEPFACE_DETECTOR_BACKEND: str = "mediapipe"
    DEEPFACE_SIMILARITY_THRESHOLD: float = 0.35     # lower = stricter (Facenet512 typical 0.30)
    IDENTITY_CHECK_INTERVAL_SEC: float = 2.0         # check every 2 seconds
    IDENTITY_GRACE_PERIOD_SEC: float = 5.0           # no checks after reference capture
    IDENTITY_FAILURE_STREAK_NEEDED: int = 3          # 3 consecutive failures before life loss
    IDENTITY_MATCH_CONFIRMATIONS: int = 2
    IDENTITY_MIN_CONFIDENCE: float = 0.55

    # Violation Cooldowns
    VIOLATION_COOLDOWN_SEC: float = 8.0             # longer cooldown (was 5s)
    FACE_MISSING_COOLDOWN_SEC: float = 3.0          # face can be out for 3s before losing life
    FACE_MISSING_STREAK_NEEDED: int = 3
    MULTIPLE_FACE_STREAK_NEEDED: int = 2

    # Lives
    MAX_LIVES: int = 3

settings = Settings()
