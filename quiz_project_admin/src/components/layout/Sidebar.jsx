import { NavLink, useLocation } from 'react-router-dom'
import {
  LayoutDashboard,
  BarChart3,
  FolderTree,
  FileQuestion,
  MessageSquare,
  Users,
  Mail,
  Award,
  Settings,
  LogOut,
  X,
} from 'lucide-react'
import { useAuth } from '../../hooks/useAuth'

const navItems = [
  { to: '/', icon: LayoutDashboard, label: 'Dashboard' },
  { to: '/categories', icon: FolderTree, label: 'Categories' },
  { to: '/quizzes', icon: FileQuestion, label: 'Quizzes' },
  { to: '/questions', icon: MessageSquare, label: 'Questions' },
  { to: '/results', icon: BarChart3, label: 'Results' },
  { to: '/certificate-templates', icon: Award, label: 'Certificates' },
  { to: '/feedback', icon: Mail, label: 'Feedback' },
  { to: '/users', icon: Users, label: 'Users' },
]

export const Sidebar = ({ mobileOpen, setMobileOpen }) => {
  const { logout } = useAuth()
  const location = useLocation()

  const handleLogout = async () => {
    await logout()
  }

  return (
    <>
      {/* Mobile backdrop */}
      {mobileOpen && (
        <div
          className="fixed inset-0 z-40 bg-black/50 lg:hidden"
          onClick={() => setMobileOpen(false)}
        />
      )}
      <aside
        className={`fixed top-0 left-0 z-50 h-full w-64 bg-slate-900 text-white transform transition-transform duration-200 ease-in-out lg:translate-x-0 ${mobileOpen ? 'translate-x-0' : '-translate-x-full'} lg:static lg:z-auto`}
      >
        <div className="flex items-center justify-between h-16 px-6 border-b border-slate-700/50">
          <span className="text-xl font-bold tracking-tight bg-gradient-to-r from-indigo-400 to-cyan-400 bg-clip-text text-transparent">
            QuizAdmin
          </span>
          <button className="lg:hidden p-1 rounded hover:bg-slate-700" onClick={() => setMobileOpen(false)}>
            <X className="w-5 h-5" />
          </button>
        </div>
        <nav className="mt-6 px-3 space-y-1">
          {navItems.map((item) => {
            const isActive = location.pathname === item.to
            return (
              <NavLink
                key={item.to}
                to={item.to}
                onClick={() => setMobileOpen(false)}
                className={`flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-colors ${
                  isActive
                    ? 'bg-white/10 text-white'
                    : 'text-slate-300 hover:bg-white/5 hover:text-white'
                }`}
              >
                <item.icon className="w-5 h-5" />
                {item.label}
              </NavLink>
            )
          })}
        </nav>
        <div className="absolute bottom-0 left-0 right-0 p-4 border-t border-slate-700/50">
          <button
            onClick={handleLogout}
            className="flex items-center gap-3 w-full px-3 py-2.5 text-sm font-medium text-slate-300 hover:bg-white/5 hover:text-white rounded-lg transition-colors"
          >
            <LogOut className="w-5 h-5" />
            Logout
          </button>
        </div>
      </aside>
    </>
  )
}
