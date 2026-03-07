import { useAuth } from '../context/AuthContext'
import { useNavigate } from 'react-router-dom'

function Dashboard() {
  const { username, logout } = useAuth()
  const navigate = useNavigate()

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  return (
    <div className="min-h-screen bg-gray-50 p-6">
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-2xl font-bold text-gray-900">Dashboard</h1>
        <div className="flex items-center gap-4">
          <span className="text-gray-600">Hoş geldin, {username}</span>
          <button
            onClick={handleLogout}
            className="px-8 py-4 bg-red-600 text-white rounded-lg hover:bg-red-700 transition-colors"
          >
              Logout
          </button>
        </div>
      </div>
      <p className="text-gray-600">
        Ana panel - istatistikler ve özet bilgiler burada gösterilecek
      </p>
    </div>
  )
}

export default Dashboard
