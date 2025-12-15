from pyspark.sql import SparkSession
from pyspark.sql.functions import col, udf
from pyspark.sql.types import StringType
import requests
from bs4 import BeautifulSoup
from datetime import datetime
import random
import matplotlib.pyplot as plt
import pandas as pd
from collections import Counter

# Spark oturumu oluşturma
spark = SparkSession.builder \
    .appName("Haber Analizi") \
    .config("spark.executor.memory", "2g") \
    .config("spark.driver.memory", "2g") \
    .getOrCreate()

def fetch_news_from_websites():
    """
    Türkçe haber sitelerinden haberleri çeker
    """
    all_articles = []
    headers = {
        'User-Agent': random.choice([
            'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36',
            'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36',
            'Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:89.0) Gecko/20100101 Firefox/89.0'
        ]),
        'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8',
        'Accept-Language': 'tr,en-US;q=0.7,en;q=0.3',
    }

    # Hürriyet
    try:
        response = requests.get('https://www.hurriyet.com.tr/gundem/', headers=headers, timeout=10)
        response.raise_for_status()
        soup = BeautifulSoup(response.content, 'html.parser')
        for article in soup.find_all('div', class_='category__list__item'):
            try:
                h2 = article.find('h2')
                title = h2.text.strip() if h2 else ''
                a_tag = article.find('a', href=True)
                link = a_tag['href'] if a_tag else ''
                if link and not link.startswith('http'):
                    link = 'https://www.hurriyet.com.tr' + link
                summary = article.find('p').text.strip() if article.find('p') else ''
                all_articles.append({
                    'title': title,
                    'content': summary,
                    'source': 'Hürriyet',
                    'publishedAt': datetime.now().strftime('%Y-%m-%d')
                })
            except:
                continue
    except:
        pass

    # Sabah
    try:
        response = requests.get('https://www.sabah.com.tr/gundem', headers=headers, timeout=10)
        response.raise_for_status()
        soup = BeautifulSoup(response.content, 'html.parser')
        for article in soup.find_all('a', title=True):
            title = article['title'].strip()
            link = article['href']
            if not link.startswith('http'):
                link = 'https://www.sabah.com.tr' + link
            if len(title) > 30 and '/gundem/' in link:
                all_articles.append({
                    'title': title,
                    'content': title,
                    'source': 'Sabah',
                    'publishedAt': datetime.now().strftime('%Y-%m-%d')
                })
    except:
        pass

    # Milliyet
    try:
        response = requests.get('https://www.milliyet.com.tr/gundem/', headers=headers, timeout=10)
        response.raise_for_status()
        soup = BeautifulSoup(response.content, 'html.parser')
        for article in soup.find_all('strong', class_='cat-list-card__title'):
            title = article.text.strip()
            a_tag = article.find_parent('a', href=True)
            link = a_tag['href'] if a_tag else ''
            if link and not link.startswith('http'):
                link = 'https://www.milliyet.com.tr' + link
            if len(title) > 20 and '/gundem/' in link:
                all_articles.append({
                    'title': title,
                    'content': title,
                    'source': 'Milliyet',
                    'publishedAt': datetime.now().strftime('%Y-%m-%d')
                })
    except:
        pass

    return all_articles

def categorize_news(df):
    """
    Haberleri kategorilere ayırır
    """
    def assign_category(text):
        text = text.lower()
        categories = {
            'Asayiş': ['cinayet', 'ölüm', 'kaza', 'tutuklama', 'ceza', 'mahkeme', 'dava', 'şüpheli', 'suç', 
                      'operasyon', 'gözaltı', 'şebeke', 'organize suç', 'yolsuzluk', 'hırsızlık', 'dolandırıcılık',
                      'öldür', 'boğul', 'feci olay', 'korkunç olay', 'katled', 'bıçakla', 'silah', 'terör', 
                      'bomba', 'patlama', 'kaçakçılık', 'uyuşturucu', 'fetö', 'pdy', 'terör örgütü'],
            'Siyaset': ['bakan', 'meclis', 'parti', 'seçim', 'hükümet', 'cumhurbaşkanı', 'dışişleri', 'terör',
                       'başkan', 'lider', 'miting', 'chp', 'akp', 'mhp', 'iyi parti', 'demokratik parti', 
                       'siyasi', 'diplomatik', 'uluslararası', 'dış politika', 'kamu', 'devlet', 'pakistan',
                       'başbakan', 'dışişleri bakanı', 'büyükelçi', 'diplomat'],
            'Spor': ['futbol', 'basketbol', 'spor', 'milli takım', 'şampiyonlar ligi', 'süper lig', 
                    'galatasaray', 'fenerbahçe', 'beşiktaş', 'trabzonspor', 'maç', 'turnuva', 'şampiyona',
                    'sporcu', 'antrenman', 'etnospor', 'spor kulübü', 'spor federasyonu', 'olimpiyat'],
            'Turizm': ['turizm', 'seyahat', 'tatil', 'otel', 'rezervasyon', 'tur', 'gezi', 'turist',
                      'seyahat acentesi', 'uçak', 'bilet', 'nemrut', 'antik kent', 'müze', 'tarihi eser',
                      'kültür turu', 'doğa turu', 'ziyaretçi', 'turizm sezonu', 'turizm bakanlığı'],
            'Teknoloji': ['teknoloji', 'yazılım', 'bilgisayar', 'telefon', 'internet', 'dijital', 'yapay zeka',
                         'siber', 'uygulama', 'donanım', 'inovasyon', 'startup', 'sosyal medya', 'dijital varlık',
                         'blockchain', 'kripto', 'yazılım şirketi', 'teknoloji şirketi', 'akıllı telefon'],
            'Sağlık': ['sağlık', 'hastane', 'doktor', 'tedavi', 'ilaç', 'korona', 'virüs', 'hastalık', 'aşı',
                      'klinik', 'ameliyat', 'check-up', 'muayene', 'sağlık bakanlığı', 'tıp', 'sağlık hizmeti',
                      'hasta', 'sağlık personeli', 'sağlık çalışanı'],
            'Doğal Afet': ['deprem', 'sel', 'fırtına', 'taşkın', 'afet', 'heyelan', 'çığ', 'kuraklık', 'yangın',
                          'doğal afet', 'meydana geldi', 'hasar', 'zede', 'kayıp', 'felaket', 'afet bölgesi',
                          'itfaiye', 'afad', 'kurtarma ekibi'],
            'Ulaşım': ['trafik', 'metro', 'otobüs', 'tren', 'havayolu', 'karayolu', 'denizyolu', 'ulaşım',
                      'seyahat', 'yol', 'kaza', 'araba', 'şoför', 'sürücü', 'yolcu', 'toplu taşıma',
                      'trafik yoğunluğu', 'trafik kazası', 'trafik polisi'],
            'Çevre': ['çevre', 'doğa', 'iklim', 'hava', 'su', 'toprak', 'kirlilik', 'geri dönüşüm', 'atık',
                     'enerji', 'yeşil', 'sürdürülebilir', 'ekoloji', 'biyoçeşitlilik', 'çevre kirliliği',
                     'doğal kaynak', 'petrol', 'rezerv', 'doğal gaz', 'enerji kaynağı'],
            'Ekonomi': ['ekonomi', 'piyasa', 'borsa', 'dolar', 'euro', 'altın', 'faiz', 'enflasyon', 'yatırım',
                       'şirket', 'ticaret', 'ihracat', 'ithalat', 'sanayi', 'üretim', 'tüketim', 'ekonomik kriz',
                       'ekonomi bakanlığı', 'merkez bankası', 'ekonomi politikası'],
            'Magazin': ['magazin', 'sanatçı', 'oyuncu', 'show', 'dizi', 'film', 'müzik', 'konser', 'festival',
                       'ödül', 'röportaj', 'sanat', 'kültür', 'tiyatro', 'sergi', 'sinema', 'sanat etkinliği',
                       'kültür sanat', 'festival']
        }
        
        for category, keywords in categories.items():
            if any(keyword in text for keyword in keywords):
                return category
        return 'Diğer'
    
    categorize_udf = udf(assign_category, StringType())
    return df.withColumn('category', categorize_udf(col('content')))

def analyze_news_content(df):
    """
    Haber içeriklerini detaylı analiz eder
    """
    pdf = df.toPandas()
    category_stats = {}
    
    for category in pdf['category'].unique():
        category_news = pdf[pdf['category'] == category]
        category_stats[category] = {
            'count': len(category_news),
            'sources': category_news['source'].value_counts().to_dict(),
            'words': Counter(' '.join(category_news['content'].fillna('')).lower().split())
        }
    
    return category_stats

def visualize_categories(df):
    """
    Kategori dağılımı ve kaynak bazlı kategori dağılımını görselleştirir
    """
    pdf = df.toPandas()
    
    # Kategori dağılımı
    plt.figure(figsize=(15, 8))
    category_counts = pdf['category'].value_counts()
    plt.pie(category_counts, labels=category_counts.index, autopct='%1.1f%%', startangle=90)
    plt.title('Haber Kategorileri Dağılımı', pad=20, fontsize=14)
    plt.axis('equal')
    plt.savefig('category_distribution.png', bbox_inches='tight', dpi=300)
    plt.close()
    
    # Kaynak bazlı kategori dağılımı
    plt.figure(figsize=(15, 8))
    source_category = pd.crosstab(pdf['source'], pdf['category'])
    source_category.plot(kind='bar', stacked=True)
    plt.title('Kaynak Bazlı Kategori Dağılımı', pad=20, fontsize=14)
    plt.xlabel('Haber Kaynağı', fontsize=12)
    plt.ylabel('Haber Sayısı', fontsize=12)
    plt.xticks(rotation=45)
    plt.legend(title='Kategori', bbox_to_anchor=(1.05, 1), loc='upper left')
    plt.tight_layout()
    plt.savefig('source_category_distribution.png', bbox_inches='tight', dpi=300)
    plt.close()

def main():
    articles = fetch_news_from_websites()
    print(f"\nToplam {len(articles)} haber çekildi.")
    
    df = spark.createDataFrame(articles)
    df = categorize_news(df)
    
    # Kategori istatistiklerini göster
    category_counts = df.groupBy('category').count().collect()
    print("\nKategori Dağılımı:")
    for row in category_counts:
        print(f"{row['category']}: {row['count']} haber")
    
    # Kaynak istatistiklerini göster
    source_counts = df.groupBy('source').count().collect()
    print("\nKaynak Dağılımı:")
    for row in source_counts:
        print(f"{row['source']}: {row['count']} haber")
    
    stats = analyze_news_content(df)
    visualize_categories(df)

if __name__ == "__main__":
    main() 