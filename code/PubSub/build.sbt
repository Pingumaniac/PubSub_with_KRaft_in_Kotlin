ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.3.1"

lazy val root = (project in file("."))
  .settings(
    name := "PubSub",
    libraryDependencies ++= Seq(
      // Add RabbitMQ library dependency
      "com.rabbitmq" % "amqp-client" % "5.16.0"
    )
  )
