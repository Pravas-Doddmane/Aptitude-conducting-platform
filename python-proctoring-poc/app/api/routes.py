from fastapi import APIRouter
from fastapi.responses import FileResponse
from app.config import settings
import os

router = APIRouter()

@router.get("/")
async def read_root():
    return FileResponse(os.path.join("static", "index.html"))

@router.get("/api/config")
async def get_config():
    """Public proctoring configuration sourced from .env (no hardcoding in frontend)."""
    return {
        "max_lives": settings.MAX_LIVES,
        "identity_check_interval_sec": settings.IDENTITY_CHECK_INTERVAL_SEC,
        "identity_grace_period_sec": settings.IDENTITY_GRACE_PERIOD_SEC,
        "identity_failure_streak_needed": settings.IDENTITY_FAILURE_STREAK_NEEDED,
        "violation_cooldown_sec": settings.VIOLATION_COOLDOWN_SEC,
        "face_missing_cooldown_sec": settings.FACE_MISSING_COOLDOWN_SEC,
        "face_detection_confidence": settings.FACE_DETECTION_CONFIDENCE,
        "deepface_similarity_threshold": settings.DEEPFACE_SIMILARITY_THRESHOLD,
    }