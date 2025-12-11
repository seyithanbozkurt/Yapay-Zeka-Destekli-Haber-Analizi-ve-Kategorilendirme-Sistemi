# Haber İşleme ve Kategorilendirme Projesi

Bu proje, Apache Spark kullanarak büyük ölçekli haber verilerini işleyen ve kategorilendiren bir sistemdir. NewsAPI entegrasyonu ile güncel haberleri otomatik olarak çekip işler.

## Özellikler

- NewsAPI üzerinden otomatik haber verisi çekme
- Haber metinlerinin ön işlenmesi
- Metin sınıflandırma
- Kategori dağılımı görselleştirme
- Ölçeklenebilir büyük veri işleme

## Gereksinimler

Projeyi çalıştırmak için aşağıdaki gereksinimlere ihtiyacınız vardır:

- Python 3.8+
- Apache Spark
- NewsAPI API anahtarı (https://newsapi.org/ adresinden ücretsiz alabilirsiniz)
- Gerekli Python paketleri (requirements.txt dosyasında listelenmiştir)

## Kurulum

1. Gerekli paketleri yükleyin:
```bash
pip install -r requirements.txt
```

2. Apache Spark'ı kurun ve ortam değişkenlerini ayarlayın.

3. NewsAPI'den bir API anahtarı alın.

4. `news_processing.py` dosyasında `API_KEY` değişkenini kendi API anahtarınızla güncelleyin.

## Kullanım

1. `news_processing.py` dosyasını çalıştırın:
```bash
python news_processing.py
```

Program otomatik olarak:
- NewsAPI'den son 7 günün haberlerini çekecek
- Verileri işleyecek
- Kategorilendirme yapacak
- Sonuçları görselleştirecek

## Özelleştirme

`main()` fonksiyonunda aşağıdaki parametreleri değiştirebilirsiniz:
- `query`: Arama terimi
- `language`: Haber dili
- `from_date`: Başlangıç tarihi
- `to_date`: Bitiş tarihi

## Veri Formatı

Program, haberleri aşağıdaki JSON formatında kaydeder:

```json
{
    "title": "Haber Başlığı",
    "content": "Haber İçeriği",
    "category": "Kategori",
    "source": "Haber Kaynağı",
    "publishedAt": "Yayın Tarihi"
}
```

## Lisans

MIT 