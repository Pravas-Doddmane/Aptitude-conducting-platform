# Quiz Project Admin Frontend

This is the admin dashboard for the Quiz Platform. It is built with React + Vite and connects to `QuizeBackend` for quiz management, categories, questions, results, feedback, users, and certificate templates.

## What is inside

- React 19 + Vite
- Admin login and protected dashboard routing
- Axios-based API layer with token refresh
- React Query for server state
- Tailwind CSS styling
- Admin layout with sidebar and topbar
- Dashboard, quiz, question, user, feedback, and result management screens

## Main pages

- Login
- Dashboard
- Categories
- Quizzes
- Questions
- Results
- Certificate templates
- Feedback
- Users

## Main folders

- `src/api/` - backend service calls
- `src/components/layout/` - admin shell with sidebar and topbar
- `src/components/ui/` - reusable UI controls
- `src/context/` - auth state
- `src/hooks/` - auth helpers and toast helpers
- `src/pages/` - admin pages
- `src/routes/` - route definitions and access control
- `src/utils/` - constants and helper functions

## How it works

1. The app loads `VITE_API_BASE_URL` from the environment.
2. Admin users log in through the backend auth API.
3. The app stores the refresh token in `localStorage` and keeps the access token in memory.
4. Protected routes block unauthenticated users and send them to `/login`.
5. The admin layout shows the sidebar and topbar once logged in.
6. Each page talks to the backend through the API layer for CRUD actions and reporting.
7. If a request returns `401`, the app tries to refresh the session automatically.

## Prerequisites

- Node.js 18+ recommended
- npm
- `QuizeBackend` running locally or on a reachable server

## Required environment variables

Create a `.env` file in `quiz_project_admin/`:

```env
VITE_API_BASE_URL=http://localhost:8081
```

If your backend is hosted elsewhere, replace the URL with your backend address.

## Open and run

### In your editor

1. Open the `quiz_project_admin` folder
2. Install dependencies
3. Make sure the `.env` file is set
4. Start the frontend

### From terminal

```bash
cd "C:\Users\prava\Java Developer Projects\QuizApp Complex Project\quiz_project_admin"
npm install
npm run dev
```

The app usually runs on `http://localhost:5173`.

## Build for production

```bash
npm run build
```

Preview the build:

```bash
npm run preview
```

## Project flow

- Open the admin app
- Log in with admin credentials
- View the dashboard summary
- Manage categories, quizzes, and questions
- Review results and feedback
- Manage users and certificate templates
- Log out when finished

## Dependencies worth knowing

- `react-router-dom` - page routing
- `@tanstack/react-query` - API data caching
- `axios` - HTTP requests
- `react-hot-toast` - notifications
- `react-hook-form` and `zod` - forms and validation
- `lucide-react` - icons

## Troubleshooting

- **Login fails**: check `VITE_API_BASE_URL` and make sure the backend is running
- **Blank dashboard**: confirm the backend returns admin data successfully
- **Session keeps expiring**: clear browser storage and log in again
- **API errors**: verify CORS is enabled in the backend for the admin frontend origin

## Notes

- The admin app uses protected routes, so the dashboard is not visible until login succeeds.
- `src/api/axios.js` automatically attaches the access token to requests.
- When the backend returns `401`, the app attempts token refresh before logging out.
- The dashboard layout is handled by `src/components/layout/AdminLayout.jsx`.

---
