package com.bitirme.nlp;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Spark MLlib multinomial NaiveBayesModel parametreleri (disk).
 * Spark SQL / parquet yüklemeden {@link org.apache.spark.mllib.classification.NaiveBayesModel}
 * oluşturmak için — Hibernate ile ANTLR çakışmasından kaçınır.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record NbModelExport(
        double[] labels,
        double[] pi,
        double[][] theta,
        String modelType,
        int numFeatures
) {}
