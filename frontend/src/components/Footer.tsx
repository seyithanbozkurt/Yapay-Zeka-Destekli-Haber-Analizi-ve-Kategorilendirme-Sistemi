function Footer() {
  return (
    <footer className="mt-10 bg-white border-t border-gray-200">
      <div className="max-w-7xl mx-auto px-4 py-8 grid grid-cols-1 md:grid-cols-3 gap-6">
        <div>
          <h4 className="text-sm font-semibold text-gray-900">Haber Analiz</h4>
          <p className="mt-2 text-sm text-gray-600">
            Yapay zeka destekli haber sınıflandırma ve kullanıcı geri bildirimi platformu.
          </p>
        </div>
        <div>
          <h4 className="text-sm font-semibold text-gray-900">İletişim</h4>
          <p className="mt-2 text-sm text-gray-600">info@haberanaliz.com</p>
          <p className="text-sm text-gray-600">+90 (555) 000 00 00</p>
        </div>
        <div>
          <h4 className="text-sm font-semibold text-gray-900">Adres</h4>
          <p className="mt-2 text-sm text-gray-600">
            İstanbul, Türkiye
          </p>
          <p className="text-xs text-gray-400 mt-3">© 2026 Haber Analiz</p>
        </div>
      </div>
    </footer>
  )
}

export default Footer

