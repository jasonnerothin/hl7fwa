package com.epam.helper

import java.sql.{Connection, DriverManager, PreparedStatement, Statement}
import java.util.Properties

import com.epam.ClaimRecord


class PostgreSQLHelper(postgresHost: String, postgresPort: String, postgresDBname: String, postgresDbuser: String,
                       postgresPassword: String) {

  val dbUser: String = postgresDbuser
  val dbPassword: String = postgresPassword
  val props = new Properties()
  props.setProperty("user", dbUser)
  props.setProperty("password", dbPassword)
  classOf[org.postgresql.Driver]
  org.postgresql.Driver.isRegistered
  val connection: Connection = DriverManager.getConnection(pgConnectionString(postgresHost, postgresPort, postgresDBname), props)

  private val logFrequency = 100
  private val batchSize = 5
  private var batchCounter = 0

  def pgConnectionString(pghost:String, pgport:String, pgdbname:String): String = {
    val host = pghost
    val port = pgport
    val dbName = pgdbname
    s"jdbc:postgresql://$host:$port/$dbName"
  }

  def sendToPostgres(claim: ClaimRecord, statement: Statement): Unit = {
    val qry = s"INSERT INTO claims (id, patient_id, amount) VALUES (${claim.id}, ${claim.patientId}, ${claim.amount})"
    //val foo: PreparedStatement = null
    //foo.execute()
    statement.addBatch(qry)
    if (claim.id % logFrequency == 0) println(s"Sending claim id [${claim.id}] to DB.")
    batchCounter = batchCounter + 1
    if (batchCounter > 0 && batchCounter % batchSize == 0) statement.executeBatch()
  }


}
