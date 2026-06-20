import { Menu, Moon, Sun } from 'lucide-react'
import { useTheme } from '../../context/ThemeContext'

export default function Topbar({ onMenuClick }) {
  const { dark, toggleTheme } = useTheme()

  return (
    <header className="h-16 border-b bg-white dark:bg-gray-800 dark:border-gray-700 flex items-center justify-between px-4 sm:px-6 lg:px-8">
      <button
        onClick={onMenuClick}
        className="lg:hidden p-2 rounded-md hover:bg-gray-100 dark:hover:bg-gray-700"
      >
        <Menu className="w-5 h-5" />
      </button>
      <div className="flex-1 lg:ml-0 ml-4">
        {/* You can add breadcrumbs or page title here if needed */}
      </div>
      <button
        onClick={toggleTheme}
        className="p-2 rounded-md hover:bg-gray-100 dark:hover:bg-gray-700"
        aria-label="Toggle dark mode"
      >
        {dark ? (
          <Sun className="w-5 h-5" />
        ) : (
          <Moon className="w-5 h-5" />
        )}
      </button>
    </header>
  )
}
