ThisBuild / organization := "ch.epfl.vlsc"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "2.13.8"


enablePlugins(JavaAppPackaging)

val jvmHeapMemOptions = Seq(
  "-Xms512M", // initial JVM heap pool size
  "-Xmx8192M", // maximum heap size
  "-Xss32M", // initial stack size
  "-Xms256M" // maximum stacks size
)

val chiselVersion = "3.5.1"



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
    "org.scala-graph" %% "graph-constrained" % "1.13.2", // for testing
    "org.scalatest" %% "scalatest" % "3.2.9" % Test, // scala test
    // we import chisel for integration tests and not the compiler itself
    "edu.berkeley.cs" %% "chisel3" % chiselVersion,
    "edu.berkeley.cs" %% "chiseltest" % "0.5.1" % Test,
    "ch.epfl.vlsc" %% "manticore-machine" % "0.1.0-SNAPSHOT" % Test,
    "org.jgrapht" % "jgrapht-io" % "1.5.1", // graphs
    "org.jgrapht" % "jgrapht-core" % "1.5.1", // for importing/exporting graphs
    "com.google.ortools" % "ortools-java" % "9.3.10497"
  ),
  javaOptions ++= jvmHeapMemOptions,

  // clean generated files by tests
  cleanFiles += baseDirectory.value / "test_run_dir",
  addCompilerPlugin("edu.berkeley.cs" % "chisel3-plugin" % chiselVersion cross CrossVersion.full),
  // package the tests into the jar
  // Test / publishArtifact := true
)



