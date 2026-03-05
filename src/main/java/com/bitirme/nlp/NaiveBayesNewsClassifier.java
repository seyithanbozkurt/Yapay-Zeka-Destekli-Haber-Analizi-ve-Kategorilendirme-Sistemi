package com.bitirme.nlp;

import com.bitirme.entity.Category;
import com.bitirme.entity.News;
import com.bitirme.entity.NewsClassificationResult;
import com.bitirme.repository.NewsClassificationResultRepository;
import com.bitirme.repository.NewsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Basit çok terimli Naive Bayes metin sınıflandırıcı.
 * Eğitim verisi olarak news_classification_results tablosundaki mevcut etiketler kullanılır.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NaiveBayesNewsClassifier {

    private final NewsClassificationResultRepository classificationResultRepository;
    private final NewsRepository newsRepository;
    private final TurkishTextPreprocessor preprocessor;

    private boolean modelReady = false;

    // Sınıf isimleri
    private List<String> classes = List.of();
    // P(c) log uzayında
    private double[] classLogPrior;
    // P(w|c) log uzayında
    private Map<String, double[]> wordLogLikelihood = new HashMap<>();

    @Transactional(readOnly = true)
    public void train() {
        List<NewsClassificationResult> results = classificationResultRepository.findByActiveTrue();
        if (results.isEmpty()) {
            log.warn("NaiveBayes: no active classification results found, model not trained.");
            modelReady = false;
            return;
        }

        // Haber bazında tek etiket
        Map<Long, String> newsIdToLabel = new LinkedHashMap<>();
        for (NewsClassificationResult r : results) {
            News news = r.getNews();
            Category cat = r.getPredictedCategory();
            if (news == null || cat == null) continue;
            newsIdToLabel.putIfAbsent(news.getId(), cat.getName());
        }

        if (newsIdToLabel.size() < 20) {
            log.warn("NaiveBayes: not enough labeled samples ({}), need at least 20.", newsIdToLabel.size());
            modelReady = false;
            return;
        }

        classes = new ArrayList<>(new TreeSet<>(newsIdToLabel.values()));
        int numClasses = classes.size();
        Map<String, Integer> classToIndex = new HashMap<>();
        for (int i = 0; i < numClasses; i++) {
            classToIndex.put(classes.get(i), i);
        }

        log.info("NaiveBayes: training with {} samples, {} classes.", newsIdToLabel.size(), numClasses);

        Map<String, int[]> wordCounts = new HashMap<>();
        int[] classDocCounts = new int[numClasses];
        int[] classTokenCounts = new int[numClasses];

        for (Map.Entry<Long, String> e : newsIdToLabel.entrySet()) {
            Optional<News> newsOpt = newsRepository.findById(e.getKey());
            if (newsOpt.isEmpty()) continue;
            News news = newsOpt.get();

            String text = preprocessor.preprocess(news.getTitle(), news.getContent());
            if (text.isBlank()) continue;

            List<String> tokens = Arrays.asList(text.split("\\s+"));
            if (tokens.size() < 3) continue;

            int classIndex = classToIndex.get(e.getValue());
            classDocCounts[classIndex]++;

            for (String token : tokens) {
                if (token.length() < 2) continue;
                int[] counts = wordCounts.computeIfAbsent(token, k -> new int[numClasses]);
                counts[classIndex]++;
                classTokenCounts[classIndex]++;
            }
        }

        int totalDocs = Arrays.stream(classDocCounts).sum();
        if (totalDocs == 0) {
            log.warn("NaiveBayes: no valid documents after preprocessing, model not trained.");
            modelReady = false;
            return;
        }

        classLogPrior = new double[numClasses];
        for (int c = 0; c < numClasses; c++) {
            classLogPrior[c] = Math.log((classDocCounts[c] + 1.0) / (totalDocs + numClasses));
        }

        int vocabSize = wordCounts.size();
        wordLogLikelihood = new HashMap<>(vocabSize * 2);
        for (Map.Entry<String, int[]> entry : wordCounts.entrySet()) {
            String word = entry.getKey();
            int[] counts = entry.getValue();
            double[] logProbs = new double[numClasses];
            for (int c = 0; c < numClasses; c++) {
                double num = counts[c] + 1.0;
                double den = classTokenCounts[c] + vocabSize;
                logProbs[c] = Math.log(num / den);
            }
            wordLogLikelihood.put(word, logProbs);
        }

        modelReady = true;
        log.info("NaiveBayes: training completed. Vocab size={}, classes={}", vocabSize, numClasses);
    }

    public boolean isModelReady() {
        return modelReady;
    }

    public Optional<MlClassificationResult> classify(News news) {
        if (!modelReady || news == null) return Optional.empty();

        String text = preprocessor.preprocess(news.getTitle(), news.getContent());
        if (text.isBlank()) return Optional.empty();

        List<String> tokens = Arrays.asList(text.split("\\s+"));
        if (tokens.size() < 3) return Optional.empty();

        int numClasses = classes.size();
        double[] logScores = Arrays.copyOf(classLogPrior, numClasses);

        for (String token : tokens) {
            double[] wordLogProbs = wordLogLikelihood.get(token);
            if (wordLogProbs == null) continue;
            for (int c = 0; c < numClasses; c++) {
                logScores[c] += wordLogProbs[c];
            }
        }

        double max = Double.NEGATIVE_INFINITY;
        int bestIndex = -1;
        for (int c = 0; c < numClasses; c++) {
            if (logScores[c] > max) {
                max = logScores[c];
                bestIndex = c;
            }
        }
        if (bestIndex < 0) return Optional.empty();

        double sumExp = 0.0;
        double[] exps = new double[numClasses];
        for (int c = 0; c < numClasses; c++) {
            double v = Math.exp(logScores[c] - max);
            exps[c] = v;
            sumExp += v;
        }
        double prob = exps[bestIndex] / sumExp;

        String predictedCategory = classes.get(bestIndex);
        return Optional.of(MlClassificationResult.of(predictedCategory, prob));
    }
}

