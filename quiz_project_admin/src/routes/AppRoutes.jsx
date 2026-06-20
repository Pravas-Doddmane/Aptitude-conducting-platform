import { Routes, Route, Navigate } from 'react-router-dom'
import { AdminLayout } from '../components/layout/AdminLayout'
import { ProtectedRoute } from './ProtectedRoute'
import { Login } from '../pages/Login'
import { Dashboard } from '../pages/Dashboard'
import { Categories } from '../pages/Categories'
import { Quizzes } from '../pages/Quizzes'
import { Questions } from '../pages/Questions'
import { Users } from '../pages/Users'
import { Feedback } from '../pages/Feedback'
import { Results } from '../pages/Results'
import { CertificateTemplates } from '../pages/CertificateTemplates'

const AppRoutes = () => {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route
        element={
          <ProtectedRoute>
            <AdminLayout />
          </ProtectedRoute>
        }
      >
        <Route index element={<Dashboard />} />
        <Route path="categories" element={<Categories />} />
        <Route path="quizzes" element={<Quizzes />} />
        <Route path="questions" element={<Questions />} />
        <Route path="results" element={<Results />} />
        <Route path="certificate-templates" element={<CertificateTemplates />} />
        <Route path="feedback" element={<Feedback />} />
        <Route path="users" element={<Users />} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}

export default AppRoutes
