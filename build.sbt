
import sbt._
import sbt.Keys._
import sbtassembly.MergeStrategy


libraryDependencies += "org.apache.spark" %% "spark-core" % "2.2.1" % "provided"

assemblyOption in assembly := (assemblyOption in assembly).value.copy(includeScala = false)

lazy val compileOptions = Seq(
  "-unchecked",
  "-deprecation",
  "-language:_",
  "-target:jvm-1.8",
  "-encoding", "UTF-8",
  "-Xcheckinit"
)

lazy val commonDependencies = Seq(
  "org.scalatest" %% "scalatest" % "3.0.1" % "test"
)

lazy val twitterDependencies = Seq(
  "org.twitter4j"            % "twitter4j-core"               % "3.0.3",
  "org.twitter4j"            % "twitter4j-stream"             % "3.0.3"
)

lazy val akkaDependencies = Seq(
  "com.typesafe.akka"        %% "akka-actor"                 % "2.4.19",
  "com.typesafe.akka"        %% "akka-slf4j"                 % "2.4.19",
  "com.typesafe.akka"        %% "akka-stream-kafka"          % "0.16",
  "com.typesafe.akka"        %% "akka-http"                  % "10.1.0-RC1",
  "com.typesafe.akka"        %% "akka-http-spray-json"       % "10.1.0-RC1",
  "io.spray"                 %% "spray-can"                  % "1.3.4",
  "io.spray"                 %% "spray-client"               % "1.3.4",
  "io.spray"                 %% "spray-routing"              % "1.3.4",
  "io.spray"                 %% "spray-json"                 % "1.3.4",
  "org.json4s"               %% "json4s-jackson"             % "3.2.11",
  "com.fasterxml.jackson.module" % "jackson-module-scala_2.11"    % "2.6.1"
)

lazy val kafkaDependencies = Seq(
  "org.apache.kafka"         %% "kafka"                      % "0.11.0.1"
)

lazy val sparkDependencies = Seq(
  "org.apache.spark"           %% "spark-core"                           % "2.2.0",
  "org.apache.spark"           %% "spark-streaming"                      % "2.2.0",
  "org.apache.spark"           %% "spark-streaming-kafka-0-10"           % "2.2.0",
  "com.datastax.spark"         %% "spark-cassandra-connector"            % "2.0.1",
  "org.apache.spark"           %% "spark-sql"                            % "2.2.0"

)

lazy val coreNlpDependencies = Seq(
  "edu.stanford.nlp" % "stanford-corenlp" % "3.6.0",
  "edu.stanford.nlp" % "stanford-corenlp" % "3.6.0" classifier "models"
)

lazy val logDependencies = Seq(
  "org.slf4j"                % "slf4j-api"                    % "1.7.18"
)



lazy val commonSettings = Seq(
  organization := "ch.presland.data",
  scalacOptions ++= compileOptions,
  libraryDependencies ++= commonDependencies,
  libraryDependencies ++= logDependencies,
  licenses := Seq("Apache-2.0" -> url("https://opensource.org/licenses/Apache-2.0"))
)

lazy val root = (project in file(".")).
  settings(commonSettings: _*).
  settings(
    name := "tweetstream",
    scalaVersion := "2.11.8"
  ).
  aggregate(commons, ingest)

lazy val commons = (project in file("commons")).
  settings(commonSettings: _*).
  settings(
    name := "commons",
    scalaVersion := "2.11.8",
    libraryDependencies ++= kafkaDependencies
  )

lazy val ingest = (project in file("ingestion")).
  settings(commonSettings: _*).
  settings(
    name := "ingestion",
    scalaVersion := "2.11.8",
    libraryDependencies ++= akkaDependencies,
    libraryDependencies ++= kafkaDependencies,
    libraryDependencies ++= twitterDependencies,
    mainClass in (Compile, run) := Some("ch.presland.data.stream.TweetIngestor")
  ).dependsOn(commons)

lazy val digest = (project in file("digestion")).
  settings(commonSettings: _*).
  settings(
    name := "digestion",
    scalaVersion := "2.11.8",
    libraryDependencies ++= akkaDependencies,
    libraryDependencies ++= kafkaDependencies,
    libraryDependencies ++= sparkDependencies,
    libraryDependencies ++= twitterDependencies,
    libraryDependencies ++= coreNlpDependencies,
    mainClass in (Compile,run) := Some("ch.presland.data.stream.TweetDigestor"),

    assemblyMergeStrategy in assembly := {
      case PathList("de", xs @_ * ) => MergeStrategy.first
      case PathList("com", "datastax", "driver", xs @_ *) => MergeStrategy.first
      case PathList("org", "apache", "flink", "streaming", "connectors", xs @_ *) => MergeStrategy.first
      case PathList("org", "apache", "flink", "streaming", "util", xs @_ *) => MergeStrategy.first
      case PathList("org", "apache", "kafka", "common", xs @_ *) => MergeStrategy.first
      case PathList("org", "nustaq", xs @_ *) => MergeStrategy.first
      case PathList("org", "apache", "flink", "batch", "connectors", "cassandra", xs @_ *) => MergeStrategy.first
      case PathList("org", "apache", "flink", "batch", "connectors", "cassandra", xs @_ *) => MergeStrategy.first
      case PathList("org", "apache", "flink", "cassandra", "shaded", xs @_ *) => MergeStrategy.first
      case PathList("org", "apache", "kafka", "clients", xs @_ *) => MergeStrategy.first
      case PathList("com", "codahale", "metrics", xs @_ *) => MergeStrategy.first
      case PathList("org", "joda", xs @_ *) => MergeStrategy.first
      case PathList("breeze", "linalg", xs @_ * ) => MergeStrategy.first
      case PathList(xs @_ * ) => MergeStrategy.discard
      case x =>
        val oldStrategy = (assemblyMergeStrategy in assembly).value
        oldStrategy(x)
    },
    artifact in (Compile, assembly) := {
      val art = (artifact in (Compile, assembly)).value
      art.withClassifier(Some("assembly"))
    },
    addArtifact(artifact in (Compile, assembly), assembly)

  ).dependsOn(commons)

val srvProject = "server"

lazy val server = (project in file(srvProject)).
  settings(commonSettings: _*).
  settings(
    name := srvProject,
    scalaVersion := "2.11.8",
    libraryDependencies ++= akkaDependencies,
    libraryDependencies ++= sparkDependencies,
    mainClass in (Compile,run) := Some("ch.presland.data.stream.TweetServer"),
    artifact in (Compile, assembly) := {
      val art = (artifact in (Compile, assembly)).value
      art.withClassifier(Some("assembly"))
    },
    addArtifact(artifact in (Compile, assembly), assembly)
  ).dependsOn(commons)

addCommandAlias("createDigest", "digest/assembly")
addCommandAlias("submitDigest", "digest/sparkSubmit --master local[2] --class ch.presland.data.stream.TweetDigestor -- localhost:9042")

addCommandAlias("ingest", "ingest/run")
addCommandAlias("digest", "digest/run")
addCommandAlias("server", "server/run")
