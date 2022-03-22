ThisBuild / organization := "ch.epfl.vlsc"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "2.13.8"

val jvmHeapMemOptions = Seq(
  "-Xms512M", // initial JVM heap pool size
  "-Xmx8192M", // maximum heap size
  "-Xss32M", // initial stack size
  "-Xms256M" // maximum stacks size
)

val chiselVersion = "3.5.1"

// lazy val root = (project in file(".")).aggregate(compiler)
lazy val compiler = (project in file(".")).settings(
  name := "manticore-compiler",
  scalacOptions ++= Seq(
    "-feature",
    "-deprecation",
    "-unchecked",
    "-encoding",
    "utf-8"
  ),
  libraryDependencies ++= Seq(
    "org.scala-lang.modules" %% "scala-parallel-collections" % "1.0.0",
    "org.scala-lang.modules" %% "scala-parser-combinators" % "2.1.0",
    "com.github.scopt" %% "scopt" % "4.0.1", // cli arg parsing
    "org.scala-graph" %% "graph-core" % "1.13.2", // graphs
    "org.scala-graph" %% "graph-dot" % "1.13.0", // for exporting graphs
    "org.scalatest" %% "scalatest" % "3.2.9" % Test, // scala test
    // we import chisel for integration tests and not the compiler itself
    "edu.berkeley.cs" %% "chisel3" % chiselVersion % Test,
    "edu.berkeley.cs" %% "chiseltest" % "0.5.1" % Test,
    "ch.epfl.vlsc" %% "manticore-machine" % "0.1.0-SNAPSHOT" % Test,
    "net.java.dev.jna" % "jna" % "5.10.0" % Test
  ),
  javaOptions ++= jvmHeapMemOptions,

  // clean generated files by tests
  cleanFiles += baseDirectory.value / "test_run_dir",

  // package the tests into the jar
  Test / publishArtifact := true
)



