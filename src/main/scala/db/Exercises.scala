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

object ExerciseUtils {
  /** Ranks first by whether owner is subscribed to, then by ID (highest, i.e. newest, first) */
  def ltSubscribed(subs: HashSet[Int])(e1: Exercise, e2: Exercise) = {
    val e1match = subs.contains(e1.ownerId)
    val e2match = subs.contains(e2.ownerId)
    if ((e1match && e2match) || !(e1match || e2match)) e1.id > e2.id // If both/neither subscribed, e1 newer than e2
    else e1match // Which one is subscribed
  }
  def genLt(ranking: Rank)(e1: Exercise, e2: Exercise) = ranking match {
    case NoRank => e1.id > e2.id
    case SubscribedUsers(ids) => ltSubscribed(ids)(e1, e2)
    case StandardRank => {
      val f = (e: Exercise) => (e.upvotes * e.upvotes)/(e.downvotes * e.downvotes + 1) * e.completions
      f(e1) > f(e2)
    }
  }
  def genFilter(filters: List[Filter])(exercise: Exercise): Boolean = {
      val filterList: List[Exercise => Boolean] = filters map {
        case Owner(id) => (e: Exercise) => e.ownerId == id
        case Title(title, exact) => (e: Exercise) => e.title == title // Only exact, for now
        case Types(types, exact) => { (e: Exercise) =>
          if (exact) types == e.types
          else e.types.exists(types.contains(_))
        }
        case Virtues(virtues, exact) => { (e: Exercise) =>
          if (exact) virtues == e.virtues
          else e.virtues.exists(virtues.contains(_))
        }
        case Disciplines(disc, exact) => { (e: Exercise) =>
          if (exact) disc == e.disciplines
          else e.disciplines.exists(disc.contains(_))
        }
        case MinCompletions(min) => (e: Exercise) => e.completions >= min
        case MaxCompletions(max) => (e: Exercise) => e.completions <= max
        case MinUpvotes(min) => (e: Exercise) => e.upvotes >= min
        case MinUpvoteRatio(min) => { (e: Exercise) =>
          if (e.downvotes == 0) {
            if (e.upvotes == 0) min <= 1.0
            else true
          } else e.upvotes/(e.upvotes + e.downvotes) >= min
        }
      }
      filterList.map(f => f(exercise)).fold(true)((a, b) => a && b)
  }
}

object ExercisesDb extends ExercisesProvider {
  val mult: Int = 10 // For filters/sorts that can't be done within Slick
  val exercises = TableQuery[Exercises]
  import SqlDb._
  type QueryType = Query[Exercises, Exercise, Seq]

  def init = db.run(exercises.schema.create)

  def create(exercise: Exercise) = db.run(exercises += exercise)
  def log(item: ExerciseLogItem) = ExerciseLogDb.log(item)

  def get(id: Int): Future[Option[Exercise]] = db.run(exercises.filter(_.id === id).result.headOption)

  def getTop(rank: Rank = NoRank, count: Int = nToLoad): Future[Seq[Exercise]] = _getTop(rank, count)

  def _getTop(rank: Rank = NoRank, count: Int = nToLoad, query: QueryType = exercises): Future[Seq[Exercise]] = rank match {
    case NoRank => db.run(exercises.take(count).result)
      // Next case: Slick doesn't seem to support this type of sorting readily
      // count * 10 ensures getting a good number of subscribed results
    case SubscribedUsers(ids: HashSet[Int]) => db.run(exercises.take(count * mult).result).map(_.sortWith(ExerciseUtils.ltSubscribed(ids)).take(count))
    case StandardRank => db.run(topExercises(exercises).take(count).result)
  }
  def getFiltered(filters: List[Filter] = List(), rank: Rank = NoRank, count: Int = nToLoad): Future[Seq[Exercise]] = {
    db.run(exercises.take(count * mult).result).map(_.filter(ExerciseUtils.genFilter(filters)).sortWith(ExerciseUtils.genLt(rank)))
  }

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

  def getLogEntries(userId: Int): Future[Seq[ExerciseLogItem]] = ExerciseLogDb.getByUser(userId)

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

  def getByUser(userId: Int): Future[Seq[ExerciseLogItem]] = db.run(exerciseLog.filter(_.userId === userId).result)
}
