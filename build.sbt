lazy val buildSettings = Seq(
  organization := "com.dwolla",
  name := "scala-aws-utils",
  homepage := Some(url("https://github.com/Dwolla/scala-aws-utils")),
  description := "Utilities for interacting with the AWS SDKs from Scala",
  licenses += ("MIT", url("http://opensource.org/licenses/MIT")),
  version := "1.1.0",
  scalaVersion := "2.12.1",
  crossScalaVersions := Seq("2.10.6", "2.11.8", "2.12.1"),
  startYear := Option(2016),
  libraryDependencies ++= {
    val awsSdkVersion = "1.11.75"
    val specs2Version = "3.8.6"
    Seq(
      "com.amazonaws"   %  "aws-java-sdk-cloudformation"  % awsSdkVersion,
      "ch.qos.logback"  %  "logback-classic"              % "1.1.7",
      "org.specs2"      %% "specs2-core"                  % specs2Version  % Test,
      "org.specs2"      %% "specs2-mock"                  % specs2Version  % Test
    )
  },
  scalacOptions += "-deprecation"
)

lazy val bintraySettings = Seq(
  bintrayVcsUrl := Some("https://github.com/Dwolla/scala-aws-utils"),
  publishMavenStyle := false,
  bintrayRepository := "maven",
  bintrayOrganization := Option("dwolla"),
  pomIncludeRepository := { _ ⇒ false }
)

lazy val scalaAwsUtils = (project in file("."))
  .settings(buildSettings ++ bintraySettings: _*)

lazy val pipeline = TaskKey[Unit]("pipeline", "CI Pipeline")
pipeline <<= test in Test
