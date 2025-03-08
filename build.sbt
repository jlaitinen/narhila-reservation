val scala3Version = "3.3.1"

lazy val root = project
  .in(file("."))
  .aggregate(backend, frontend, shared.jvm, shared.js)
  .settings(
    name := "cottage-reservation-service",
    version := "0.1.0",
    scalaVersion := scala3Version
  )

lazy val backend = project
  .in(file("backend"))
  .settings(
    name := "cottage-reservation-backend",
    version := "0.1.0",
    scalaVersion := scala3Version,
    
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-core" % "2.10.0",
      "org.typelevel" %% "cats-effect" % "3.5.2",
      
      "org.http4s" %% "http4s-ember-server" % "0.23.24",
      "org.http4s" %% "http4s-ember-client" % "0.23.24",
      "org.http4s" %% "http4s-circe" % "0.23.24",
      "org.http4s" %% "http4s-dsl" % "0.23.24",
      
      "io.circe" %% "circe-core" % "0.14.5",
      "io.circe" %% "circe-generic" % "0.14.5",
      "io.circe" %% "circe-parser" % "0.14.5",
      
      "org.tpolecat" %% "doobie-core" % "1.0.0-RC4",
      "org.tpolecat" %% "doobie-h2" % "1.0.0-RC4",
      "org.tpolecat" %% "doobie-hikari" % "1.0.0-RC4",
      "org.tpolecat" %% "doobie-postgres" % "1.0.0-RC4",
      
      "com.h2database" % "h2" % "2.2.224",
      "org.flywaydb" % "flyway-core" % "9.22.3",
      
      "dev.profunktor" %% "http4s-jwt-auth" % "1.2.0",
      "org.mindrot" % "jbcrypt" % "0.4",
      
      "com.github.jwt-scala" %% "jwt-circe" % "9.4.4",
      
      "com.github.pureconfig" %% "pureconfig-core" % "0.17.4",
      
      "ch.qos.logback" % "logback-classic" % "1.4.14",
      "org.typelevel" %% "log4cats-slf4j" % "2.6.0",
      
      "org.scalatest" %% "scalatest" % "3.2.17" % Test,
      "org.scalatestplus" %% "mockito-4-11" % "3.2.17.0" % Test,
      "org.typelevel" %% "cats-effect-testing-scalatest" % "1.5.0" % Test
    )
  )
  .dependsOn(shared.jvm)

lazy val frontend = project
  .in(file("frontend"))
  .enablePlugins(ScalaJSPlugin)
  .enablePlugins(ScalaJSBundlerPlugin)
  .settings(
    name := "cottage-reservation-frontend",
    version := "0.1.0",
    scalaVersion := scala3Version,
    scalaJSUseMainModuleInitializer := true,
    
    Compile / npmDependencies ++= Seq(
      "react" -> "18.2.0",
      "react-dom" -> "18.2.0",
      "react-calendar" -> "4.8.0", // Calendar component for date selection
      "@emotion/react" -> "11.11.1",
      "@emotion/styled" -> "11.11.0",
      "@mui/material" -> "5.14.18",   // Material UI components
      "@mui/icons-material" -> "5.14.18"  // Material icons
    ),
    
    scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.CommonJSModule) },
    requireJsDomEnv := true,
    
    libraryDependencies ++= Seq(
      "com.github.japgolly.scalajs-react" %%% "core" % "2.1.1",
      "com.github.japgolly.scalajs-react" %%% "extra" % "2.1.1",
      "io.circe" %%% "circe-core" % "0.14.5",
      "io.circe" %%% "circe-generic" % "0.14.5",
      "io.circe" %%% "circe-parser" % "0.14.5",
      "io.github.cquiroz" %%% "scala-java-time" % "2.5.0", // For date/time handling in ScalaJS
      "io.github.cquiroz" %%% "scala-java-time-tzdb" % "2.5.0" // Timezone database for ScalaJS
    ),
    Compile / envVars += ("NODE_OPTIONS" -> "--openssl-legacy-provider"),
    run / envVars += ("NODE_OPTIONS" -> "--openssl-legacy-provider"),

    webpackConfigFile := Some(baseDirectory.value / "webpack.config.js"),
    webpackDevServerExtraArgs := Seq("--mode", "development", "--open")
  )
  .dependsOn(shared.js)

lazy val shared = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("shared"))
  .settings(
    name := "cottage-reservation-shared",
    version := "0.1.0",
    scalaVersion := scala3Version,
    
    libraryDependencies ++= Seq(
      "io.circe" %%% "circe-core" % "0.14.5",
      "io.circe" %%% "circe-generic" % "0.14.5",
      "io.circe" %%% "circe-parser" % "0.14.5"
    )
  )