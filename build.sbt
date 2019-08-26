
scalaVersion := "2.11.12"

name := "hl7fwa"
organization := "jasonnerothin"
version := "1.0"

resolvers ++= Seq(
  Resolver.sonatypeRepo("releases"), Resolver.sonatypeRepo("snapshots")
)

libraryDependencies += "org.typelevel" %% "cats-core" % "1.4.0" withSources() withJavadoc()
libraryDependencies += "com.typesafe" % "config" % "1.3.2" withSources() withJavadoc()

libraryDependencies += "org.apache.spark" %% "spark-sql" % "2.4.0" % Provided withSources() withJavadoc()

libraryDependencies += "org.apache.spark" %% "spark-sql-kafka-0-10" % "2.4.0" withSources() withJavadoc()
libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.0-SNAP10" % Test withSources() withJavadoc()
libraryDependencies += "org.scalacheck" %% "scalacheck" % "1.14.0" % Test withSources() withJavadoc()

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case x => MergeStrategy.first
}
