package stoickit.api.users

import stoickit.interface.users._

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.Http
import akka.http.scaladsl.server
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.model.headers.{HttpCookie, HttpCookiePair}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import spray.json.DefaultJsonProtocol._
import spray.json._
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

object LoginCookie {
  def loginCookie(identifier: String) = HttpCookie("identifier", value = identifier, path=Some("/"))
  def readLoginCookie(cookiePair: HttpCookiePair): Option[String] = Some(cookiePair.value)
  def withLoginCookie(f: String => server.Route) = cookie("identifier")(readLoginCookie(_) match {
    case None => complete(StatusCodes.Unauthorized)
    case Some(str) => f(str)
  })
}

object Route {
  import LoginCookie._

  implicit val loginFormat = jsonFormat2(Login)
  implicit val profileFormat = jsonFormat4(Profile)
  case class Success(success: Boolean, result: String = "")
  implicit val successFormat = jsonFormat2(Success)

  val route = path("") {
    post {
      entity(as[Login]) { login: Login =>
        val eitherProfile = Await.result(Users.login(login), 1.second)
        eitherProfile match {
          case Left(err) => complete(Success(false, err.toString))
          case Right(profile) => setCookie(loginCookie(profile.identifier))(complete(profile))
        }
      }
    } ~ get {
      optionalCookie("identifier") (_.flatMap(readLoginCookie) match {
        /* Note: this is NOT to be the final implementation, but using it allows future versions to
          return profile info, decrypt encrypted cookies, etc, without need for further modification.
         */
        case Some(ident) => complete(Success(true, ident))
        case None => complete(Success(false))
      })
    }
  } ~ path("create")(post{
    entity(as[Login]) { login => Await.result(Users.create(login), 1.second) match {
      case Left(err) => complete(Success(false, err.toString))
      case Right(_) => complete(Success(true))
    }}
  }) ~ path("admin")(get {
    optionalCookie("identifier") (_.flatMap(readLoginCookie) match {
      case Some(ident) => complete(Success(Users.isAdmin(ident)))
      case None => complete(Success(false))
    })
  }) ~ path("logout")(deleteCookie("identifier", path="/")(complete(Success(true))))
}
