const WS_URL = `ws://${window.location.host}/ws`;
let ws;
let video;
let canvas;
let streaming = false;
let captureReferenceMode = false;

const banner = document.getElementById('warning-banner');
const bannerMsg = document.getElementById('banner-message');
const heartsContainer = document.getElementById('hearts-container');
const faceCountOverlay = document.getElementById('face-count');
const matchScoreOverlay = document.getElementById('match-score');
const riskLevelOverlay = document.getElementById('risk-level');
const verificationStatusOverlay = document.getElementById('verification-status');

const faceDetectedSpan = document.getElementById('face-detected');
const statusFaceCountSpan = document.getElementById('status-face-count');
const statusMatchScoreSpan = document.getElementById('status-match-score');
const statusVerificationSpan = document.getElementById('status-verification');
const riskScoreSpan = document.getElementById('risk-score');
const remainingLivesSpan = document.getElementById('remaining-lives');
const violationCountSpan = document.getElementById('violation-count');
const sessionStateSpan = document.getElementById('session-state');

const captureBtn = document.getElementById('capture-reference');
const captureStatus = document.getElementById('capture-status');

function init() {
    video = document.getElementById('video');
    canvas = document.getElementById('canvas');
    canvas.width = 640;
    canvas.height = 480;

    navigator.mediaDevices.getUserMedia({ video: { width: 640, height: 480 } })
        .then(stream => {
            video.srcObject = stream;
            video.play();
            streaming = true;
            connectWebSocket();
        })
        .catch(() => {
            bannerMsg.textContent = 'Camera access denied!';
            banner.className = 'banner violation';
        });

    captureBtn.addEventListener('click', () => {
        captureReferenceMode = true;
    });
}

function connectWebSocket() {
    ws = new WebSocket(WS_URL);
    ws.onopen = () => {
        console.log('WebSocket connected');
        sendFrame();
    };
    ws.onmessage = (event) => {
        const data = JSON.parse(event.data);
        handleServerMessage(data);
    };
    ws.onclose = () => {
        console.log('WebSocket disconnected');
        streaming = false;
    };
    ws.onerror = (err) => console.error('WebSocket error', err);
}

function sendFrame() {
    if (!streaming) return;

    const ctx = canvas.getContext('2d');
    ctx.drawImage(video, 0, 0, canvas.width, canvas.height);
    const frameBase64 = canvas.toDataURL('image/jpeg', 0.8);

    if (ws.readyState === WebSocket.OPEN) {
        const payload = { frame: frameBase64 };
        if (captureReferenceMode) {
            payload.action = 'capture_reference';
            captureReferenceMode = false;
        }
        ws.send(JSON.stringify(payload));
    }

    setTimeout(sendFrame, 200);
}

function handleServerMessage(data) {
    if (data.action === 'reference_captured') {
        captureStatus.textContent = data.message || (data.success ? 'Reference face captured successfully!' : 'Failed to capture reference.');
        setTimeout(() => {
            captureStatus.textContent = '';
        }, 3000);
        return;
    }

    if (data.action === 'session_reset') {
        captureStatus.textContent = 'Session reset successfully.';
        setTimeout(() => {
            captureStatus.textContent = '';
        }, 3000);
        return;
    }

    const messages = data.status_messages || [];
    if (messages.length > 0) {
        bannerMsg.textContent = messages.join(' | ');
        if (data.session_terminated || data.risk_level === 'HIGH' || data.risk_level === 'CRITICAL') {
            banner.className = 'banner violation';
        } else if (data.risk_level === 'MEDIUM') {
            banner.className = 'banner warning';
        } else {
            banner.className = 'banner normal';
        }
    } else {
        bannerMsg.textContent = 'Monitoring...';
        banner.className = 'banner normal';
    }

    const remainingLives = data.remaining_lives !== undefined ? data.remaining_lives : 3;
    const maxLives = data.max_lives || 3;
    let heartsHtml = '';
    for (let i = 0; i < maxLives; i++) {
        heartsHtml += i < remainingLives ? '&#10084; ' : '&#9825; ';
    }
    heartsContainer.innerHTML = heartsHtml.trim();

    faceCountOverlay.textContent = data.face_count;
    matchScoreOverlay.textContent = typeof data.match_score === 'number' ? data.match_score.toFixed(2) : 'N/A';
    riskLevelOverlay.textContent = data.risk_level;
    verificationStatusOverlay.textContent = data.verification_status;

    faceDetectedSpan.textContent = data.face_detected ? 'Yes' : 'No';
    statusFaceCountSpan.textContent = data.face_count;
    statusMatchScoreSpan.textContent = typeof data.match_score === 'number' ? data.match_score.toFixed(2) : '0';
    statusVerificationSpan.textContent = data.verification_status;
    riskScoreSpan.textContent = data.risk_score;
    remainingLivesSpan.textContent = `${remainingLives}/${maxLives}`;
    violationCountSpan.textContent = data.violation_count;
    sessionStateSpan.textContent = data.session_terminated ? 'TERMINATED' : 'ACTIVE';

    if (data.session_terminated) {
        streaming = false;
        bannerMsg.textContent = `EXAM TERMINATED - ${data.termination_reason || ''}`;
        banner.className = 'banner violation';
    }
}

window.onload = init;
