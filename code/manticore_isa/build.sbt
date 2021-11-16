ThisBuild / organization := "ch.epfl.vlsc"
ThisBuild / version      := "prototype"
ThisBuild / scalaVersion := "2.12.13"


val jvmHeapMemOptions = Seq(
    "-Xms512M", // initial JVM heap pool szie
    "-Xmx8192M", // maximum heap size
    "-Xss32M", // initial stack size
    "-Xms256M" // maximum stacks size
)


lazy val root = (project in file(".")).
  settings(
    name := "scalatest-example",

    scalacOptions ++= Seq("-feature",
                          "-deprecation",
                          "-unchecked",
                          "-encoding", "utf-8"),

    libraryDependencies ++= Seq(
      "org.scalatest" %% "scalatest" % "3.2.9" % Test,
      "org.scala-lang.modules" %% "scala-parser-combinators" % "2.1.0",
      "ch.qos.logback" % "logback-classic" % "1.2.3", // for logging
      "com.typesafe.scala-logging" %% "scala-logging" % "3.9.4" // for logging
  
    ),


    javaOptions in Test ++= jvmHeapMemOptions,

  )


