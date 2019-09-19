package com.epam

import java.sql.{Connection, DriverManager, ResultSet, Statement}
import java.util.Properties

import com.typesafe.config.{Config, ConfigFactory}
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}

/**
  * Pipe test messages to Kafka and Postgres
  */
object Feeder extends NapTime with Synonyms with PgUrl {

  private val patientSeed = scala.util.Random
  //private val priceSeed = scala.util.Random
  private val sometimeSeed = scala.util.Random
  private val aliasSeed = scala.util.Random
  private val initialDrugSeed = scala.util.Random

  private var loadToKafka: Boolean = false
  private var loadToDB: Boolean = false


  private val conf: Config = ConfigFactory.load()

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

  private val producer: KafkaProducer[String, String] = initProducer()

  private def initProducer(): KafkaProducer[String, String] = {
    val props = new Properties
    props.put("key.serializer", conf.getString("key.serializer"))
    props.put("value.serializer", conf.getString("value.deserializer"))
    props.put("bootstrap.servers", conf.getString("bootstrap.servers"))
    props.put("kafka.producer.retries", conf.getString("kafka.producer.retries"))
    props.put("kafka.linger.ms", conf.getString("kafka.linger.ms"))
    new KafkaProducer[String, String](props)
  }

  def pgConnection(): Connection = {
    val dbUser = conf.getString("postgres.dbuser")
    val dbPassword = conf.getString("postgres.password")
    val props = new Properties()
    props.setProperty("user", dbUser)
    props.setProperty("password", dbPassword)
    classOf[org.postgresql.Driver]
    org.postgresql.Driver.isRegistered
    DriverManager.getConnection(pgConnectionString()(conf), props)
  }

  def sendToKafka(msg: String): Unit = {
    val record = new ProducerRecord[String, String](conf.getString("kafka.topic"), "key", msg)
    producer.send(record)
  }

  private val logFrequency = 100

  def sendToPostgres(claim: ClaimRecord, statement: Statement): Unit = {
    val qry = s"INSERT INTO claims (id, patient_id, amount) VALUES (${claim.id}, ${claim.patientId}, ${claim.amount})"
    //    val foo: PreparedStatement = null
    //    foo.execute()
    statement.addBatch(qry)
    if (claim.id % logFrequency == 0) println(s"Sending claim id [${claim.id}] to DB.")
    batchCounter = batchCounter + 1
    if (batchCounter > 0 && batchCounter % batchSize == 0) statement.executeBatch()
  }

  private val batchSize = 2048
  private var batchCounter = 0

  def main(args: Array[String]): Unit = {

    if (args.length < 2) {
      print("please provide 2 arguments where first is one of the following: k, kd, d and second is an int")
      return
    }
    if (BigInt(args(1)) >= Int.MaxValue) {
      print("please provide a smaller amount")
      return
    }
    if (args(0) != "k" && args(0) != "d" && args(0) != "kd") {
      print("please provide the first argument as 1 of the following: k, kd, d")
      return
    }

    var id = 0

    if (args(0).contains('k')) loadToKafka = true
    if (args(0).contains('d')) loadToDB = true

    val amount = args(1).toInt

    val connection = pgConnection()
    val statement = connection.createStatement(ResultSet.CLOSE_CURSORS_AT_COMMIT, ResultSet.CONCUR_UPDATABLE)

    while (id < amount) {

      id = id + 1

      originalDrug = getRandomDrug

      val pttId = randomPatientId
      val msg = makeHL7Message("%07d".format(pttId), originalDrug)
      val record = ClaimRecord(id, pttId, stablePrice)

      if (loadToKafka) {
        sendToKafka(msg)

        //Potential problem #1 - drug prescribed twice under different drug names
        if (timeToCreateFraud()) {
          sendToKafka(makeRxeAlias(msg))
          println("sent to kafka")
        }
      }

      if (loadToDB) {
        if (!timeToCreateFraud()) sendToPostgres(record, statement)
        else { // Potential problem #2 - claim is quite expensive...
          println(s"Big claim with id [${record.id}].")
          sendToPostgres(ClaimRecord(record.id, record.patientId, record.amount * 32), statement)
        }
      }

      zzz(2)

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
