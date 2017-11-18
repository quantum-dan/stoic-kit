package stoickit.api.handbook

import stoickit.interface.handbook._
import stoickit.db.handbook.Implicits._
import stoickit.interface.users.Users
import stoickit.db.users.Implicits._

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

  // So that all the requests aren't trying to use the same object and hence waiting on it
  def entries(): Entries = new Entries()
  def chapters(): Chapters = new Chapters()

  case class Success(success: Boolean = true, result: String = "")

  implicit val chapterFormat = jsonFormat4(Chapter)
  implicit val entryFormat = jsonFormat4(Entry)
  implicit val fullChapterFormat = jsonFormat2(FullChapter)
  implicit val successFormat = jsonFormat2(Success)

  def routeWrapper(f: Int => server.Route): server.Route = withLoginCookie(new Users().getId(_) match {
    case None => complete(StatusCodes.Unauthorized)
    case Some(userId) => f(userId)
  })

  val route = routeWrapper { userId: Int =>
    path("") {
      get {
        complete(entries.getByUser(userId).takeRight(nToTake))
      } ~ post (entity(as[Entry]) { entry =>
        entries.create(entry.copy(userId = userId, chapterId = None))
        complete(Success())
      })
    } ~ path("chapter") {
      post (entity(as[Chapter]) { chapter =>
        onSuccess(chapters.create(chapter.copy(userId = userId)))(id => complete(Success(true, id.toString())))
      })
    } ~ path("chapters") {
      get { complete(chapters.getChapters(userId)) }
    } ~ pathPrefix("chapter" / IntNumber) { chapterId =>
      get {
        entries.getByChapter(userId, chapterId) match {
          case None => complete(StatusCodes.Unauthorized)
          case Some(entriesResult) => complete(entriesResult)
        }
      } ~ post (entity(as[Entry]) { entry =>
        val mChapter = chapters.get(chapterId)
        mChapter match {
          case None => complete(StatusCodes.NotFound)
          case Some(chapter: Chapter) => if (chapter.userId == userId) {
            entries.create(entry.copy(userId = userId, chapterId = Some(chapter.id)))
            complete(Success())
          } else complete(StatusCodes.Unauthorized)
        }
      })
    } ~ pathPrefix("html") { // Generates plain HTML documents for the user to keep--their own local copy of the handbook
      pathPrefix("chapter" / IntNumber) { chapterId =>
        chapters.get(chapterId) match {
          case Some(chapter) => {
            if (chapter.userId == userId) {
              val entriesResult = entries.getByChapter(chapter.id)
                complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, HandbookHtml.htmlChapter(entriesResult, chapter.title, chapter.number)))
            }
            else complete(StatusCodes.Unauthorized)
          }
          case None => complete(StatusCodes.NotFound)
        }
      } ~ path("all")({
        val entriesResult = entries.getByUser(userId)
        val fullchapters = entries.byChapters(entriesResult)
        val unChaptered = entries.unChaptered(entriesResult)
        complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, HandbookHtml.htmlAll(fullchapters, unChaptered)))
      })
    }
  }
}