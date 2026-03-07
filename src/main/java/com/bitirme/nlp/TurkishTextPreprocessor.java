package com.bitirme.nlp;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Türkçe metin ön işleme: Lucene StandardTokenizer + küçük harf + Türkçe stop-word.
 * Zemberek opsiyonel; classpath'te yoksa bu Lucene tabanlı ön işleme kullanılır.
 */
@Component
public class TurkishTextPreprocessor {

    private static final CharArraySet TURKISH_STOP_WORDS = new CharArraySet(
            Arrays.asList(
                    "ve", "bir", "bu", "da", "de", "için", "ile", "gibi", "kadar", "mi", "mı", "mu", "mü",
                    "olan", "olarak", "var", "yok", "daha", "en", "çok", "şu", "ne", "nasıl", "niye", "neden",
                    "veya", "ya", "hem", "ise", "ki", "ama", "fakat", "ancak", "lakin", "üzere", "sonra",
                    "önce", "şimdi", "bazı", "her", "tüm", "bütün", "diğer", "aynı", "kendi", "biz", "siz",
                    "onlar", "bu", "şu", "o", "ben", "sen", "hep", "hiç", "artık", "yine", "zaten", "hala",
                    "henüz", "daha", "sadece", "yalnız", "belki", "galiba", "acaba", "tabii", "elbette"
            ),
            true
    );

    private final Analyzer analyzer = new TurkishLuceneAnalyzer();

    /**
     * Ham haber metnini (başlık + içerik) TF-IDF / ML için normalize edilmiş tek metne çevirir.
     */
    public String preprocess(String title, String content) {
        String raw = (title != null ? title : "") + " " + (content != null ? content : "");
        return preprocess(raw);
    }

    /**
     * Tek metni tokenize + küçük harf + stop-word temizleyerek döner.
     */
    public String preprocess(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return "";
        }
        try (TokenStream stream = analyzer.tokenStream("", new StringReader(rawText.trim()))) {
            CharTermAttribute termAttr = stream.addAttribute(CharTermAttribute.class);
            stream.reset();
            StringBuilder sb = new StringBuilder();
            while (stream.incrementToken()) {
                if (sb.length() > 0) sb.append(' ');
                sb.append(termAttr);
            }
            stream.end();
            return sb.toString().toLowerCase(Locale.forLanguageTag("tr"));
        } catch (IOException e) {
            return rawText.toLowerCase(Locale.forLanguageTag("tr"))
                    .replaceAll("[^a-zçğıöşü0-9\\s]", " ")
                    .replaceAll("\\s+", " ").trim();
        }
    }

    /**
     * Token listesi olarak döner (Spark için alternatif).
     */
    public List<String> preprocessToTokens(String rawText) {
        String normalized = preprocess(rawText);
        if (normalized.isEmpty()) return List.of();
        return Arrays.stream(normalized.split("\\s+")).filter(s -> !s.isEmpty()).collect(Collectors.toList());
    }

    private static final class TurkishLuceneAnalyzer extends Analyzer {
        @Override
        protected TokenStreamComponents createComponents(String fieldName) {
            Tokenizer tokenizer = new StandardTokenizer();
            TokenStream stream = new LowerCaseFilter(tokenizer);
            stream = new StopFilter(stream, TURKISH_STOP_WORDS);
            return new TokenStreamComponents(tokenizer, stream);
        }
    }
}
