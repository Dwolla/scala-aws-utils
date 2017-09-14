lazy val primaryName = "scala-aws-utils"
lazy val specs2Version = "3.8.9"
lazy val awsSdkVersion = "1.11.193"

lazy val commonSettings = Seq(
  organization := "com.dwolla",
  homepage := Some(url("https://github.com/Dwolla/scala-aws-utils")),
  licenses += ("MIT", url("http://opensource.org/licenses/MIT")),
  releaseVersionBump := sbtrelease.Version.Bump.Minor,
  releaseProcess := {
    import ReleaseTransformations._
    Seq[ReleaseStep](
      checkSnapshotDependencies,
      inquireVersions,
      setReleaseVersion,
      commitReleaseVersion,
      tagRelease,
      setNextVersion,
      commitNextVersion,
      pushChanges
    )
  },
  scalaVersion := "2.12.3",
  crossScalaVersions := Seq("2.11.11", "2.12.3"),
  startYear := Option(2016),
  scalacOptions ++= Seq(
    "-deprecation",
  )
)

lazy val bintraySettings = Seq(
  bintrayPackage := primaryName,
  bintrayVcsUrl := homepage.value.map(_.toString),
  publishMavenStyle := true,
  bintrayRepository := "maven",
  bintrayOrganization := Option("dwolla"),
  pomIncludeRepository := { _ ⇒ false }
)

lazy val dontPublishSettings = Seq(
  publishArtifact := false,
  publishTo := Some("fake-repo" at "fake-uri")
)

lazy val scalaAwsUtils = (project in file("scalaAwsUtils"))
  .settings({
    Seq(
      name := primaryName,
      description := "Utilities for interacting with the AWS SDKs from Scala",
      libraryDependencies ++= {
        Seq(
          "com.amazonaws"   %  "aws-java-sdk-core"            % awsSdkVersion,
          "com.amazonaws"   %  "aws-java-sdk-cloudformation"  % awsSdkVersion % Provided,
          "com.amazonaws"   %  "aws-java-sdk-kms"             % awsSdkVersion % Provided,
          "org.specs2"      %% "specs2-core"                  % specs2Version % Test,
          "org.specs2"      %% "specs2-mock"                  % specs2Version % Test,
          "com.amazonaws"   %  "aws-java-sdk-ecs"             % awsSdkVersion % Test,
        )
      },
    )
  } ++ commonSettings ++ bintraySettings: _*)
  .dependsOn(core)
  .dependsOn(testkit % Test)

lazy val core = (project in file("core"))
  .settings({
    Seq(
      name := primaryName + "-core",
      description := "Shared types for all scala-aws-utils submodules",
      libraryDependencies ++= {
        Seq(
          "com.amazonaws"   %  "aws-java-sdk-core"            % awsSdkVersion,
        )
      },
    )
  } ++ commonSettings ++ bintraySettings: _*)

lazy val fs2AwsUtils = (project in file("fs2AwsUtils"))
  .settings({
    Seq(
      name := primaryName + "-fs2",
      description := "Utilities for interacting with the AWS SDKs from Scala using fs2",
      libraryDependencies ++= {
        Seq(
          "com.amazonaws"   %  "aws-java-sdk-core"            % awsSdkVersion,
          "com.amazonaws"   %  "aws-java-sdk-cloudformation"  % awsSdkVersion,
          "co.fs2"          %% "fs2-core"                     % "0.10.0-M6",
        )
      },
    )
  } ++ commonSettings ++ bintraySettings: _*)
  .dependsOn(core)
  .dependsOn(testkit % Test)

lazy val testkit = (project in file("testkit"))
  .settings(Seq(
    name := primaryName + "-testkit",
    description := "Test utilities for interacting with the AWS SDKs from Scala",
    libraryDependencies ++= {
      Seq(
        "com.amazonaws"   %  "aws-java-sdk-core"            % awsSdkVersion,
        "org.specs2"      %% "specs2-mock"                  % specs2Version,
      )
    }
  ) ++ commonSettings ++ bintraySettings: _*)

lazy val root = (project in file("."))
  .settings(commonSettings)
  .settings(dontPublishSettings)
  .aggregate(core, scalaAwsUtils, fs2AwsUtils, testkit)
