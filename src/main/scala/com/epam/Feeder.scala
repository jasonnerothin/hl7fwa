package com.epam

import java.sql.ResultSet

import com.epam.helper.{KafkaHelper, PostgreSQLHelper}
import com.typesafe.config.{Config, ConfigFactory}

/**
  * Pipe test messages to Kafka and Postgres
  */
object Feeder extends NapTime with Synonyms {

  private val patientSeed = scala.util.Random
  //private val priceSeed = scala.util.Random
  private val sometimeSeed = scala.util.Random
  private val aliasSeed = scala.util.Random
  private val initialDrugSeed = scala.util.Random

  private var loadToKafka: Boolean = false
  private var loadToDB: Boolean = false

  private val NUM_PATIENTS: Int = 2E20.toInt
  private val DUPLICATE_FREQUENCY = 512

  private def randomPatientId = patientSeed.nextInt(NUM_PATIENTS) + 1

  /**
    * @return a Float between 10 and 320, inclusive
    */
  //private def randomPrice: Float = ((priceSeed.nextInt(32) + 1) * 10).toFloat
  private val stablePrice = 42

  def makeHL7Message(patientId: String, drugName: String): String = {
    bigMessageFormat.replaceAll("XXXXXX", patientId).replaceAll("DDDDDDDDDDDDDDDDDD", drugName)
  }

  def makeRxeAlias(msg: String): String = {
    val synonyms: Set[String] = of(originalDrug).filter(_ != originalDrug)
    require(synonyms.nonEmpty, "Error finding synonym for " + originalDrug)
    msg.replaceAll(originalDrug, synonyms.toList(aliasSeed.nextInt(synonyms.size)))
  }

  def getRandomDrug: String = {
    val drugs: Set[String] = getAllDrugNames
    drugs.toVector(initialDrugSeed.nextInt(drugs.size))
  }


  def timeToCreateFraud(): Boolean = {
    sometimeSeed.nextInt(DUPLICATE_FREQUENCY) == 0
  }

  def main(args: Array[String]): Unit = {

    if (args.length < 8) {
      print("please provide 8 arguments:\n k/d/kd kafkaServers kafkaTopic postgresHost postgresPort postgresDBname postgresDbuser postgresPassword")
      return
    }
    if (args(0) != "k" && args(0) != "d" && args(0) != "kd") {
      print("please provide the first argument as 1 of the following: k, kd, d")
      return
    }

    val kafkaServers = args(1)
    val kafkaTopic = args(2)
    val kafkaHelper: KafkaHelper = new KafkaHelper(kafkaServers, kafkaTopic)


    val postgresHost = args(3)
    val postgresPort = args(4)
    val postgresDBname = args(5)
    val postgresDbuser = args(6)
    val postgresPassword = args(7)
    val postgreSQLHelper = new PostgreSQLHelper(postgresHost, postgresPort, postgresDBname, postgresDbuser, postgresPassword)


    var id = 0

    if (args(0).contains('k')) loadToKafka = true
    if (args(0).contains('d')) loadToDB = true

    val connection = postgreSQLHelper.connection
    val statement = connection.createStatement(ResultSet.CLOSE_CURSORS_AT_COMMIT, ResultSet.CONCUR_UPDATABLE)

    while (true) {

      id = id + 1

      originalDrug = getRandomDrug

      val pttId = randomPatientId

      val msg = makeHL7Message("%07d".format(pttId), originalDrug)
      val record = ClaimRecord(id, pttId, stablePrice)
      if (loadToKafka) {
        kafkaHelper.sendToKafka(msg)

        //Potential problem #1 - drug prescribed twice under different drug names
        if (timeToCreateFraud()) {
          kafkaHelper.sendToKafka(makeRxeAlias(msg))
          println("sent to kafka")
        }
      }

      if (loadToDB) {
        if (!timeToCreateFraud()) postgreSQLHelper.sendToPostgres(record, statement)
        else { // Potential problem #2 - claim is quite expensive...
          println(s"Big claim with id [${record.id}].")
          postgreSQLHelper.sendToPostgres(ClaimRecord(record.id, record.patientId, record.amount * 32), statement)
        }
      }

      zzz(2000)

    }

    statement.executeBatch()
    statement.close()
    connection.close()

  }

  private var originalDrug: String = "PROPRANOLOL"

  val messageFormat =
    """|PID||XXXXXX|XXXXXX||PYXIS TEST PATIENT 2||
       |RXE||(INDERAL)|40||MG|EACH|HOLD FOR SBP lg 90 |||1||||||||||||||"""


  val bigMessageFormat =
    """|MSH|^~\&|CPSI_IF_FEED_OUT|Murphy Medical Center|||20091026120921||RDE|20091026120921|P|2.3||
       |EVN||2009102612092156|||KLS
       |PID||XXXXXX|XXXXXX||PYXIS^TEST^PATIENT 2||19240829|M||W|4130 US HWY 64E^^MURPHY^NC^0000028906|CHE|8288378161^^^^^0000000000|0000000
       |000||S|OT|XXXXXX|999999999|||||||||||N
       |PV1||1^I/P^00|003^UCC12^|D|||005600^HEAVNER^TERESA^MD|^^^|^^^|1|||||||00
       |5600^HEAVNER^TERESA^MD|||||||||||||||||||||||||||200910010938|
       |PV2|||||||U|20090930000000|||||||||||||||||||
       |MRG|112923
       |OBX|1|ST|1010.3^Height||072|Inches
       |OBX|2|ST|1010.1^Body Weight||190.00|pounds
       |AL1|||99999998^No Known Drug Allergies
       |DG1||||||A
       |ORC|XO|0000010|||IP||1^BID&1000,2200,^^200910150932^^0^0^
       |RXE|1^BID&1000,2200,^^200910150932^^0^0^|361906^DDDDDDDDDDDDDDDDDD 40MG TAB
       |(INDERAL)|40||MG|EACH|HOLD FOR SBP #lg;90 |||1||||||||||||||
       |RXR|^PO
       |NTE|||"""


  """
    |PID => PatientID
    |RXE, RDE => Drug Records
    |ADT => Admission, Discharge, T(ransfer?)
  """.stripMargin
}
