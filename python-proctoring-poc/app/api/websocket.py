import json
from fastapi import APIRouter, WebSocket, WebSocketDisconnect
from app.services.proctoring_service import ProctoringService
from app.utils.image_utils import base64_to_image
from app.utils.logger import logger
from app.core.session import ProctoringSession

router = APIRouter()

@router.websocket("/ws")
async def websocket_endpoint(websocket: WebSocket):
    await websocket.accept()
    session = ProctoringSession()
    service = ProctoringService(session)
    try:
        while True:
            data = await websocket.receive_text()
            try:
                frame_data = json.loads(data)
            except json.JSONDecodeError:
                frame_data = {"frame": data}

            action = frame_data.get("action")

            if action == "reset_session":
                service.reset_session()
                await websocket.send_text(json.dumps({
                    "action": "session_reset",
                    "success": True,
                    "max_lives": session.lives,
                    "remaining_lives": session.lives,
                }))
                continue

            frame_b64 = frame_data.get("frame")
            if frame_b64 is None:
                continue
            frame = base64_to_image(frame_b64)
            if frame is None:
                continue

            # Handle reference capture command
            if action == "capture_reference":
                success, message = service.capture_reference(frame)
                response = {
                    "action": "reference_captured",
                    "success": success,
                    "message": message,
                    "max_lives": session.lives,
                    "remaining_lives": session.lives,
                }
                await websocket.send_text(json.dumps(response))
                continue

            # Normal processing
            result = service.process_frame(frame)
            await websocket.send_text(json.dumps(result))
    except WebSocketDisconnect:
        logger.info("WebSocket disconnected")
        service.release()
    except Exception as e:
        logger.error(f"WebSocket error: {e}")
        service.release()
