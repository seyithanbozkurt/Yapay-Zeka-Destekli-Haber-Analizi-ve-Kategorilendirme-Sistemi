package com.bitirme.nlp;

import com.bitirme.entity.Category;
import com.bitirme.entity.News;
import com.bitirme.entity.NewsClassificationResult;
import com.bitirme.nlp.config.MlClassifierProperties;
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
    private final MlClassifierProperties properties;

    private boolean modelReady = false;
    private volatile MlEvaluationMetrics lastEvaluationMetrics;

    // Sınıf isimleri
    private List<String> classes = List.of();
    // P(c) log uzayında
    private double[] classLogPrior;
    // P(w|c) log uzayında
    private Map<String, double[]> wordLogLikelihood = new HashMap<>();
    // Eğitimde görülmeyen kelimeler için sınıf bazlı log olasılık
    private double[] unknownWordLogLikelihood = new double[0];

    @Transactional(readOnly = true)
    public void train() {
        lastEvaluationMetrics = null;
        List<NewsClassificationResult> results = classificationResultRepository.findByActiveTrue();
        if (results.isEmpty()) {
            log.warn("NaiveBayes: no active classification results found, model not trained.");
            modelReady = false;
            return;
        }

        // Haber bazında tek etiket; gürültülü kategorileri (Diğer, Kurumsal vb.) hariç tut
        Set<String> excludedCategories = Set.of("Diğer", "Diger", "Sosyal", "Haberler", "Kurumsal");
        Map<Long, String> newsIdToLabel = new LinkedHashMap<>();
        for (NewsClassificationResult r : results) {
            News news = r.getNews();
            Category cat = r.getPredictedCategory();
            if (news == null || cat == null) continue;
            if (cat.getName() == null || excludedCategories.contains(cat.getName())) continue;
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

        List<LabeledExample> examples = new ArrayList<>();

        for (Map.Entry<Long, String> e : newsIdToLabel.entrySet()) {
            Optional<News> newsOpt = newsRepository.findById(e.getKey());
            if (newsOpt.isEmpty()) continue;
            News news = newsOpt.get();

            int classIndex = classToIndex.get(e.getValue());
            List<String> terms = extractTerms(news.getTitle(), news.getContent());
            if (terms.size() < 3) continue;
            examples.add(new LabeledExample(classIndex, terms));
        }

        if (examples.size() < 20) {
            log.warn("NaiveBayes: no valid documents after preprocessing, model not trained.");
            modelReady = false;
            return;
        }

        Collections.shuffle(examples, new Random(properties.getEvaluationSeed()));
        int splitIndex = (int) Math.max(1, Math.floor(examples.size() * (1.0 - properties.getTestSplitRatio())));
        List<LabeledExample> trainExamples = new ArrayList<>(examples.subList(0, splitIndex));
        List<LabeledExample> testExamples = new ArrayList<>(examples.subList(splitIndex, examples.size()));
        if (testExamples.size() < 10) {
            trainExamples = examples;
            testExamples = List.of();
        }

        ModelData model = buildModel(trainExamples, numClasses);
        if (model.totalDocs == 0) {
            log.warn("NaiveBayes: no valid documents after preprocessing, model not trained.");
            modelReady = false;
            return;
        }

        this.classLogPrior = model.classLogPrior;
        this.wordLogLikelihood = model.wordLogLikelihood;
        this.unknownWordLogLikelihood = model.unknownWordLogLikelihood;
        this.modelReady = true;

        if (!testExamples.isEmpty()) {
            this.lastEvaluationMetrics = evaluate(testExamples, numClasses);
            if (lastEvaluationMetrics != null) {
                log.info("NaiveBayes evaluation - Accuracy: {}, Weighted F1: {}, Test samples: {}",
                        lastEvaluationMetrics.getAccuracy(),
                        lastEvaluationMetrics.getWeightedF1(),
                        lastEvaluationMetrics.getTestSampleCount());
            }
        }

        log.info("NaiveBayes: training completed. Vocab size={}, classes={}, train={}, test={}",
                model.vocabSize, numClasses, trainExamples.size(), testExamples.size());
    }

    public boolean isModelReady() {
        return modelReady;
    }

    public Optional<MlClassificationResult> classify(News news) {
        if (!modelReady || news == null) return Optional.empty();

        List<String> terms = extractTerms(news.getTitle(), news.getContent());
        if (terms.size() < 3) return Optional.empty();

        int numClasses = classes.size();
        double[] logScores = Arrays.copyOf(classLogPrior, numClasses);

        for (String token : terms) {
            double[] wordLogProbs = wordLogLikelihood.get(token);
            if (wordLogProbs == null) {
                for (int c = 0; c < numClasses; c++) {
                    logScores[c] += unknownWordLogLikelihood[c];
                }
                continue;
            }
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

    public MlEvaluationMetrics getLastEvaluationMetrics() {
        return lastEvaluationMetrics;
    }

    private List<String> extractTerms(String title, String content) {
        String text = preprocessor.preprocess(title, content);
        if (text.isBlank()) return List.of();
        List<String> tokens = Arrays.stream(text.split("\\s+"))
                .filter(t -> t.length() >= 2)
                .toList();
        if (tokens.isEmpty()) return List.of();
        int maxN = Math.max(1, Math.min(3, properties.getNGramMax()));
        List<String> terms = new ArrayList<>(tokens);
        for (int n = 2; n <= maxN; n++) {
            for (int i = 0; i + n <= tokens.size(); i++) {
                terms.add(String.join("_", tokens.subList(i, i + n)));
            }
        }
        return terms;
    }

    private ModelData buildModel(List<LabeledExample> examples, int numClasses) {
        Map<String, int[]> wordCounts = new HashMap<>();
        int[] classDocCounts = new int[numClasses];
        int[] classTokenCounts = new int[numClasses];

        for (LabeledExample ex : examples) {
            classDocCounts[ex.labelIndex]++;
            for (String term : ex.terms) {
                int[] counts = wordCounts.computeIfAbsent(term, k -> new int[numClasses]);
                counts[ex.labelIndex]++;
                classTokenCounts[ex.labelIndex]++;
            }
        }

        int totalDocs = Arrays.stream(classDocCounts).sum();
        double[] classLogPrior = new double[numClasses];
        for (int c = 0; c < numClasses; c++) {
            classLogPrior[c] = Math.log((classDocCounts[c] + 1.0) / (totalDocs + numClasses));
        }

        int vocabSize = Math.max(1, wordCounts.size());
        Map<String, double[]> wordLogLikelihood = new HashMap<>(vocabSize * 2);
        double[] unknownWordLogLikelihood = new double[numClasses];
        for (int c = 0; c < numClasses; c++) {
            unknownWordLogLikelihood[c] = Math.log(1.0 / (classTokenCounts[c] + vocabSize));
        }

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

        return new ModelData(totalDocs, vocabSize, classLogPrior, wordLogLikelihood, unknownWordLogLikelihood);
    }

    private MlEvaluationMetrics evaluate(List<LabeledExample> testExamples, int numClasses) {
        if (testExamples.isEmpty()) return null;
        int[][] confusion = new int[numClasses][numClasses];
        int correct = 0;
        for (LabeledExample ex : testExamples) {
            int pred = predictClassIndex(ex.terms);
            if (pred < 0) continue;
            confusion[ex.labelIndex][pred]++;
            if (pred == ex.labelIndex) correct++;
        }

        int total = Arrays.stream(confusion).flatMapToInt(Arrays::stream).sum();
        if (total == 0) return null;

        Map<String, Double> precisionByClass = new LinkedHashMap<>();
        Map<String, Double> recallByClass = new LinkedHashMap<>();
        Map<String, Double> f1ByClass = new LinkedHashMap<>();
        double weightedPrecision = 0.0;
        double weightedRecall = 0.0;
        double weightedF1 = 0.0;
        double macroF1 = 0.0;

        for (int c = 0; c < numClasses; c++) {
            int tp = confusion[c][c];
            int fp = 0;
            int fn = 0;
            int support = 0;
            for (int i = 0; i < numClasses; i++) {
                support += confusion[c][i];
                if (i != c) {
                    fn += confusion[c][i];
                    fp += confusion[i][c];
                }
            }
            double precision = tp + fp == 0 ? 0.0 : (double) tp / (tp + fp);
            double recall = tp + fn == 0 ? 0.0 : (double) tp / (tp + fn);
            double f1 = precision + recall == 0 ? 0.0 : (2 * precision * recall) / (precision + recall);
            String className = classes.get(c);
            precisionByClass.put(className, precision);
            recallByClass.put(className, recall);
            f1ByClass.put(className, f1);
            weightedPrecision += precision * support;
            weightedRecall += recall * support;
            weightedF1 += f1 * support;
            macroF1 += f1;
        }

        return MlEvaluationMetrics.builder()
                .accuracy((double) correct / total)
                .weightedPrecision(weightedPrecision / total)
                .weightedRecall(weightedRecall / total)
                .weightedF1(weightedF1 / total)
                .macroF1(macroF1 / numClasses)
                .testSampleCount(total)
                .precisionByClass(precisionByClass)
                .recallByClass(recallByClass)
                .f1ByClass(f1ByClass)
                .build();
    }

    private int predictClassIndex(List<String> terms) {
        int numClasses = classes.size();
        double[] logScores = Arrays.copyOf(classLogPrior, numClasses);
        for (String token : terms) {
            double[] wordLogProbs = wordLogLikelihood.get(token);
            if (wordLogProbs == null) {
                for (int c = 0; c < numClasses; c++) {
                    logScores[c] += unknownWordLogLikelihood[c];
                }
                continue;
            }
            for (int c = 0; c < numClasses; c++) {
                logScores[c] += wordLogProbs[c];
            }
        }

        int bestIndex = -1;
        double max = Double.NEGATIVE_INFINITY;
        for (int c = 0; c < numClasses; c++) {
            if (logScores[c] > max) {
                max = logScores[c];
                bestIndex = c;
            }
        }
        return bestIndex;
    }

    private static class LabeledExample {
        private final int labelIndex;
        private final List<String> terms;

        private LabeledExample(int labelIndex, List<String> terms) {
            this.labelIndex = labelIndex;
            this.terms = terms;
        }
    }

    private static class ModelData {
        private final int totalDocs;
        private final int vocabSize;
        private final double[] classLogPrior;
        private final Map<String, double[]> wordLogLikelihood;
        private final double[] unknownWordLogLikelihood;

        private ModelData(int totalDocs, int vocabSize, double[] classLogPrior,
                          Map<String, double[]> wordLogLikelihood,
                          double[] unknownWordLogLikelihood) {
            this.totalDocs = totalDocs;
            this.vocabSize = vocabSize;
            this.classLogPrior = classLogPrior;
            this.wordLogLikelihood = wordLogLikelihood;
            this.unknownWordLogLikelihood = unknownWordLogLikelihood;
        }
    }
}

