package stoickit.interface.exercises

import stoickit.db.exercises
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Await}
import stoickit.interface.generic

sealed trait Filter

object Exercises {
  type Exercise = exercises.Exercise
  def streamRecommendations[U](filters: List[Filter] = List())(handler: Exercise => U) = generic.ReadDb.handleStream(handler, exercises.ExercisesDb.streamRecommended)

  def createExercise: Exercise => Unit = exercises.ExercisesDb.create
}

