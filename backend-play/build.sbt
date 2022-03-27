import play.sbt.routes.RoutesKeys

lazy val root = (project in file("."))
  .enablePlugins(PlayJava)
  .settings(
    name := """backend-play""",
    version := "1.0",
    scalaVersion := "2.13.8",
    libraryDependencies ++= Seq(
      guice,
      javaJdbc,
      javaJpa,
      "org.hibernate" % "hibernate-core" % "5.6.7.Final",
      "com.vladmihalcea" % "hibernate-types-55" % "2.14.1",
      "org.postgresql" % "postgresql" % "42.3.3",
      "org.mapstruct" % "mapstruct" % "1.4.2.Final",
      "com.google.inject" % "guice" % "5.1.0",
      "com.google.inject.extensions" % "guice-assistedinject" % "5.1.0",
      "cz.jirutka.rsql" % "rsql-parser" % "2.1.0",
      "org.awaitility" % "awaitility" % "4.2.0" % Test,
      "org.assertj" % "assertj-core" % "3.22.0" % Test,
      "org.mockito" % "mockito-core" % "4.3.1" % Test,
      // To provide an implementation of JAXB-API, which is required by Ebean.
      "javax.xml.bind" % "jaxb-api" % "2.3.1",
      "javax.activation" % "activation" % "1.1.1",
      "org.glassfish.jaxb" % "jaxb-runtime" % "3.0.2",
      "org.mapstruct" % "mapstruct-processor" % "1.4.2.Final" % Provided
    ),
    Test / testOptions += Tests.Argument(TestFrameworks.JUnit, "-a", "-v"),
    scalacOptions ++= List("-encoding", "utf8", "-deprecation", "-feature", "-unchecked"),
    javacOptions ++= List("-Xlint:unchecked", "-Xlint:deprecation"),
    PlayKeys.externalizeResourcesExcludes += baseDirectory.value / "conf" / "META-INF" / "persistence.xml",
    routesGenerator := RoutesKeys.InjectedRoutesGenerator
  )
