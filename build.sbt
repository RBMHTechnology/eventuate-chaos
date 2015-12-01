organization := "com.rbmhtechnology"

name := "eventuate-chaos"

version := "0.1-SNAPSHOT"

scalaVersion := "2.11.7"

resolvers += "OJO Snapshots" at "https://oss.jfrog.org/oss-snapshot-local"

libraryDependencies ++= Seq(
  "com.typesafe.akka"  %% "akka-actor"    % "2.4-M2",
  "com.typesafe.akka"  %% "akka-remote"   % "2.4-M2"       % "test",
  "com.rbmhtechnology" %% "eventuate"     % "0.5-SNAPSHOT" % "test",
  "org.slf4j"           % "slf4j-log4j12" % "1.7.9"        % "test"
)
