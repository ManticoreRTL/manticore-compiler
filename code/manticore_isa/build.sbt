ThisBuild / organization := "ch.epfl.vlsc"
ThisBuild / version      := "prototype"
ThisBuild / scalaVersion := "2.12.13"


val jvmHeapMemOptions = Seq(
    "-Xms512M", // initial JVM heap pool szie
    "-Xmx8192M", // maximum heap size
    "-Xss32M", // initial stack size
    "-Xsm256M" // maximum stacks size
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
      "org.scala-lang.modules" %% "scala-parser-combinators" % "2.1.0"
      // "com.github.scopt" %% "scopt" % "4.0.1"
    ),


    javaOptions in Test ++= jvmHeapMemOptions,

  )


