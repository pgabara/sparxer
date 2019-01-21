package sparxer

import doobie.Transactor
import sparxer.http._
import sparxer.submit.{ SparkJobLauncherLive, SparkSubmitLive }
import sparxer.registry._
import zio._
import zio.clock._
import zio.console._
import zio.blocking.Blocking

object Main extends App {

  def run(args: List[String]): ZIO[Environment, Nothing, Int] =
    ConfigReader.appConfig
      .flatMap(start)
      .foldM(e => putStrLn(s"Error: ${e.getMessage}") *> ZIO.succeed(1), _ => ZIO.succeed(0))

  private def start(config: AppConfig): TaskR[Environment, Unit] =
    ZIO.runtime[Environment].flatMap { implicit rts =>
      for {
        blockingEC  <- blocking.blockingExecutor.map(_.asEC).provide(Blocking.Live)
        transactorR = Database.hikariTransactor[Environment](config.database, Platform.executor.asEC, blockingEC)
        _           <- transactorR.use(xa => HttpServer.start(config).provide(env(xa)))
      } yield ()
    }

  private def env(xa: Transactor[Task]) =
    new DoobieJobRegistry with DoobieStatusRegistry with DoobieUserRegistry with ReferenceLive with SparkSubmitLive
    with SparkJobLauncherLive with Clock.Live {
      def transactor: Transactor[Task] = xa
    }
}
