package stoickit.db.exercises

import stoickit.db.generic._
import scala.concurrent.ExecutionContext.Implicits.global
import slick.jdbc.MySQLProfile.api._
import scala.concurrent.{Future, Await}
import stoickit.db.users
import slick.basic.DatabasePublisher
import scala.collection.mutable.HashSet

import stoickit.interface.exercises
import exercises.Parameters._
import exercises.Classifiers._
import exercises.{ExercisesProvider, Exercise, ExerciseLogItem}

import com.typesafe.config.ConfigFactory

object Implicits {
  implicit val exercisesDb = ExercisesDb
}

object ColumnTypes {
  implicit val exerciseSetType = MappedColumnType.base[HashSet[ExerciseType], Int](toIntExercise, fromIntExercise)

  implicit val virtueSetType = MappedColumnType.base[HashSet[VirtueType], Int](toIntVirtue, fromIntVirtue)

  implicit val disciplineSetType = MappedColumnType.base[HashSet[DisciplineType], Int](toIntDiscipline, fromIntDiscipline)
}

import ColumnTypes._

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
  def timestamp = column[Int]("timestamp")
  def * = (id, userId, exerciseId, timestamp) <> (ExerciseLogItem.tupled, ExerciseLogItem.unapply)
}

object ExercisesDb extends ExercisesProvider {
  val exercises = TableQuery[Exercises]
  import SqlDb._
  type QueryType = Query[Exercises, Exercise, Seq]

  def init = db.run(exercises.schema.create)

  def create(exercise: Exercise) = db.run(exercises += exercise)
  def log(item: ExerciseLogItem) = ExerciseLogDb.log(item)

  def get(id: Int): Future[Option[Exercise]] = db.run(exercises.filter(_.id === id).result.headOption)

  def getTop(rank: Rank = NoRank, count: Int = nToLoad): Future[Seq[Exercise]] = db.run(exercises.take(count).result)
  def getFiltered(filters: List[Filter] = List(), rank: Rank = NoRank, count: Int = nToLoad): Future[Seq[Exercise]] = getTop(count = count)

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
  /** TODO */
  def streamRecommendations[U](filters: List[Filter] = List(), rank: Rank = NoRank)(handler: Exercise => U): Unit = ()

  // Note: String interpolation here is sanitized--no injection risk
  def completion(id: Int): Future[Unit] = db.run(
    sqlu""" UPDATE exercises SET completions = completions + 1
          WHERE id=$id
        """).map((_) => ())
  def upvote(id: Int): Future[Unit] = db.run(
    sqlu""" UPDATE exercises SET upvotes = upvotes + 1
      WHERE id=$id""").map((_) => ())
  def downvote(id: Int): Future[Unit] = db.run(
    sqlu""" UPDATE exercises SET downvotes = downvotes + 1
      WHERE id=$id"""
  ).map((_) => ())
}

object ExerciseLogDb {
  val exerciseLog = TableQuery[ExerciseLog]
  import SqlDb._
  def init = db.run(exerciseLog.schema.create)

  def log(exercise: ExerciseLogItem) = db.run(exerciseLog += exercise)
}
