package com.bitirme.nlp.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

/**
 * ML sınıflandırıcı ayarları: Spark model yolu, açık/kapalı, eğitim parametreleri.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "ml.classifier")
public class MlClassifierProperties {

    /** ML sınıflandırıcı kullanılsın mı (false ise keyword-based fallback). */
    private boolean enabled = false;

    /** Spark master (local mode için local[*]). */
    private String sparkMaster = "local[*]";

    /** Eğitilmiş PipelineModel'in kaydedildiği dizin. */
    private String modelPath = "data/ml-model";

    /** Minimum güven eşiği (altındaki tahminler 'Diğer' sayılabilir). */
    private double minConfidence = 0.0;

    /** Eğitim için minimum etiketli haber sayısı. */
    private int minTrainingSamples = 50;
}
