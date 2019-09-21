package com.epam.helper

import java.util.Properties

import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}

class KafkaHelper(bootstrapServers: String, kafkaTopic: String) {

  val keySerializer: String = "org.apache.kafka.common.serialization.StringSerializer"
  val valueSerializer: String = "org.apache.kafka.common.serialization.StringSerializer"
  val valueDeserializer: String = "org.apache.kafka.common.serialization.StringSerializer"

  //val bootstrapServers:String="localhost:9092"
  //val kafkaTopic:String="MyPreciousTopic2"
  val kafkaProducerRetries: String = "3"
  val kafkaLingerMs: String = "3"
  val kafkaCompressionType: String = "snappy"

  val props = new Properties
  props.put("key.serializer", keySerializer)
  props.put("value.serializer", valueDeserializer)
  props.put("bootstrap.servers", bootstrapServers)
  props.put("kafka.producer.retries", kafkaProducerRetries)
  props.put("kafka.linger.ms", kafkaLingerMs)

  val producer = new KafkaProducer[String, String](props)


  def sendToKafka(msg: String): Unit = {
    print("in loading")
    val record = new ProducerRecord[String, String](kafkaTopic, "key", msg)
    producer.send(record)
  }

}
