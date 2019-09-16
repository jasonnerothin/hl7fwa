package com.epam.jobs

import com.epam.HL7Deserializer
import com.typesafe.config.{Config, ConfigFactory}
import org.apache.spark.sql.SparkSession
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.spark.SparkConf
import org.apache.spark.streaming._
import org.apache.spark.streaming.kafka010._

object SuspiciousMessages {

  private val conf: Config = ConfigFactory.load()

  private val bootstrapServers = conf.getString("kafka.bootstrap.servers")
  private val keySerializer = conf.getString("key.serializer")
  private val valueDeserializer = conf.getString("value.deserializer")
  private val topics = Array(conf.getString("kafka.topic"))
  private val groupId = "SuspiciousClaim_group_id"

  def main(args: Array[String]): Unit = {

    val sparkConf = new SparkConf()
      .setMaster("local[1]")
      .setAppName("SuspiciousMessages")
    val ssc = new StreamingContext(sparkConf, Seconds(2))

    // Create direct kafka stream with brokers and topics
    val topicsSet = topics.toSet
    val kafkaParams = Map[String, Object](
      ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG -> bootstrapServers,
      ConsumerConfig.GROUP_ID_CONFIG -> groupId,
      ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG -> classOf[StringDeserializer],
      ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG -> classOf[StringDeserializer],
      "startingOffsets" -> "earliest")

    val deser = new HL7Deserializer

    val hl7Recs = KafkaUtils.createDirectStream[String, String](
      ssc,
      LocationStrategies.PreferConsistent,
      ConsumerStrategies.Subscribe[String, String](topicsSet, kafkaParams))


    val x = hl7Recs.map {
      _.value().toString
    }

//    val y = x.map(deserialize _)

    //hl7Recs.map(_.value()).print()

    x.print()

    ssc.start()
    ssc.awaitTermination()

  }

  def deserialize(str: String): HL7Record = {
    val saneStr = str.replaceAll("""\Q|\E""", "")
      .replaceAll("""\Q^\E""", "")
      .replaceAll("""\Q~\E""", "")
      .replaceAll("""\Q\\E""", "")
      .replaceAll("""\Q&\E""", "")
    /*
    MSHCPSI_IF_FEED_OUTMurphy Medical Center20091026120921RDE20091026120921P2.3
       EVN2009102612092156KLS
       PID14255038401425503840PYXISTESTPATIENT 219240829MW4130 US HWY 64EMURPHYNC0000028906CHE828837816100000000000000000
       000SOT1425503840999999999N
       PV11I/P00003UCC12D005600HEAVNERTERESAMD100
       5600HEAVNERTERESAMD200910010938
       PV2U20090930000000
       MRG112923
       OBX1ST1010.3Height072Inches
       OBX2ST1010.1Body Weight190.00pounds
       AL199999998No Known Drug Allergies
       DG1A
       ORCXO0000010IP1BID1000,2200,20091015093200
       RXE1BID1000,2200,20091015093200361906PROPRANOLOL 40MG TAB
       (INDERAL)40MGEACHHOLD FOR SBP #lg;90 1
       RXRPO
       NTE
     */
    val patientIdPattern =
      """^PID(\d{10}+)""".r
    val patientIdPattern(patientId) = saneStr
    val drugNamePattern = """\Q(\E([A-Za-z]+)\Q)\E""".r
    val drugNamePattern(drugName) = saneStr
    HL7Record(patientId, drugName)
  }

}

//object XXX