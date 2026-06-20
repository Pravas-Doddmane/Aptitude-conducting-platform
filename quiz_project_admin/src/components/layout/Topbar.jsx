import { Menu } from 'lucide-react'

export const Topbar = ({ onMenuClick }) => {
  return (
    <header className="sticky top-0 z-30 flex items-center justify-between h-16 px-4 sm:px-6 bg-white/80 backdrop-blur-md border-b border-gray-200/60">
      <button onClick={onMenuClick} className="lg:hidden p-2 -ml-2 rounded-lg hover:bg-gray-100">
        <Menu className="w-5 h-5 text-gray-600" />
      </button>
    </header>
  )
}
