import { useTheme } from '../context/ThemeContext'

function Footer() {
  const { theme } = useTheme()
  const isDark = theme === 'dark'

  return (
    <footer className={`mt-10 border-t ${isDark ? 'bg-slate-900 border-slate-700' : 'bg-white border-gray-200'}`}>
      <div className="max-w-7xl mx-auto px-4 py-8 grid grid-cols-1 md:grid-cols-3 gap-6">
        <div>
          <h4 className={`text-sm font-semibold ${isDark ? 'text-slate-100' : 'text-gray-900'}`}>Haber Analiz</h4>
          <p className={`mt-2 text-sm ${isDark ? 'text-slate-300' : 'text-gray-600'}`}>
            Yapay zeka destekli haber sınıflandırma ve kullanıcı geri bildirimi platformu.
          </p>
        </div>
        <div>
          <h4 className={`text-sm font-semibold ${isDark ? 'text-slate-100' : 'text-gray-900'}`}>İletişim</h4>
          <p className={`mt-2 text-sm ${isDark ? 'text-slate-300' : 'text-gray-600'}`}>info@haberanaliz.com</p>
          <p className={`text-sm ${isDark ? 'text-slate-300' : 'text-gray-600'}`}>+90 (555) 000 00 00</p>
        </div>
        <div>
          <h4 className={`text-sm font-semibold ${isDark ? 'text-slate-100' : 'text-gray-900'}`}>Adres</h4>
          <p className={`mt-2 text-sm ${isDark ? 'text-slate-300' : 'text-gray-600'}`}>
            İstanbul, Türkiye
          </p>
          <p className={`text-xs mt-3 ${isDark ? 'text-slate-500' : 'text-gray-400'}`}>© 2026 Haber Analiz</p>
        </div>
      </div>
    </footer>
  )
}

export default Footer

