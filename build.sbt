import java.lang.System._

lazy val buildVersion = {
  val mainVersion = "0.3"
  val minorVersion = Option(getenv("TRAVIS_BUILD_NUMBER"))
  minorVersion match {
    case Some(v: String) ⇒ s"$mainVersion.$v"
    case None ⇒ mainVersion + "-SNAPSHOT"
  }
}

lazy val buildSettings = Seq(
  organization := "com.dwolla",
  name := "scala-aws-utils",
  homepage := Some(url("https://github.com/Dwolla/scala-aws-utils")),
  description := "Utilities for interacting with the AWS SDKs from Scala",
  licenses += ("MIT", url("http://opensource.org/licenses/MIT")),
  version := buildVersion,
  scalaVersion := "2.11.8",
  crossScalaVersions := Seq("2.10.6", "2.11.8"),
  startYear := Option(2016),
  libraryDependencies ++= {
    val awsSdkVersion = "1.11.35"
    val specs2Version = "3.8.5"
    Seq(
      "com.amazonaws"   %  "aws-java-sdk-cloudformation"  % awsSdkVersion,
      "ch.qos.logback"  %  "logback-classic"              % "1.1.7",
      "org.specs2"      %% "specs2-core"                  % specs2Version  % Test,
      "org.specs2"      %% "specs2-mock"                  % specs2Version  % Test
    )
  }
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
