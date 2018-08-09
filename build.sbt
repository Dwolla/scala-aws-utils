lazy val primaryName = "scala-aws-utils"

lazy val commonSettings = Seq(
  organization := "com.dwolla",
  homepage := Some(url("https://github.com/Dwolla/scala-aws-utils")),
  licenses += ("MIT", url("http://opensource.org/licenses/MIT")),
  releaseVersionBump := sbtrelease.Version.Bump.Minor,
  releaseCrossBuild := true,
  startYear := Option(2016),
  libraryDependencies ++= {
    val awsSdkVersion = "1.11.331"
    val specs2Version = "3.10.0"

    Seq(
      "com.amazonaws"   %  "aws-java-sdk-core"            % awsSdkVersion,
      "com.amazonaws"   %  "aws-java-sdk-cloudformation"  % awsSdkVersion % Provided,
      "com.amazonaws"   %  "aws-java-sdk-kms"             % awsSdkVersion % Provided,
      "ch.qos.logback"  %  "logback-classic"              % "1.2.3",
      "org.specs2"      %% "specs2-core"                  % specs2Version % Test,
      "org.specs2"      %% "specs2-mock"                  % specs2Version % Test,
      "com.amazonaws"   %  "aws-java-sdk-ecs"             % awsSdkVersion % Test
    )
  },
  scalacOptions += "-deprecation"
)

lazy val bintraySettings = Seq(
  bintrayPackage := primaryName,
  bintrayVcsUrl := homepage.value.map(_.toString),
  bintrayRepository := "maven",
  bintrayOrganization := Option("dwolla"),
  pomIncludeRepository := { _ ⇒ false }
)

lazy val scalaAwsUtils = (project in file("."))
  .settings({
    Seq(
      name := primaryName,
      description := "Utilities for interacting with the AWS SDKs from Scala"
    )
  } ++ commonSettings ++ bintraySettings: _*)
  .dependsOn(testkit % Test)
  .aggregate(testkit)

lazy val testkit = (project in file("testkit"))
  .settings(Seq(
    name := primaryName + "-testkit",
    description := "Test utilities for interacting with the AWS SDKs from Scala",
    libraryDependencies ++= {
      Seq(
        "org.mockito"   %  "mockito-core"                % "1.9.5"
      )
    }
  ) ++ commonSettings ++ bintraySettings: _*)
