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
        complete(Await.result(Entries.getByUser(userId), Duration.Inf).takeRight(nToTake))
      } ~ post (entity(as[Entry]) { entry =>
        Entries.create(entry.copy(userId = userId, chapterId = None))
        complete(Success())
      })
    } ~ path("chapter") {
      post (entity(as[Chapter]) { chapter =>
        onSuccess(Chapters.create(chapter.copy(userId = userId)))(id => complete(Success(true, id.toString())))
      })
    } ~ path("chapters") {
      get { onSuccess(Chapters.getChapters(userId))(chapters => complete(chapters))}
    } ~ pathPrefix("chapter" / IntNumber) { chapterId =>
      get {
        Entries.getByChapter(userId, chapterId) match {
          case None => complete(StatusCodes.Unauthorized)
          case Some(futureEntries) => onSuccess(futureEntries)(entries => complete(entries))
        }
      } ~ post (entity(as[Entry]) { entry =>
        val mChapter = Await.result(Chapters.get(chapterId), Duration.Inf)
        mChapter match {
          case None => complete(StatusCodes.NotFound)
          case Some(chapter: Chapter) => if (chapter.userId == userId) {
            Entries.create(entry.copy(userId = userId, chapterId = Some(chapter.id)))
            complete(Success())
          } else complete(StatusCodes.Unauthorized)
        }
      })
    } ~ pathPrefix("html") { // Generates plain HTML documents for the user to keep--their own local copy of the handbook
      pathPrefix("chapter" / IntNumber) { chapterId =>
        onSuccess(Chapters.get(chapterId)) {
          case Some(chapter) => {
            if (chapter.userId == userId) {
              onSuccess(Entries.getByChapter(chapter.id)) { entries =>
                complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, HandbookHtml.htmlChapter(entries, chapter.title, chapter.number)))
              }
            }
            else complete(StatusCodes.Unauthorized)
          }
          case None => complete(StatusCodes.NotFound)
        }
      } ~ path("all")(onSuccess(Entries.getByUser(userId)) { entries =>
        val fullChapters = Entries.byChapters(entries)
        val unChaptered = Entries.unChaptered(entries)
        complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, HandbookHtml.htmlAll(fullChapters, unChaptered)))
      })
    }
  }
}