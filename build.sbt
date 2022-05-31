inThisBuild(List(
  organization := "com.dwolla",
  homepage := Some(url("https://github.com/Dwolla/scala-aws-utils")),
  licenses += ("MIT", url("http://opensource.org/licenses/MIT")),
  developers := List(
    Developer(
      "bpholt",
      "Brian Holt",
      "bholt+github@dwolla.com",
      url("https://dwolla.com")
    ),
  ),
  crossScalaVersions := Seq("2.13.5", "2.12.13"),
  scalaVersion := crossScalaVersions.value.head,
  startYear := Option(2016),
  addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.11.3" cross CrossVersion.full),
  addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1"),
  libraryDependencies ++= {
    val awsSdkVersion = "1.12.217"
    val specs2Version = "4.10.6"

    Seq(
      "com.amazonaws"           %  "aws-java-sdk-core"            % awsSdkVersion,
      "com.amazonaws"           %  "aws-java-sdk-cloudformation"  % awsSdkVersion % Provided,
      "com.amazonaws"           %  "aws-java-sdk-kms"             % awsSdkVersion % Provided,
      "ch.qos.logback"          %  "logback-classic"              % "1.2.3",
      "org.scala-lang.modules"  %% "scala-collection-compat"      % "2.4.3",
      "org.specs2"              %% "specs2-core"                  % specs2Version % Test,
      "org.specs2"              %% "specs2-mock"                  % specs2Version % Test,
      "com.amazonaws"           %  "aws-java-sdk-ecs"             % awsSdkVersion % Test
    )
  },

  githubWorkflowJavaVersions := Seq("adopt@1.8", "adopt@1.11"),
  githubWorkflowTargetTags ++= Seq("v*"),
  githubWorkflowPublishTargetBranches :=
    Seq(RefPredicate.StartsWith(Ref.Tag("v"))),
  githubWorkflowPublish := Seq(
    WorkflowStep.Sbt(
      List("ci-release"),
      env = Map(
        "PGP_PASSPHRASE" -> "${{ secrets.PGP_PASSPHRASE }}",
        "PGP_SECRET" -> "${{ secrets.PGP_SECRET }}",
        "SONATYPE_PASSWORD" -> "${{ secrets.SONATYPE_PASSWORD }}",
        "SONATYPE_USERNAME" -> "${{ secrets.SONATYPE_USERNAME }}"
      )
    )
  ),
))

lazy val primaryName = "scala-aws-utils"

lazy val scalaAwsUtils = (project in file("."))
  .settings(
    name := primaryName,
    description := "Utilities for interacting with the AWS SDKs from Scala",
    scalacOptions += "-language:reflectiveCalls",
  )
  .dependsOn(testkit % Test)
  .aggregate(testkit)

lazy val testkit = (project in file("testkit"))
  .settings(
    name := primaryName + "-testkit",
    description := "Test utilities for interacting with the AWS SDKs from Scala",
    libraryDependencies ++= {
      Seq(
        "org.mockito" % "mockito-core" % "3.9.0"
      )
    },
  )
