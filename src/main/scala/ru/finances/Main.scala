package ru.finances


import cats.effect.{IO, IOApp}

object Main extends IOApp.Simple {

  def run: IO[Unit] =
    Server.server.use(_ => IO.never)

}
