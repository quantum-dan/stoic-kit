package stoickit.db.exercises

import stoickit.db.generic._
import scala.concurrent.ExecutionContext.Implicits.global
import slick.jdbc.MySQLProfile.api._
import scala.concurrent.{Future, Await}
import stoickit.db.users

import com.typesafe.config.ConfigFactory

case class Exercise(id: Int = 0,
                    title: String, description: String,
                    askesis: Boolean = false, meditation: Boolean = false, general: Boolean = false, // Category
                    wisdom: Boolean = false, justice: Boolean = false, fortitude: Boolean = false, temperance: Boolean = false, // Virtue
                    desire: Boolean = false, action: Boolean = false, assent: Boolean = false, // Discipline
                    duration: Int = 1, // Days
                    recommended: Boolean = false,
                    ownerId: Int, completions: Int = 0, upvotes: Int = 0, downvotes: Int = 0)

class Exercises(tag: Tag) extends Table[Exercise](tag, "exercises") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def title = column[String]("title")
  def description = column[String]("description")
  def askesis = column[Boolean]("askesis")
  def meditation = column[Boolean]("meditation")
  def general = column[Boolean]("general")
  def wisdom = column[Boolean]("wisdom")
  def justice = column[Boolean]("justice")
  def fortitude = column[Boolean]("fortitude")
  def temperance = column[Boolean]("temperance")
  def desire = column[Boolean]("desire")
  def assent = column[Boolean]("assent")
  def action = column[Boolean]("action")
  def duration = column[Int]("duration")
  def recommended = column[Boolean]("recommended")
  def ownerId = column[Int]("owner_id") // Note: no foreign key restriction, so that exercises created will remain
  // Even if user account is deleted
  def completions = column[Int]("completions")
  def upvotes = column[Int]("upvotes")
  def downvotes = column[Int]("downvotes")

  def * = (id, title, description, askesis, meditation, general, wisdom, justice, fortitude,
    temperance, desire, action, assent, duration, recommended, ownerId, completions, upvotes, downvotes) <> (Exercise.tupled, Exercise.unapply)
}

object ExercisesDb {
  val exercises = TableQuery[Exercises]
  import SqlDb._
  type QueryType = Query[Exercises, Exercise, Seq]

  def init = db.run(exercises.schema.create)

  def create(exercise: Exercise) = db.run(exercises += exercise)

  val numberToLoad: Int = ConfigFactory.load().getInt("exercises.numberToLoad")
  // Write methods to get top numberToLoad exercises by various filters

  def getExercises(query: QueryType): Future[Seq[Exercise]] = db.run(query.result)

  def topExercises(filtered: QueryType): QueryType = filtered.sortBy(e => (e.downvotes + 1) *
    (e.downvotes + 1)/(e.upvotes * e.upvotes * e.completions + 1)).take(numberToLoad) // +1 avoids division by 0

  def loadRecommendations = db.run(topExercises(exercises.filter(_.recommended)).result)

}
