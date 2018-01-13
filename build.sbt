
import sbt._

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

lazy val akkaDependencies = Seq(
  "com.typesafe.akka"        %% "akka-actor"                 % "2.4.19",
  "com.typesafe.akka"        %% "akka-slf4j"                 % "2.4.19",
  "com.typesafe.akka"        %% "akka-stream-kafka"          % "0.16",
  "org.json4s"               %% "json4s-jackson"             % "3.2.11"
)

lazy val kafkaDependencies = Seq(
  "org.apache.kafka"         %% "kafka"                      % "0.10.2.1",
  "org.apache.kafka"         % "kafka-clients"               % "0.10.2.1"
)

lazy val logDependencies = Seq(
  "org.slf4j"                % "slf4j-api"                    % "1.7.18"
)

lazy val twitterDependencies = Seq(
  "org.twitter4j"            % "twitter4j-core"               % "3.0.3",
  "org.twitter4j"            % "twitter4j-stream"             % "3.0.3"
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
    name := "Ingest",
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

lazy val ingest = (project in file("akka-ingest")).
  settings(commonSettings: _*).
  settings(
    name := "akka-ingest",
    scalaVersion := "2.11.8",
    libraryDependencies ++= akkaDependencies,
    libraryDependencies ++= kafkaDependencies,
    libraryDependencies ++= twitterDependencies,
    mainClass in (Compile,run) := Some("ch.presland.data.stream.TweetStreamer")
  ).dependsOn(commons)

addCommandAlias("ingest", "ingest/run")
