package stoickit.api.users

import stoickit.interface.users._

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.model.headers.HttpCookie
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import spray.json.DefaultJsonProtocol._
import spray.json._
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

object Route {
  implicit val loginFormat = jsonFormat2(Login)
  implicit val profileFormat = jsonFormat4(Profile)
  case class Success(success: Boolean)
  implicit val successFormat = jsonFormat1(Success)

  val route = path("login") {
    post {
      entity(as[Login]) { login: Login =>
        val eitherProfile = Await.result(Users.login(login), 1.second)
        eitherProfile match {
          case Left(err) => complete(err.toString)
          case Right(profile) => setCookie(HttpCookie("identifier", value = profile.identifier))(complete(profile))
        }
      }
    } ~ get {
      optionalCookie("identifier") {
        /* Note: this is NOT to be the final implementation, but using it allows future versions to
          return profile info, decrypt encrypted cookies, etc, without need for further modification.
         */
        case Some(cookie) => complete(cookie.value)
        case None => complete(Success(false))
      }
    }
  } ~ path("create")(post{
    entity(as[Login]) { login => Await.result(Users.create(login), 1.second) match {
      case Left(err) => complete(err.toString)
      case Right(_) => complete(Success(true))
    }}
  })
}
