import { Routes, Route, Navigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import Layout from '../components/layout/Layout';
import Login from '../pages/Login';
import Register from '../pages/Register';
import ForgotPassword from '../pages/ForgotPassword';
import ResetPassword from '../pages/ResetPassword';
import Dashboard from '../pages/Dashboard';
import QuizDetails from '../pages/QuizDetails';
import QuizAttempt from '../pages/QuizAttempt';
import AttemptResult from '../pages/AttemptResult';
import Profile from '../pages/Profile';
import History from '../pages/History';
import Leaderboard from '../pages/Leaderboard';

const ProtectedRoute = ({ children }) => {
  const { isAuthenticated, loading } = useAuth();
  if (loading) return <div className="p-8 text-center">Loading...</div>;
  return isAuthenticated ? children : <Navigate to="/login" replace />;
};

export default function AppRoutes() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route path="/register" element={<Register />} />
      <Route path="/forgot-password" element={<ForgotPassword />} />
      <Route path="/reset-password" element={<ResetPassword />} />

      <Route path="/" element={<Layout><Navigate to="/dashboard" replace /></Layout>} />
      <Route path="/dashboard" element={<Layout><ProtectedRoute><Dashboard /></ProtectedRoute></Layout>} />
      <Route path="/quizzes/:quizId" element={<Layout><ProtectedRoute><QuizDetails /></ProtectedRoute></Layout>} />
      <Route path="/attempts/:attemptId/play" element={<ProtectedRoute><QuizAttempt /></ProtectedRoute>} /> {/* no Layout because fullscreen */}
      <Route path="/attempts/:attemptId/result" element={<Layout><ProtectedRoute><AttemptResult /></ProtectedRoute></Layout>} />
      <Route path="/profile" element={<Layout><ProtectedRoute><Profile /></ProtectedRoute></Layout>} />
      <Route path="/history" element={<Layout><ProtectedRoute><History /></ProtectedRoute></Layout>} />
      <Route path="/leaderboard" element={<Layout><ProtectedRoute><Leaderboard /></ProtectedRoute></Layout>} />
      <Route path="*" element={<Layout><div className="text-center py-20"><h2 className="text-3xl font-bold">404</h2><p>Page not found</p></div></Layout>} />
    </Routes>
  );
}