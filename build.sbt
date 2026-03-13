ThisBuild / version      := "0.1.0"
ThisBuild / scalaVersion := "3.3.1"
ThisBuild / organization := "autocompletion"

lazy val root = (project in file("."))
  .settings(
    name := "scala_autocompletion_texte",
    libraryDependencies ++= Seq(
      "org.scalameta" %% "munit" % "0.7.29" % Test
    ),
    testFrameworks += new TestFramework("munit.Framework")
  )
