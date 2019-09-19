
scalaVersion := "2.11.12"

name := "hl7fwa"
organization := "jasonnerothin"
version := "1.0"
cancelable in Global := true

cancelable in Global := true

resolvers ++= Seq(
  Resolver.sonatypeRepo("releases"), Resolver.sonatypeRepo("snapshots"),
  "Cloudera Repository" at "https://repository.cloudera.com/artifactory/cloudera-repos/"

)

libraryDependencies += "org.typelevel" %% "cats-core" % "1.4.0" withSources() withJavadoc()
libraryDependencies += "com.typesafe" % "config" % "1.3.2" withSources() withJavadoc()

libraryDependencies += "org.apache.spark" %% "spark-sql" % "2.4.3" withSources() withJavadoc()
libraryDependencies += "org.apache.spark" %% "spark-streaming" % "2.4.3" withSources() withJavadoc()

libraryDependencies += "org.apache.spark" %% "spark-streaming-kafka-0-10" % "2.4.3" withSources() withJavadoc()

libraryDependencies += "org.postgresql" % "postgresql" % "42.2.6" withSources() withJavadoc()

//libraryDependencies += "org.postgresql" % "postgresql" % "9.4.1212"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.0-SNAP10" % Test withSources() withJavadoc()
libraryDependencies += "org.scalacheck" %% "scalacheck" % "1.14.0" % Test withSources() withJavadoc()

// https://mvnrepository.com/artifact/org.apache.spark/spark-sql-kafka-0-10
libraryDependencies += "org.apache.spark" %% "spark-sql-kafka-0-10" % "2.4.4"



assemblyMergeStrategy in assembly := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case x => MergeStrategy.first
}

enablePlugins(JavaAppPackaging)
enablePlugins(DockerPlugin)

