organization := "com.rbmhtechnology"

name := "eventuate-chaos"

version := "0.1-SNAPSHOT"

scalaVersion := "2.11.7"

resolvers += "Eventuate Releases" at "https://dl.bintray.com/rbmhtechnology/maven"

libraryDependencies ++= Seq(
  "com.typesafe.akka"  %% "akka-actor"    % "2.4-M2",
  "com.rbmhtechnology" %% "eventuate"     % "0.2.1" % "test",
  "org.slf4j"           % "slf4j-log4j12" % "1.7.9" % "test"
)
