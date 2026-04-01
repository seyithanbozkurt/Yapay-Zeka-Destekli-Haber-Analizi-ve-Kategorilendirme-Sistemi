# Değişiklik Özeti (Bu commit için)

## 1. Haber silme: Her 5 saatte bir çalışacak şekilde ayarlandı

- **Dosya:** `src/main/java/com/bitirme/config/ScheduledTasks.java`
- **Değişiklik:** `deleteAndRefreshNews()` görevinin cron ifadesi düzeltildi.
  - **Eski:** `0 * */5 * * ?` (her 5 saatte bir saatte her dakika tetikleniyordu)
  - **Yeni:** `0 0 */5 * * ?` (her 5 saatte bir, sadece saatin 0. dakikasında: 00:00, 05:00, 10:00, 15:00, 20:00)
- **Dosya:** `src/main/resources/application.properties`
- **Değişiklik:** Periyodik haber silme ve yenileme varsayılan olarak açıldı.
  - **Eski:** `scheduler.delete-refresh.enabled=false`
  - **Yeni:** `scheduler.delete-refresh.enabled=true`
  - Açıklama satırı güncellendi: "Her 5 saatte bir haberler silinir ve yeniden çekilir. Kapatmak için false yapın."

## 2. Kategorilendirme testleri eklendi

- **Yeni dosya:** `src/test/java/com/bitirme/service/NewsClassificationServiceTest.java`
- **Amaç:** Anahtar kelime + başlık ağırlığı ile doğru kategorinin atandığını doğrulamak.
- **Test senaryoları:**
  - **Asayiş (cinayet/ceza):** "Oğlunu parkta 37 bıçak darbesi ile öldürmüştü: Cezası belli oldu" → Asayiş beklenir.
  - **Asayiş (başlık baskın):** Başlıkta bıçak/ceza, içerikte yol/trafik olsa bile Asayiş beklenir.
  - **Savaş:** "Savaş bölgesinde ateşkes ilan edildi" → Savaş beklenir.
  - **Ulaşım:** "Ankara'da trafik kazası: 2 yaralı" → Ulaşım beklenir.
  - **Kaza tek başına Ulaşım değil:** "İş kazası" gibi trafik kazası olmayan haber Ulaşım’a düşmemeli.
  - **Spor:** Süper Lig / maç içeren haber → Spor beklenir.
  - **Ekonomi:** Merkez Bankası / faiz içeren haber → Ekonomi beklenir.
- **Servis değişikliği:** `NewsClassificationService` içine test için `predictCategoryKeywordOnly(String title, String content)` eklendi; sadece anahtar kelime skoruna göre kategori adını döndürür (kaydetmez).

## 3. Özet

| Ne değişti? | Nerede? | Neden? |
|-------------|---------|--------|
| Cron ifadesi | ScheduledTasks.java | Her 5 saatte bir yalnızca bir kez (0. dakikada) tetiklensin diye. |
| Scheduler açık | application.properties | Haber silme/yenileme periyodik çalışsın diye. |
| Kategorilendirme testleri | NewsClassificationServiceTest.java | Doğru kategorilendirme (Asayiş, Savaş, Ulaşım vb.) doğrulansın diye. |
| Test için metot | NewsClassificationService.java | Testlerin başlık+içerik ile kategori tahminini çağırabilmesi için. |

---

**Commit mesajı önerisi (aşağıda).**
