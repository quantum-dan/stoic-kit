package stoickit.api.exercises

import stoickit.interface.exercises.{Exercise, ExerciseLogItem, Exercises}
import stoickit.interface.exercises.Classifiers._
import stoickit.interface.exercises.Parameters._
import stoickit.db.exercises.Implicits._
import stoickit.api.users.LoginCookie._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.Http
import akka.http.scaladsl.server
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import spray.json.DefaultJsonProtocol._
import spray.json.{DeserializationException, _}
import scala.collection.mutable.HashSet

import server.directives.ParameterDirectives.ParamMagnet

import stoickit.api.generic.Success._

object Route {
  def exercises() = new Exercises()

  implicit object ExerciseFormat extends RootJsonFormat[Exercise] {
    def write(e: Exercise) = JsObject(
      "id" -> JsNumber(e.id),
      "title" -> JsString(e.title),
      "description" -> JsString(e.description),
      "types" -> JsNumber(toIntExercise(e.types)),
      "virtues" -> JsNumber(toIntVirtue(e.virtues)),
      "disciplines" -> JsNumber(toIntDiscipline(e.disciplines)),
      "duration" -> JsNumber(e.duration),
      "recommended" -> JsBoolean(e.recommended),
      "ownerId" -> JsNumber(e.ownerId),
      "completions" -> JsNumber(e.completions),
      "upvotes" -> JsNumber(e.upvotes),
      "downvotes" -> JsNumber(e.downvotes)
    )
    def read(value: JsValue) = value.asJsObject.getFields("title", "description",
                                                            "types", "virtues", "disciplines",
                                                            "duration", "recommended") match {
      case Seq(JsString(title), JsString(description), JsNumber(types), JsNumber(virtues),
                JsNumber(disciplines), JsNumber(duration), JsBoolean(recommended)) =>
        Exercise(0, title, description, fromIntExercise(types.toInt), fromIntVirtue(virtues.toInt),
          fromIntDiscipline(disciplines.toInt), duration.toInt, recommended, 0, 0, 0, 0)
      case _ => throw new DeserializationException("Exercise expected")
    }
  }
  implicit val logFormat = jsonFormat4(ExerciseLogItem)

  val route = path("") {
    get(complete(exercises.loadRecommendations())) ~
    post(withLoginCookieId{ id: Int =>
      entity(as[Exercise]) { exercise =>
        exercises.createAndLog(exercise.copy(ownerId = id))
        complete(Success())
      }
    })
  } ~
  path("new")(post(withLoginCookieId{ id: Int =>
      entity(as[Exercise]) { exercise =>
        exercises.create(exercise.copy(ownerId = id))
        complete(Success())
      }
  })) ~
  path("log")(withLoginCookieId { id: Int =>
    complete(exercises.getLog(id))
  }) ~
  pathPrefix("log" / IntNumber) {exerciseId =>
    withLoginCookieId{ id: Int =>
      exercises.log(ExerciseLogItem(id = 0, userId = id, exerciseId = exerciseId, timestamp = 0))
      complete(Success())
    }
  } ~
  pathPrefix("filter")(parameterMap { params => // Because parameters()() is bugged, it seems
    def extract(param: String, handle: String => Filter): Option[Filter] = params.get(param).map(handle(_))
    def extractWithExact(param: String, exactParam: String, handle: (String, Boolean) => Filter): Option[Filter] = params.get(param).map({p =>
      handle(p, params.get(exactParam) match {
        case None => false
        case Some(x) => x.toBoolean
      })
    }
    )

    val rank: Rank = params.get("rank") match {
      case Some("std") => StandardRank
      case _ => NoRank
    }
    val filters: List[Filter] = List(
      extract("owner", str => Owner(str.toInt)),
      extractWithExact("title", "title-exact", Title(_, _)),
      extractWithExact("types", "types-exact", (str, exact) => Types(fromIntExercise(str.toInt), exact)),
      extractWithExact("virtues", "virtues-exact", (str, exact) => Virtues(fromIntVirtue(str.toInt), exact)),
      extractWithExact("disciplines", "disciplines-exact", (str, exact) => Disciplines(fromIntDiscipline(str.toInt), exact)),
      extract("min-completions", str => MinCompletions(str.toInt)),
      extract("max-completions", str => MaxCompletions(str.toInt)),
      extract("upvotes", str => MinUpvotes(str.toInt)),
      extract("ratio", str => MinUpvoteRatio(str.toDouble))
    ).filter(!_.isEmpty).map(_.get)
    complete(exercises.getFiltered(filters, rank))
  })
}