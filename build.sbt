import com.typesafe.sbt.packager.SettingsHelper.makeDeploymentSettings

organization := "com.rbmhtechnology"

name := "eventuate-chaos"

version := "0.1-SNAPSHOT"

scalaVersion := "2.11.7"

// put bintray resolver at first pos to avoid problems with typesafe resolver
// see also: https://github.com/mohiva/play-silhouette-seed/issues/20
resolvers := Seq(
  "Eventuate Releases" at "https://dl.bintray.com/rbmhtechnology/maven",
  "OJO Snapshots" at s"https://oss.jfrog.org/oss-snapshot-local"
) ++ resolvers.value

val eventuateVersion = "0.9"

libraryDependencies ++= Seq(
  "com.rbmhtechnology" %% "eventuate-core"          % eventuateVersion,
  "com.rbmhtechnology" %% "eventuate-log-leveldb"   % eventuateVersion,
  "com.rbmhtechnology" %% "eventuate-log-cassandra" % eventuateVersion,
  "com.rbmhtechnology" %% "eventuate-crdt"          % eventuateVersion,
  "org.slf4j"           % "slf4j-log4j12"           % "1.7.9"  % "test"
)

enablePlugins(JavaAppPackaging)

publishArtifact in (Compile, packageDoc) := false

publishArtifact in (Test, packageDoc) := false

mainClass in Compile := Some("com.rbmhtechnology.eventuate.chaos.ChaosCounterCassandra")

makeDeploymentSettings(Universal, packageBin in Universal, "zip")

publish <<= publish dependsOn (publish in Universal)

publishLocal <<= publishLocal dependsOn (publishLocal in Universal)

val runNobootcp =
  InputKey[Unit]("run-nobootcp", "Runs main classes without Scala library on the boot classpath")

runNobootcp <<= runNobootcpInputTask(Runtime)
runNobootcp <<= runNobootcpInputTask(Test)

def runNobootcpInputTask(configuration: Configuration) = inputTask {
  (argTask: TaskKey[Seq[String]]) => (argTask, streams, fullClasspath in configuration) map { (at, st, cp) =>
    val runCp = cp.map(_.data).mkString(java.io.File.pathSeparator)
    val runOpts = Seq("-classpath", runCp) ++ at
    val result = Fork.java.fork(None, runOpts, None, Map(), false, LoggedOutput(st.log)).exitValue()
    if (result != 0) error("Run failed")
  }
}
