ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.3.1"

lazy val root = (project in file("."))
  .settings(
    name := "PubSub",
    libraryDependencies ++= Seq(
      // Add RabbitMQ library dependency
      "com.rabbitmq" % "amqp-client" % "5.16.0",
      "com.fasterxml.jackson.core" % "jackson-databind" % "2.15.1",
      "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.14.2",
      "org.slf4j" % "slf4j-api" % "2.0.5"
    )
  )
