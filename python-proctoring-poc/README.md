# Python Proctoring POC

This is the Python-based proctoring service for the quiz platform. It provides face detection, identity verification, live session monitoring, and a small browser UI for testing the proctoring flow.

## What is inside

- FastAPI backend
- WebSocket-based live frame processing
- Face detection with MediaPipe/OpenCV
- Identity verification with DeepFace
- Session and life tracking
- Static test UI for manual verification
- Config endpoint for frontend integration
- Pytest-based automated tests

## Main folders

- `app/` - application code
- `app/api/` - HTTP and WebSocket routes
- `app/core/` - session state and exceptions
- `app/services/` - proctoring logic
- `app/vision/` - face detection and identity verification
- `app/utils/` - helpers for logging and image conversion
- `static/` - browser UI files
- `tests/` - automated tests

## How it works

1. The browser or frontend opens a WebSocket connection to `/ws`.
2. Frames are sent as base64 images.
3. The service detects faces and checks face quality.
4. When a reference face is captured, the service stores an identity embedding.
5. Each new frame is compared against the reference face.
6. Repeated violations reduce lives and can terminate the session.
7. The frontend can also read `/api/config` to sync safe runtime values.

## Features

- Face presence monitoring
- Multiple-face detection
- Face quality checks
- Reference face capture
- Identity mismatch detection
- Life deduction and termination
- Risk/status response payloads
- Resettable proctoring session

## Prerequisites

- Python 3.10 or newer
- pip
- Camera access for browser testing

## Install

### 1. Create and activate a virtual environment

```bash
python -m venv venv
```

Windows:

```bash
venv\Scripts\activate
```

macOS/Linux:

```bash
source venv/bin/activate
```

### 2. Install dependencies

```bash
pip install -r requirements.txt
```

## Configure

This project reads settings from `.env` through `app/config.py`.

Common values include:

- `APP_NAME`
- `HOST`
- `PORT`
- face detection thresholds
- identity verification thresholds
- life and cooldown settings

Default values are already defined in `app/config.py`, so the app can run without extra environment variables.

## Run the service

### Development server

```bash
uvicorn app.main:app --reload
```

### Or run the module directly

```bash
python -m app.main
```

The service usually starts on:

- `http://localhost:8000`

## Test the UI

Open the root page in a browser after starting the server:

- `http://localhost:8000`

From the UI you can:

- grant camera permission
- capture a reference face
- stream frames for monitoring
- observe life deduction and identity verification results

## API endpoints

- `GET /` - serves the static test UI
- `GET /api/config` - returns public runtime config
- `WS /ws` - receives live frames and returns proctoring results

## Response behavior

The WebSocket response can include:

- face detection status
- face count
- identity verification status
- match score
- risk level
- remaining lives
- violation count
- session termination state
- status messages

## Example workflow

1. Open the UI in the browser
2. Allow camera access
3. Capture a reference face
4. Start sending frames
5. Watch the live status updates
6. Trigger a violation by hiding the face or showing another face
7. See lives decrease until the session terminates

## Automated tests

```bash
pytest
```

The tests cover:

- session life deduction
- session termination
- reference capture validation
- multiple-face violation streaks
- identity failure streaks
- terminated session handling

## Troubleshooting

- **Camera does not work**: allow camera permissions in the browser
- **Server does not start**: confirm the virtual environment is active and dependencies are installed
- **WebSocket not connecting**: make sure the server is running on the expected port
- **Face capture fails**: ensure exactly one face is visible and the lighting is good
- **Identity verification is unstable**: keep the face centered and avoid blur or motion

## Notes

- The app uses permissive CORS during local development.
- Static files are served from the `static/` folder.
- Runtime settings are centralized in `app/config.py`.
- The proctoring session logic lives in `app/core/session.py` and `app/services/proctoring_service.py`.

---

