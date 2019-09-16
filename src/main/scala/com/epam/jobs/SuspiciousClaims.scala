package com.epam.jobs

import java.io.File

import com.epam.{NapTime, PgUrl}
import com.typesafe.config.{Config, ConfigFactory}
import org.apache.spark.SparkConf
import org.apache.spark.sql.SparkSession
import org.postgresql.Driver
import org.apache.spark.sql.functions._

object SuspiciousClaims extends PgUrl with NapTime {

  private val conf: Config = ConfigFactory.load()

  def main(args: Array[String]): Unit = {
    import org.apache.spark.sql.SparkSession

    val spark = SparkSession.builder
      .appName("SuspiciousClaims")
      .master("local[1]")
      .getOrCreate()

    import spark.sqlContext.implicits._
    classOf[org.postgresql.Driver]
    org.postgresql.Driver.isRegistered() // postgres driver bug

    val jdbcDF = spark.read.format("jdbc")
      .option("url", pgConnectionString()(conf))
      .option("driver", "org.postgresql.Driver")
      .option("query", "SELECT id, patient_id, amount, created_at FROM claims")
      .option("fetchsize", conf.getString("query.fetch.size"))
      .option("user", conf.getString("postgres.dbuser"))
      .option("password", conf.getString("postgres.password"))
      .load()

    jdbcDF.printSchema()

    val descDF = jdbcDF.orderBy($"amount".desc)

    val stats = descDF.select(mean($"amount").as("m"), stddev_pop($"amount").as("s"))
    val meanPlusOneSigma = stats
      .withColumn("ms", stats.col("m") + stats.col("s"))
      .select("ms")
      .drop("m")
      .drop("s")
      .first()
      .getDouble(0)

    val flaggedResults = descDF
      .where($"amount" > meanPlusOneSigma)
      .drop("created_at")
      .withColumn("amt", format_number($"amount", 2))
      .drop("amount")
      .withColumn("flagged", lit("true"))

    val reportPath = conf.getString("suspicious.claims.report")
    val report = new File(reportPath)
    if (report.exists()) {
      report.delete()
      println(s"WARN: DELETING REPORT [$reportPath]")
      zzz(1000)
    }

    flaggedResults.coalesce(1)
      .write
      .format("csv")
      .option("sep", ",")
      .option("header", "true")
      .save(reportPath)

    println(s"Wrote new suspicious claims results: [$reportPath]")

    zzz(2000)

    spark.stop()

  }

}
