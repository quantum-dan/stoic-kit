package stoickit.interface.exercises

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Await}
import stoickit.interface.generic
import scala.collection.mutable.HashSet
import scala.concurrent.duration._

import Classifiers._
import Parameters._

object Classifiers {

  sealed trait ExerciseType
  case object Askesis extends ExerciseType
  case object Meditation extends ExerciseType
  case object General extends ExerciseType

  def toIntExercise(ets: HashSet[ExerciseType]): Int = {
    (if (ets contains Askesis) {4} else {0}) +
      (if (ets contains Meditation) {2} else {0}) +
      (if (ets contains General) {1} else {0})
  }
  def fromIntExercise(i: Int): HashSet[ExerciseType] = {
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
  }

  sealed trait VirtueType
  case object Wisdom extends VirtueType
  case object Justice extends VirtueType
  case object Fortitude extends VirtueType
  case object Temperance extends VirtueType
  def toIntVirtue(vts: HashSet[VirtueType]): Int = {
    (if (vts contains Wisdom) {8} else {0}) +
      (if (vts contains Justice) {4} else {0}) +
      (if (vts contains Fortitude) {2} else {0}) +
      (if (vts contains Temperance) {1} else {0})
  }
  def fromIntVirtue(i: Int): HashSet[VirtueType] = {
    var set = new HashSet(): HashSet[VirtueType]
    if (i >= 8) set += Wisdom
    if (i % 8 >= 4) set += Justice
    if (i % 12 >= 2) set += Fortitude
    if (i % 14 == 1) set += Temperance
    set
  }

  sealed trait DisciplineType
  case object Desire extends DisciplineType
  case object Assent extends DisciplineType
  case object Action extends DisciplineType
  def toIntDiscipline(dts: HashSet[DisciplineType]): Int = {
    (if (dts contains Desire) 4 else 0) +
      (if (dts contains Assent) 2 else 0) +
      (if (dts contains Action) 1 else 0)
  }
  def fromIntDiscipline(i: Int): HashSet[DisciplineType] = {
    var set = new HashSet(): HashSet[DisciplineType]
    if (i >= 4) set += Desire
    if (i % 4 >= 2) set += Assent
    if (i % 6 == 1) set += Action
    set
  }
}

case class Exercise(id: Int,
                    title: String, description: String,
                    types: HashSet[ExerciseType],
                    virtues: HashSet[VirtueType],
                    disciplines: HashSet[DisciplineType],
                    duration: Int = 1, // Days
                    recommended: Boolean = false,
                    ownerId: Int, completions: Int = 0, upvotes: Int = 0, downvotes: Int = 0)
case class ExerciseLogItem(id: Int, userId: Int, exerciseId: Int, timestamp: Int)

abstract class ExercisesProvider {
  val nToLoad: Int = com.typesafe.config.ConfigFactory.load().getInt("exercises.numberToLoad")
  def streamRecommendations[U](filters: List[Filter] = List(), rank: Rank = NoRank)(handler: Exercise => U): Unit

  def create(exercise: Exercise): Future[Int]
  def log(item: ExerciseLogItem): Future[Int]

  def get(id: Int): Future[Option[Exercise]]

  def getTop(rank: Rank = NoRank, count: Int = nToLoad): Future[Seq[Exercise]]

  def getFiltered(filters: List[Filter] = List(), rank: Rank = NoRank, count: Int = nToLoad): Future[Seq[Exercise]]

  def completion(id: Int): Future[Unit]
  def upvote(id: Int): Future[Unit]
  def downvote(id: Int): Future[Unit]
}

object Parameters {

  sealed trait Filter
  case class Owner(ownerId: Int) extends Filter
  case class Title(title: String, exact: Boolean = false) extends Filter
  case class Types(types: HashSet[ExerciseType], exact: Boolean = false) extends Filter
  case class Virtues(virtues: HashSet[VirtueType], exact: Boolean = false) extends Filter
  case class Disciplines(disciplines: HashSet[DisciplineType], exact: Boolean = false) extends Filter
  case class MinCompletions(min: Int) extends Filter
  case class MaxCompletions(max: Int) extends Filter
  case class MinUpvotes(min: Int) extends Filter
  case class MinUpvoteRatio(min: Double) extends Filter

  sealed trait Rank
  case object NoRank extends Rank
  case class SubscribedUsers(userIds: HashSet[Int]) extends Rank
}

class Exercises(duration: Duration = Duration.Inf)(implicit exercisesDb: ExercisesProvider) {
  /** NOT FOR PRODUCTION USE */
  def currentTime(): Int = 0

  def create(exercise: Exercise): Future[Int] = exercisesDb.create(exercise)
  def log(logItem: ExerciseLogItem): Future[Int] = exercisesDb.log(logItem.copy(timestamp = currentTime()))
  def createAndLog(exercise: Exercise): Future[Int] = exercisesDb.create(exercise.copy(completions = 1)).flatMap({id =>
    exercisesDb.log(ExerciseLogItem(0, exercise.ownerId, id, currentTime()))
  })

  def loadRecommendations() = Await.result(exercisesDb.getTop(), duration).filter(_.recommended)
}

