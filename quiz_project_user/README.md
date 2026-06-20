# Quiz Project User Frontend

This is the student/user-facing web app for the Quiz Platform. It is built with React + Vite and connects to the `QuizeBackend` API for authentication, quiz access, quiz attempts, results, certificates, feedback, and proctoring.

## What is inside

- React 19 + Vite
- React Router for page navigation
- React Query for API state
- Axios-based API layer with token refresh
- Tailwind CSS for styling
- Proctoring UI components
- Identity verification support
- Quiz attempt and result screens

## Main pages

- Login and register
- Forgot password and reset password
- Dashboard
- Quiz details
- Quiz attempt screen
- Attempt result screen
- History

## Main folders

- `src/api/` - API service files for backend calls
- `src/components/` - reusable UI, layout, quiz, and proctoring components
- `src/context/` - auth and theme providers
- `src/hooks/` - custom hooks such as fullscreen and proctoring helpers
- `src/pages/` - route-level pages
- `src/routes/` - app routing
- `src/utils/` - helper functions and proctoring sync utilities

## How it works

1. The app loads in the browser and reads API URLs from environment variables.
2. Users log in or register.
3. The app stores access and refresh tokens in `localStorage`.
4. Protected routes redirect unauthenticated users to `/login`.
5. Quiz pages call the backend through the API layer in `src/api/`.
6. During a quiz attempt, the app can show proctoring controls, reference capture, fullscreen handling, and live status updates.
7. Result and history pages fetch attempt data from the backend after submission.

## Prerequisites

- Node.js 18+ recommended
- npm
- `QuizeBackend` running locally or on a reachable server

## Required environment variables

Create a `.env` file in `quiz_project_user/` with the backend URLs:

```env
VITE_API_BASE_URL=http://localhost:8081
VITE_PROCTORING_API_URL=http://localhost:8081
VITE_PROCTORING_WS_URL=ws://localhost:8081
```

If your backend runs on a different host or port, update these values.

## Open and run

### In VS Code or another editor

1. Open the `quiz_project_user` folder
2. Install dependencies
3. Make sure the `.env` file is configured
4. Start the dev server

### From terminal

```bash
cd "C:\Users\prava\Java Developer Projects\QuizApp Complex Project\quiz_project_user"
npm install
npm run dev
```

The app usually runs on `http://localhost:5173`.

## Build for production

```bash
npm run build
```

Preview the production build:

```bash
npm run preview
```

## Project flow

- Open the app
- Log in or create an account
- Browse available quizzes from the dashboard
- Open a quiz to view details and requirements
- Start the attempt screen when ready
- Complete proctoring checks if enabled
- Submit the quiz and review the result page
- Check history, leaderboard, and profile afterward

## Dependencies worth knowing

- `react-router-dom` - routing
- `@tanstack/react-query` - server-state management
- `axios` - HTTP requests
- `react-hot-toast` - notifications
- `react-hook-form` and `zod` - form handling and validation
- `lucide-react` - icons

## Troubleshooting

- **Blank page or API errors**: confirm `VITE_API_BASE_URL` is correct
- **Login not working**: check the backend is running and CORS is allowed
- **Quiz attempt issues**: ensure the backend proctoring and attempt APIs are available
- **Fullscreen or camera problems**: allow browser permissions for the quiz page
- **Refresh keeps failing**: clear `localStorage` and log in again

## Notes

- `src/api/axios.js` automatically attaches the access token to requests.
- If a `401` happens, the app tries to refresh the session using the refresh token.
- The quiz attempt screen is designed to run without the normal layout because it uses fullscreen mode.
- The app expects the backend to be available before you try quiz or proctoring flows.

---
