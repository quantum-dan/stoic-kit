package stoickit.api.exercises

import stoickit.interface.exercises.{Exercise, ExerciseLogItem, Exercises}
import stoickit.interface.exercises.Classifiers._
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
  pathPrefix("log" / IntNumber) {exerciseId =>
    withLoginCookieId{ id: Int =>
      exercises.log(ExerciseLogItem(id = 0, userId = id, exerciseId = exerciseId, timestamp = 0))
      complete(Success())
    }
  }
}