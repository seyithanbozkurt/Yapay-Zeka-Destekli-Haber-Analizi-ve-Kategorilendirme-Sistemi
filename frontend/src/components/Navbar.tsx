import { NavLink, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

function linkClass(isActive: boolean) {
  return `px-3 py-2 rounded-lg text-sm font-medium transition-colors ${
    isActive ? 'bg-blue-100 text-blue-700' : 'text-gray-700 hover:bg-gray-100'
  }`
}

function Navbar() {
  const navigate = useNavigate()
  const { username, logout } = useAuth()

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  return (
    <header className="bg-white border-b border-gray-200 sticky top-0 z-20">
      <div className="max-w-7xl mx-auto px-4 py-3 flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
        <div className="flex items-center gap-3">
          <div className="h-9 w-9 rounded-lg bg-blue-600 text-white grid place-items-center font-bold">
            H
          </div>
          <div>
            <p className="text-sm font-semibold text-gray-900">Haber Analiz</p>
            <p className="text-xs text-gray-500">AI Destekli Haber Platformu</p>
          </div>
        </div>

        <nav className="flex items-center gap-2 overflow-x-auto">
          <NavLink to="/home" className={({ isActive }) => linkClass(isActive)}>
            Anasayfa
          </NavLink>
          <NavLink to="/news" className={({ isActive }) => linkClass(isActive)}>
            Haberler
          </NavLink>
          <NavLink to="/categories" className={({ isActive }) => linkClass(isActive)}>
            Kategoriler
          </NavLink>
          <NavLink to="/feedbacks" className={({ isActive }) => linkClass(isActive)}>
            Geri Bildirimler
          </NavLink>
          <NavLink to="/profile" className={({ isActive }) => linkClass(isActive)}>
            Profil
          </NavLink>
        </nav>

        <div className="flex items-center gap-3">
          <span className="text-sm text-gray-600">Merhaba, {username}</span>
          <button
            onClick={handleLogout}
            className="px-3 py-2 rounded-lg text-sm font-medium bg-red-600 text-white hover:bg-red-700 transition-colors"
          >
            Çıkış
          </button>
        </div>
      </div>
    </header>
  )
}

export default Navbar

