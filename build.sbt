ThisBuild / organization := "ch.epfl.vlsc"
ThisBuild / version      := "prototype"
ThisBuild / scalaVersion := "2.13.7"


val jvmHeapMemOptions = Seq(
    "-Xms512M", // initial JVM heap pool size
    "-Xmx8192M", // maximum heap size
    "-Xss32M", // initial stack size
    "-Xms256M" // maximum stacks size
)


lazy val root = (project in file(".")).
  settings(
    name := "manticore-asm",

    scalacOptions ++= Seq("-feature",
                          "-deprecation",
                          "-unchecked",
                          "-encoding", "utf-8"),

    libraryDependencies ++= Seq(
      "org.scalatest" %% "scalatest" % "3.2.9" % Test,
      "org.scala-lang.modules" %% "scala-parallel-collections" % "1.0.0",
      "org.scala-lang.modules" %% "scala-parser-combinators" % "2.1.0",
      "ch.qos.logback" % "logback-classic" % "1.2.3", // for logging
      "com.typesafe.scala-logging" %% "scala-logging" % "3.9.4", // for logging
      "com.github.scopt" %% "scopt" % "4.0.1", // cli arg parsing
      "org.scala-graph" %% "graph-core" % "1.13.2", // graphs
      "org.scala-graph" %% "graph-dot" % "1.13.0" // for exporting graphs
    ),


    javaOptions in Test ++= jvmHeapMemOptions,
    parallelExecution in Test := false, // required because Transformations are
    // global objects and once the Logger's error count go up failing a test
    // will cause tests that would pass in isolation to fail...
    // clean generated files by tests
    cleanFiles += baseDirectory.value / "test_dump_dir"

  )


