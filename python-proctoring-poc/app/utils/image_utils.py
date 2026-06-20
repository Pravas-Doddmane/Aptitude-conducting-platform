import base64
import cv2
import numpy as np

def base64_to_image(base64_str: str) -> np.ndarray:
    """Decode a base64 JPEG string into an OpenCV BGR image."""
    if base64_str.startswith("data:image/jpeg;base64,"):
        base64_str = base64_str.replace("data:image/jpeg;base64,", "")
    img_bytes = base64.b64decode(base64_str)
    np_arr = np.frombuffer(img_bytes, dtype=np.uint8)
    img = cv2.imdecode(np_arr, cv2.IMREAD_COLOR)
    return img

def image_to_base64(image: np.ndarray) -> str:
    """Encode an OpenCV BGR image to base64 JPEG string."""
    _, buffer = cv2.imencode(".jpg", image)
    b64 = base64.b64encode(buffer).decode("utf-8")
    return f"data:image/jpeg;base64,{b64}"