package stoickit.db.exercises

import stoickit.db.generic._
import scala.concurrent.ExecutionContext.Implicits.global
import slick.jdbc.MySQLProfile.api._
import scala.concurrent.{Future, Await}
import stoickit.db.users
import slick.basic.DatabasePublisher
import scala.collection.mutable.HashSet

import com.typesafe.config.ConfigFactory

sealed trait ExerciseType
case object Askesis extends ExerciseType
case object Meditation extends ExerciseType
case object General extends ExerciseType

sealed trait VirtueType
case object Wisdom extends VirtueType
case object Justice extends VirtueType
case object Fortitude extends VirtueType
case object Temperance extends VirtueType

sealed trait DisciplineType
case object Desire extends DisciplineType
case object Assent extends DisciplineType
case object Action extends DisciplineType

object ColumnTypes {
  implicit val exerciseSetType = MappedColumnType.base[HashSet[ExerciseType], Int]({ ets =>
      (if (ets contains Askesis) {4} else {0}) + (if (ets contains Meditation) {2} else {0}) + (if (ets contains General) {1} else {0})
    }, { i =>
    var set = new HashSet(): HashSet[ExerciseType]
    if (i >= 4) {
      set += Askesis
    }
    if (i % 4 >= 2) {
      set += Meditation
    }
    if (i % 6 == 1) {
      set += General
    }
    set
  })

  implicit val virtueSetType = MappedColumnType.base[HashSet[VirtueType], Int]({vts =>
    (if (vts contains Wisdom) {8} else {0}) + (if (vts contains Justice) {4} else {0}) + (if (vts contains Fortitude) {2} else {0}) + (if (vts contains Temperance) {1} else {0})
  },
    {i =>
      var set = new HashSet(): HashSet[VirtueType]
      if (i >= 8) set += Wisdom
      if (i % 8 >= 4) set += Justice
      if (i % 12 >= 2) set += Fortitude
      if (i % 14 == 1) set += Temperance
      set
    }
  )

  implicit val disciplineSetType = MappedColumnType.base[HashSet[DisciplineType], Int]({dts =>
    (if (dts contains Desire) 4 else 0) + (if (dts contains Assent) 2 else 0) + (if (dts contains Action) 1 else 0)
  },
  { i =>
    var set = new HashSet(): HashSet[DisciplineType]
    if (i >= 4) set += Desire
    if (i % 4 >= 2) set += Assent
    if (i % 6 == 1) set += Action
    set
  }
  )
}

import ColumnTypes._

case class Exercise(id: Int = 0,
                    title: String, description: String,
                    types: HashSet[ExerciseType],
                    virtues: HashSet[VirtueType],
                    disciplines: HashSet[DisciplineType],
                    duration: Int = 1, // Days
                    recommended: Boolean = false,
                    ownerId: Int, completions: Int = 0, upvotes: Int = 0, downvotes: Int = 0)
case class ExerciseLogItem(id: Int, userId: Int, exerciseId: Int, timestamp: String)

class Exercises(tag: Tag) extends Table[Exercise](tag, "exercises") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def title = column[String]("title")
  def description = column[String]("description")
  def types = column[HashSet[ExerciseType]]("types")
  def virtues = column[HashSet[VirtueType]]("virtues")
  def disciplines = column[HashSet[DisciplineType]]("disciplines")
  def duration = column[Int]("duration")
  def recommended = column[Boolean]("recommended")
  def ownerId = column[Int]("owner_id") // Note: no foreign key restriction, so that exercises created will remain
  // Even if user account is deleted
  def completions = column[Int]("completions")
  def upvotes = column[Int]("upvotes")
  def downvotes = column[Int]("downvotes")

  def * = (id, title, description, types, virtues, disciplines, duration, recommended, ownerId,
    completions, upvotes, downvotes) <> (Exercise.tupled, Exercise.unapply)
}

class ExerciseLog(tag: Tag) extends Table[ExerciseLogItem](tag, "exercises_log") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def userId = column[Int]("user_id") // set up foreign key constraint
  def exerciseId = column[Int]("exercise_id")
  def timestamp = column[String]("timestamp")
  def * = (id, userId, exerciseId, timestamp) <> (ExerciseLogItem.tupled, ExerciseLogItem.unapply)
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
    (e.downvotes + 1)/(e.upvotes * e.upvotes * e.completions + 1)) // +1 avoids division by 0

  def recommended: QueryType = topExercises(exercises.filter(_.recommended))
  def streamRecommended: DatabasePublisher[Exercise] = db.stream(recommended.result)

  def streamTop: DatabasePublisher[Exercise] = db.stream(topExercises(recommended).result)

  def loadRecommendations = db.run(topExercises(exercises.filter(_.recommended)).take(numberToLoad).result)

  def streamExercises(filtered: QueryType): DatabasePublisher[Exercise] = db.stream(filtered.result)

}

object ExerciseLogDb {
  val exerciseLog = TableQuery[ExerciseLog]
  import SqlDb._
  def init = db.run(exerciseLog.schema.create)

  def log(exercise: ExerciseLogItem) = db.run(exerciseLog += exercise)
}
