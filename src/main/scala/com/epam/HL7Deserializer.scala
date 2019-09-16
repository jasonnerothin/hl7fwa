package com.epam

import java.util

import com.epam.jobs.HL7Record
import org.apache.kafka.common.serialization.Deserializer

/**
  */
class HL7Deserializer extends Deserializer[HL7Record] {

  override def configure(map: util.Map[String, _], b: Boolean): Unit = {}

  override def deserialize(topic: String, data: Array[Byte]): HL7Record = {
    val str = new java.lang.String(data)
    val drugNamePattern = "^\\(([A-Za-z]+)\\)".r
    val patientIdPattern = "^|PID||([0-9]+)|".r
    val patientIdPattern(patientId) = str
    val drugNamePattern(drugName) = str
    HL7Record(patientId, drugName)
  }

  override def close(): Unit = {}

}