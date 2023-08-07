package ru.finances
import cats.data._
import cats.effect.IO
import cats.implicits.toSemigroupKOps
import com.comcast.ip4s.{Host, Port}
import io.circe.Encoder
import io.circe.generic.auto.{exportDecoder, exportEncoder}
import io.circe.literal.JsonStringContext
import io.circe.syntax.EncoderOps
import org.http4s._
import org.http4s.circe.jsonOf
import org.http4s.dsl.io._
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.headers.Authorization
import org.http4s.implicits._
import org.http4s.server.AuthMiddleware
import ru.finances.domain.DomHandler
import ru.finances.domain.DomHandler._

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.concurrent.{Await, Future}
import scala.language.implicitConversions

object Server {


  import cats.effect.unsafe.IORuntime
  implicit val runtime: IORuntime = cats.effect.unsafe.IORuntime.global
  import org.http4s.circe.CirceEntityCodec._


  implicit val decoderUser = jsonOf[IO, User]
  implicit val decoderLineItem = jsonOf[IO, LineItem]

  val authUserEither: Kleisli[IO, Request[IO], Either[String, User]] = Kleisli { req =>
    val authHeader: Option[Authorization] = req.headers.get[Authorization]
    authHeader match {
      case Some(value) => value match {
        case Authorization(BasicCredentials(creds)) => IO(Right(findUser(creds._1)))
        case _ => IO(Left("No basic credentials"))
      }
      case None => IO(Left("Unauthorized"))
    }
  }

  val authedRoutes: AuthedRoutes[User,IO] =
    AuthedRoutes.of {
      case GET -> Root / "BudgetsWithUser" as user =>
        Ok(findBudgetWithUSer(user).asJson)
    }

  val httpUserRoutes: HttpRoutes[IO] = HttpRoutes.of[IO]{
      case request @ POST -> Root/ "userReq" =>
         System.out.println(request)
         val req = request.as[User].unsafeRunSync()
         System.out.println(req)
         val res = Await.result(DomHandler.insertUser(req), Duration(1000, TimeUnit.MILLISECONDS))
         System.out.println(res)
         Ok(res.asJson)

      case DELETE -> Root/ "userDelete" / id =>
        Ok(DomHandler.deleteUser(id.toLong))
    }

  val httpBudgetRoutes: HttpRoutes[IO] =
    HttpRoutes.of[IO]{
      case GET -> Root/ "budget" / id =>
        Ok(findBudget(id))
      case request @ POST -> Root/ "budgedReq" =>
        System.out.println(request)
        val req = request.as[Budget].unsafeRunSync()
        System.out.println(req)
        val res = Await.result(DomHandler.createBudget(req), Duration(1000, TimeUnit.MILLISECONDS))
        System.out.println(res)
        Ok(res.asJson)
      case request @ PUT -> Root/ "budgedReqPut" =>
        System.out.println(request)
        val req = request.as[Budget].unsafeRunSync()
        System.out.println(req)
        val res = Await.result(DomHandler.updateBudget(req), Duration(1000, TimeUnit.MILLISECONDS))
        System.out.println(res)
        Ok(res.asJson)
      case DELETE -> Root/ "budgetDelete" / id =>
        Ok(DomHandler.deleteBudget(id.toLong))
    }


  val httpExpenseRoutes: HttpRoutes[IO] =
    HttpRoutes.of[IO]{
      case GET -> Root/ "expense" / id =>
        Ok(findExpense(id))
      case request @ POST -> Root/ "expenseReq" =>
        System.out.println(request)
        val req = request.as[Expense].unsafeRunSync()
        System.out.println(req)
        val res = Await.result(DomHandler.createExpense(req), Duration(1000, TimeUnit.MILLISECONDS))
        System.out.println(res)
        Ok(res.asJson)
      case request @ PUT -> Root/ "expenseReqPut" =>
        System.out.println(request)
        val req = request.as[Expense].unsafeRunSync()
        System.out.println(req)
        val res = Await.result(DomHandler.updateExpense(req), Duration(1000, TimeUnit.MILLISECONDS))
        System.out.println(res)
        Ok(res.asJson)
      case DELETE -> Root/ "expenseDelete" / id =>
        Ok(DomHandler.deleteExpense(id.toLong))
    }
  val httpLineItemRoutes: HttpRoutes[IO] =
    HttpRoutes.of[IO]  {
      case GET -> Root/  "lineItem" / id   =>
        Ok(findItem(id))
      case request @ POST -> Root/ "lineItemReq" =>
        System.out.println(request)
        val req: LineItem = request.as[LineItem].onError(errorEitherValue => IO(println(s"log: $errorEitherValue"))).unsafeRunSync()
        System.out.println(req)
        val res = Await.result(DomHandler.createLineItem(req), Duration(1000, TimeUnit.MILLISECONDS))
        System.out.println(res)
        Ok(res.asJson)
      case request @ PUT -> Root/ "lineItemReqPut" =>
        System.out.println(request)
        val req = request.as[LineItem].unsafeRunSync()
        System.out.println(req)
        val res = Await.result(DomHandler.updateLineItem(req), Duration(1000, TimeUnit.MILLISECONDS))
        System.out.println(res)
        Ok(res.asJson)
      case DELETE -> Root/ "lineItemDelete" / id =>
        Ok(DomHandler.deleteLineItem(id.toLong))
    }

  val onFailure: AuthedRoutes[String, IO] = Kleisli { (req: AuthedRequest[IO,String]) =>
    req.req match {
      case _ => OptionT.pure[IO](Response[IO](status = Status.Unauthorized))
    }
  }

  val authMiddleware: AuthMiddleware[IO,User] = AuthMiddleware(authUserEither, onFailure)

  val serviceKleisli = authMiddleware(authedRoutes) <+> httpUserRoutes <+> httpLineItemRoutes <+>
    httpExpenseRoutes <+> httpBudgetRoutes


  def findUser(username: String): User = {
   val maxWaitTime: FiniteDuration = Duration(5, TimeUnit.SECONDS)
   val magicUser: User = Await.result(DomHandler.filterUser(username), maxWaitTime)
   magicUser
 }

  def findBudget(id: String): FullBudget = {
    val maxWaitTime: FiniteDuration = Duration(5, TimeUnit.SECONDS)
    val magicUser: FullBudget = Await.result(DomHandler.filterBudget(id.toLong), maxWaitTime)
    magicUser
  }
  def findBudgetWithUSer(user: User): List[FullBudget] = {
    val maxWaitTime: FiniteDuration = Duration(5, TimeUnit.SECONDS)
    val magicUser: List[FullBudget] = Await.result(DomHandler.filterBudgetsWithUser(user), maxWaitTime)
    magicUser
  }

  def findExpense(id: String): FullExpense = {
    val maxWaitTime: FiniteDuration = Duration(5, TimeUnit.SECONDS)
    val magicUser: FullExpense = Await.result(DomHandler.filterExpense(id.toLong), maxWaitTime)
    magicUser
  }

  def findItem(id: String): FullLineItem = {
    val maxWaitTime: FiniteDuration = Duration(5, TimeUnit.SECONDS)
    val magicUser: FullLineItem = Await.result(DomHandler.filterLineItem(id.toLong), maxWaitTime)
    magicUser
  }

 implicit val IntEncoder :Encoder[Future[Int]]  ={
   val maxWaitTime: FiniteDuration = Duration(5, TimeUnit.SECONDS)
   Encoder.instance { (int: Future[Int]) =>
     json"""{"Int": ${Await.result(int, maxWaitTime)}}"""
   }
 }

  val server = for {
    s <- EmberServerBuilder
      .default[IO]
      .withPort(Port.fromInt(8080).get)
      .withHost(Host.fromString("localhost").get)
      .withHttpApp(serviceKleisli.orNotFound)
      .build
  } yield s


}
