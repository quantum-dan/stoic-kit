package stoickit.api.users

import stoickit.interface.users._
import stoickit.db.users.Implicits._

import stoickit.api.generic.Success._

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

import stoickit.api.generic.Success._

object LoginCookie {
  /* Security:
   * Store the identifier and IP address in plain text.
   * Store a hash of identifier + IP, with the password as secret, as a signature
   * Hashed with HMAC, SHA512, using password as a secret (is that secure?  Hopefully)
   * See: https://github.com/Nycto/Hasher
   */
  def loginCookie(identifier: String) = HttpCookie("identifier", value = identifier, path=Some("/"))
  def readLoginCookie(cookiePair: HttpCookiePair): Option[String] = Some(cookiePair.value)
  def withLoginCookie(f: String => server.Route): server.Route = cookie("identifier")(readLoginCookie(_) match {
    case None => complete(StatusCodes.Unauthorized)
    case Some(str) => f(str)
  })
  def withLoginCookieId(f: Int => server.Route): server.Route = withLoginCookie({ident: String => (new Users()).getId(ident) match {
    case None => complete(StatusCodes.Unauthorized)
    case Some(id) => f(id)
  }})
}

object Route {
  import LoginCookie._

  implicit val loginFormat = jsonFormat2(Login)
  implicit val profileFormat = jsonFormat4(Profile)

  def usersHandler() = new Users()

  val route = path("") {
    post {
      entity(as[Login]) { login: Login =>
        val eitherProfile = usersHandler.login(login)
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
    entity(as[Login]) { login => usersHandler.create(login) match {
      case Left(err) => complete(Success(false, err.toString))
      case Right(_) => complete(Success(true))
    }}
  }) ~ path("admin")(get {
    optionalCookie("identifier") (_.flatMap(readLoginCookie) match {
      case Some(ident) => complete(Success(usersHandler.isAdmin(ident)))
      case None => complete(Success(false))
    })
  }) ~ path("logout")(deleteCookie("identifier", path="/")(complete(Success(true))))
}
