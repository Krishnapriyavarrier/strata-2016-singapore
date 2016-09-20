/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// scalastyle:off println
package org.apache.spark

// $example on$

import org.apache.spark.ml.classification.RandomForestClassifier
import org.apache.spark.ml.evaluation.BinaryClassificationEvaluator
import org.apache.spark.ml.feature.{StringIndexer, VectorAssembler}
import org.apache.spark.sql.types._

// $example off$
import org.apache.spark.sql.SparkSession


object Churn {

  def main(args: Array[String]) {
    val spark = SparkSession
      .builder
      .appName("ALSExample")
      .master("local")
      .getOrCreate()

    val customSchema = StructType(Array(
      StructField("state", StringType, true),
      StructField("account_length", DoubleType, true),
      StructField("area_code", StringType, true),

      StructField("phone_number", StringType, true),
      StructField("intl_plan", StringType, true),

      StructField("voice_mail_plan", StringType, true),
      StructField("number_vmail_messages", DoubleType, true),

      StructField("total_day_minutes", DoubleType, true),
      StructField("total_day_calls", DoubleType, true),

      StructField("total_day_charge", DoubleType, true),
      StructField("total_eve_minutes", DoubleType, true),
      StructField("total_eve_calls", DoubleType, true),
      StructField("total_eve_charge", DoubleType, true),
      StructField("total_night_minutes", DoubleType, true),

      StructField("total_night_calls", DoubleType, true),
      StructField("total_night_charge", DoubleType, true),
      StructField("total_intl_minutes", DoubleType, true),
      StructField("total_intl_calls", DoubleType, true),
      StructField("total_intl_charge", DoubleType, true),
      StructField("number_customer_service_calls", DoubleType, true),

      StructField("churned", StringType, true)

    ))

    val ds = spark.read.option("inferSchema", "true").schema(customSchema).csv("data/churn.all")

    ds.printSchema()

    // intl_plan
    val indexer = new StringIndexer()
      .setInputCol("intl_plan")
      .setOutputCol("intl_plan_idx")
    val indexed = indexer.fit(ds).transform(ds)

    indexed.printSchema()

    // churned
    val churn = new StringIndexer()
      .setInputCol("churned")
      .setOutputCol("churned_idx")
    val churned = churn.fit(indexed).transform(indexed)

    // vector assembler
    val assembler = new VectorAssembler()
      .setInputCols(Array("account_length", "intl_plan_idx", "number_vmail_messages", "total_day_minutes",
                          "total_day_calls", "total_day_charge", "total_eve_minutes", "total_eve_calls",
                          "total_night_minutes", "total_night_calls", "total_night_charge", "total_intl_minutes",
                          "total_intl_calls", "total_intl_charge", "number_customer_service_calls"))
      .setOutputCol("features")

    val assemdata = assembler.transform(churned)

    assemdata.printSchema()

    // split
    val Array(trainingData, testData) = assemdata.randomSplit(Array(0.7, 0.3), 1000)

    // Train a RandomForest model.
    val rf = new RandomForestClassifier()
      .setLabelCol("churned_idx")
      .setFeaturesCol("features")
      .setNumTrees(10)

    // Fit the model
    val rfModel = rf.fit(trainingData)

    val str = rfModel.toDebugString
    println(str)

    // predict
    val predict = rfModel.transform(testData)

    predict.select("churned", "prediction").show(1000)

    val evaluator = new BinaryClassificationEvaluator()
      .setLabelCol("churned_idx")
      .setRawPredictionCol("prediction")
      //.setMetricName("rmse")

    val accuracy = evaluator.evaluate(predict)
    println("Test Error = " + (1.0 - accuracy))

    spark.stop()
  }
}
// scalastyle:on println


