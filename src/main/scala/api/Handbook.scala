package stoickit.api.handbook

import stoickit.interface.handbook._
import stoickit.interface.users.Users.getId

import stoickit.api.users.LoginCookie.withLoginCookie

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.Http
import akka.http.scaladsl.server
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import spray.json.DefaultJsonProtocol._
import spray.json._
import scala.concurrent.{Future, Await}
import scala.concurrent.duration._

object Route {
  val nToTake: Int = 10

  case class Success(success: Boolean = true, result: String = "")

  implicit val chapterFormat = jsonFormat4(Chapter)
  implicit val entryFormat = jsonFormat4(Entry)
  implicit val fullChapterFormat = jsonFormat2(FullChapter)
  implicit val successFormat = jsonFormat2(Success)

  def routeWrapper(f: Int => server.Route): server.Route = withLoginCookie(getId(_) match {
    case None => complete(StatusCodes.Unauthorized)
    case Some(userId) => f(userId)
  })

  val route = routeWrapper { userId: Int =>
    path("") {
      get {
        complete(Await.result(Entries.getByUser(userId), 1.second).takeRight(nToTake))
      } ~ post (entity(as[Entry]) { entry =>
        Entries.create(entry.copy(userId = userId))
        complete(Success())
      })
    }
  }
}