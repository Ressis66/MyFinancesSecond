name := "MyFinancesSecond"

version := "0.1"

scalaVersion := "2.13.11"
val http4sVersion = "1.0.0-M39"
libraryDependencies ++= Seq(
  "org.http4s" %% "http4s-dsl" % http4sVersion,
  "org.http4s" %% "http4s-ember-server" % http4sVersion,
  "org.http4s" %% "http4s-ember-client" % http4sVersion,
  "org.http4s" %% "http4s-circe" % http4sVersion,
  // Optional for auto-derivation of JSON codecs
  "io.circe" %% "circe-generic" % "0.14.5",
  // Optional for string interpolation to JSON model
  "io.circe" %% "circe-literal" % "0.14.5",
  "co.fs2" %% "fs2-core" % "3.6.1",
  "co.fs2" %% "fs2-io" % "3.6.1",

  "com.github.jwt-scala"  %% "jwt-core"  %"9.3.0",
  "com.github.jwt-scala"  %% "jwt-circe" %"9.3.0"
)



libraryDependencies ++= Seq(
  "com.typesafe.slick" %% "slick" % "3.3.3",
  "org.postgresql" % "postgresql" % "42.3.4",
  "com.typesafe.slick" %% "slick-hikaricp" % "3.3.3",
  "com.github.tminglei" %% "slick-pg" % "0.20.3",
  "com.github.tminglei" %% "slick-pg_play-json" % "0.20.3"
)
