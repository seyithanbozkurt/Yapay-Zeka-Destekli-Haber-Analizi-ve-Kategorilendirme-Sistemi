import { useAuth } from '../context/AuthContext'

function Profile() {
  const { username } = useAuth()

  return (
    <div className="bg-white rounded-xl shadow p-6">
      <h1 className="text-xl font-bold text-gray-900">Profil</h1>
      <p className="mt-2 text-sm text-gray-600">Kullanıcı adı: {username}</p>
    </div>
  )
}

export default Profile

