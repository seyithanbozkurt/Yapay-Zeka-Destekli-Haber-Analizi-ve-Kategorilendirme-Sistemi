package com.bitirme.service;

import com.bitirme.nlp.NaiveBayesNewsClassifier;
import com.bitirme.nlp.SparkNewsClassifier;
import com.bitirme.nlp.SparkNewsClassifierTrainer;
import com.bitirme.nlp.config.MlClassifierProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MlModelTrainingServiceTest {

    @Mock
    private SparkNewsClassifierTrainer sparkTrainer;
    @Mock
    private SparkNewsClassifier sparkClassifier;
    @Mock
    private NaiveBayesNewsClassifier naiveBayesNewsClassifier;

    private MlClassifierProperties mlProperties;
    private MlModelTrainingService service;
    private ExecutorService executor;

    @BeforeEach
    void setUp() {
        mlProperties = new MlClassifierProperties();
        mlProperties.setEnabled(false);
        mlProperties.setSparkTrainIsolatedFirst(false);
        executor = Executors.newSingleThreadExecutor();
        service = new MlModelTrainingService(
                mlProperties,
                Optional.of(sparkTrainer),
                Optional.of(sparkClassifier),
                Optional.of(naiveBayesNewsClassifier),
                executor
        );
        ReflectionTestUtils.setField(service, "datasourceUrl", "jdbc:postgresql://localhost:5432/test");
        ReflectionTestUtils.setField(service, "datasourceUsername", "u");
        ReflectionTestUtils.setField(service, "datasourcePassword", "p");
    }

    @AfterEach
    void tearDown() {
        executor.shutdown();
    }

    @Test
    @DisplayName("runFullTrainingSync Spark kapalıyken Naive Bayes train çağrılır")
    void runFullTrainingSyncCallsNaiveBayesTest() {
        MlModelTrainingService.TrainingOutcome outcome = service.runFullTrainingSync();

        assertThat(outcome.message()).isNotBlank();
        assertThat(outcome.data()).containsKey("sparkEnabled");
        assertThat(outcome.data().get("sparkEnabled")).isEqualTo(false);
        verify(naiveBayesNewsClassifier).train();
    }

    @Test
    @DisplayName("başlangıçta son snapshot null")
    void lastSnapshotInitiallyNullTest() {
        assertThat(service.getLastTrainingSnapshot()).isNull();
    }
}
