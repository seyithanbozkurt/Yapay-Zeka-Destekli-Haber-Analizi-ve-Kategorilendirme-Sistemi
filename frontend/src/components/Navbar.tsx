import { useEffect, useRef, useState } from 'react'
import { NavLink, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { fetchAllCategories } from '../services/categoryService'
import type { Category } from '../types/category'

function linkClass(isActive: boolean) {
  return `px-3 py-2 rounded-lg text-sm font-medium transition-colors ${
    isActive ? 'bg-blue-100 text-blue-700' : 'text-gray-700 hover:bg-gray-100'
  }`
}

function Navbar() {
  const navigate = useNavigate()
  const { username, logout } = useAuth()
  const [categories, setCategories] = useState<Category[]>([])
  const [isCategoryMenuOpen, setIsCategoryMenuOpen] = useState(false)
  const categoryMenuRef = useRef<HTMLDivElement | null>(null)

  useEffect(() => {
    let isMounted = true
    const loadCategories = async () => {
      try {
        const data = await fetchAllCategories()
        if (!isMounted) return
        setCategories(data.filter((item) => item.active))
      } catch {
        if (!isMounted) return
        setCategories([])
      }
    }
    loadCategories()
    return () => {
      isMounted = false
    }
  }, [])

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (!categoryMenuRef.current) return
      if (!categoryMenuRef.current.contains(event.target as Node)) {
        setIsCategoryMenuOpen(false)
      }
    }

    document.addEventListener('mousedown', handleClickOutside)
    return () => {
      document.removeEventListener('mousedown', handleClickOutside)
    }
  }, [])

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

        <nav className="flex items-center gap-2 overflow-visible relative">
          <NavLink to="/home" className={({ isActive }) => linkClass(isActive)}>
            Anasayfa
          </NavLink>
          <NavLink to="/news" className={({ isActive }) => linkClass(isActive)}>
            Haberler
          </NavLink>
          <div className="relative" ref={categoryMenuRef}>
            <button
              type="button"
              onClick={() => setIsCategoryMenuOpen((prev) => !prev)}
              className="px-3 py-2 rounded-lg text-sm font-medium text-gray-700 hover:bg-gray-100 transition-colors"
            >
              Kategoriler ▾
            </button>
            {isCategoryMenuOpen && (
              <div className="absolute left-0 top-full mt-1 min-w-[220px] max-h-72 overflow-y-auto rounded-xl border border-gray-200 bg-white shadow-lg z-30">
              <button
                type="button"
                onClick={() => {
                  setIsCategoryMenuOpen(false)
                  navigate('/news')
                }}
                className="w-full text-left px-4 py-2.5 text-sm text-gray-700 hover:bg-gray-100"
              >
                Tum Kategoriler
              </button>
              {categories.map((category) => (
                <button
                  key={category.id}
                  type="button"
                  onClick={() => {
                    setIsCategoryMenuOpen(false)
                    navigate(`/news?category=${encodeURIComponent(category.name)}`)
                  }}
                  className="w-full text-left px-4 py-2.5 text-sm text-gray-700 hover:bg-gray-100"
                >
                  {category.name}
                </button>
              ))}
              </div>
            )}
          </div>
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

