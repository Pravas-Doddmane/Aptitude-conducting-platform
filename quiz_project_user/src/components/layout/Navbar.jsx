import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { useTheme } from '../../context/ThemeContext';
import { Sun, Moon, LogOut, User, Menu, X, LayoutDashboard, History, Trophy } from 'lucide-react';
import { useState } from 'react';

export default function Navbar() {
  const { user, logout, isAuthenticated } = useAuth();
  const { dark, toggleTheme } = useTheme();
  const navigate = useNavigate();
  const [mobileOpen, setMobileOpen] = useState(false);

  const handleLogout = async () => {
    await logout();
    navigate('/login');
  };

  return (
    <nav className="sticky top-0 z-40 bg-white/80 dark:bg-gray-900/80 backdrop-blur-md border-b dark:border-gray-800 shadow-sm">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex justify-between h-16 items-center">
          <Link to="/" className="flex items-center gap-3">
            <img src="/knowledge.png" alt="QuizMaster Pro" className="h-10 w-10" />
            <span className="text-2xl font-bold bg-gradient-to-r from-brand-600 to-purple-600 bg-clip-text text-transparent">
              QuizMaster Pro
            </span>
          </Link>

          {/* Desktop */}
          <div className="hidden md:flex items-center gap-4">
            {isAuthenticated && (
              <>
                <Link to="/dashboard" className="nav-link"><LayoutDashboard className="w-4 h-4" /> Dashboard</Link>
                <Link to="/history" className="nav-link"><History className="w-4 h-4" /> History</Link>
                <Link to="/leaderboard" className="nav-link"><Trophy className="w-4 h-4" /> Leaderboard</Link>
                <Link to="/profile" className="nav-link"><User className="w-4 h-4" /> Profile</Link>
              </>
            )}
            <button onClick={toggleTheme} className="p-2 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-800">
              {dark ? <Sun className="w-5 h-5" /> : <Moon className="w-5 h-5" />}
            </button>
            {isAuthenticated && (
              <button onClick={handleLogout} className="p-2 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-800 text-red-500">
                <LogOut className="w-5 h-5" />
              </button>
            )}
          </div>

          {/* Mobile toggle */}
          <div className="md:hidden flex items-center gap-2">
            <button onClick={toggleTheme} className="p-2 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-800">
              {dark ? <Sun className="w-5 h-5" /> : <Moon className="w-5 h-5" />}
            </button>
            <button onClick={() => setMobileOpen(!mobileOpen)} className="p-2">
              {mobileOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
            </button>
          </div>
        </div>
      </div>

      {/* Mobile menu */}
      {mobileOpen && (
        <div className="md:hidden bg-white dark:bg-gray-900 border-t dark:border-gray-800">
          <div className="px-4 py-3 space-y-2">
            {isAuthenticated ? (
              <>
                <Link to="/dashboard" onClick={() => setMobileOpen(false)} className="block py-2"><LayoutDashboard className="inline w-4 h-4 mr-2" />Dashboard</Link>
                <Link to="/history" onClick={() => setMobileOpen(false)} className="block py-2"><History className="inline w-4 h-4 mr-2" />History</Link>
                <Link to="/leaderboard" onClick={() => setMobileOpen(false)} className="block py-2"><Trophy className="inline w-4 h-4 mr-2" />Leaderboard</Link>
                <Link to="/profile" onClick={() => setMobileOpen(false)} className="block py-2"><User className="inline w-4 h-4 mr-2" />Profile</Link>
                <button onClick={() => { handleLogout(); setMobileOpen(false); }} className="block w-full text-left py-2 text-red-500"><LogOut className="inline w-4 h-4 mr-2" />Logout</button>
              </>
            ) : (
              <Link to="/login" onClick={() => setMobileOpen(false)} className="block py-2">Login</Link>
            )}
          </div>
        </div>
      )}
    </nav>
  );
}