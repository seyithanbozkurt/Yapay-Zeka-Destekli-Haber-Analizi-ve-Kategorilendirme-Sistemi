package com.bitirme.util;

/**
 * Haber başlığı normalizasyonu - duplicate kontrolü için tekrarlanan mantık.
 */
public final class NewsTitleNormalizer {

    private NewsTitleNormalizer() {}

    /**
     * Başlığı normalize eder: küçük harf, özel karakterleri ve fazla boşlukları temizler.
     * Türkçe karakterler korunur.
     */
    public static String normalize(String title) {
        if (title == null || title.isEmpty()) {
            return "";
        }
        String normalized = title.toLowerCase(java.util.Locale.forLanguageTag("tr"));
        normalized = normalized.replaceAll("[^a-zçğıöşü0-9\\s]", "");
        normalized = normalized.replaceAll("\\s+", " ").trim();
        return normalized;
    }
}
