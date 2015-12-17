organization := "com.rbmhtechnology"

name := "eventuate-chaos"

version := "0.1-SNAPSHOT"

scalaVersion := "2.11.7"

resolvers += "OJO Snapshots" at "https://oss.jfrog.org/oss-snapshot-local"

libraryDependencies ++= Seq(
  "com.typesafe.akka"  %% "akka-actor"    % "2.4-M2",
  "com.typesafe.akka"  %% "akka-remote"   % "2.4-M2"       % "test",
  "com.rbmhtechnology" %% "eventuate"     % "0.5-chaos-SNAPSHOT" % "test",
  "org.slf4j"           % "slf4j-log4j12" % "1.7.9"        % "test"
)

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
