package stoickit.db.exercises.quill

import io.getquill._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.mutable.HashSet
import scala.concurrent.Future

import stoickit.interface.exercises.Classifiers._
import stoickit.interface.exercises.Parameters._
import stoickit.interface.exercises.{ExercisesProvider, Exercise, ExerciseLogItem}

object Context {
  lazy val ctx = new MysqlAsyncContext(SnakeCase, "ctxLocal")
}

object Schema {
  import Context.ctx._
  type QueryType[A] = Quoted[Query[A]]
  type EQueryType[A] = Quoted[EntityQuery[A]]
  type ExerciseEQuery = EQueryType[Exercise]
  type ExerciseQuery = QueryType[Exercise]
  type LogQuery = QueryType[ExerciseLogItem]

  val exercises = quote(querySchema[Exercise]("exercises"))
  val log = quote(querySchema[ExerciseLogItem]("exercises_log"))
}

object Modifiers {
  import Schema.{ExerciseQuery, ExerciseEQuery}
  import Context.ctx._

  def convert(query: ExerciseEQuery): ExerciseQuery = quote {query.sortBy(e => e.id)} // Does nothing but convert the type

  def mkRank(rank: Rank)(query: ExerciseQuery): ExerciseQuery = rank match {
    case NoRank => quote {query.sortBy(e => e.id)(Ord.descNullsLast)}
    case StandardRank => quote {query.sortBy(e => (e.upvotes * e.upvotes * e.completions)/(e.downvotes * e.downvotes + 1))(Ord.descNullsLast)}
    case SubscribedUsers(_) => mkRank(NoRank)(query)
  }
  def convertRank(rank: Rank)(query: ExerciseEQuery): ExerciseQuery = mkRank(rank)(convert(query))

  def mkFilter(filter: Filter)(query: ExerciseQuery): ExerciseQuery = filter match {
    case Owner(id: Int) => quote(query.filter(_.ownerId == lift(id)))
    case Title(title, exact) => {
      if (exact) quote(query.filter(_.title.toLowerCase() == lift(title.toLowerCase())))
      else quote(query.filter(_.title.toLowerCase() like s"%${lift(title).toLowerCase()}%"))
    }
    case Types(types, exact) => {
      quote(query.filter(_.types == lift(types)))
    }
    case Virtues(virtues, exact) => {
      quote(query.filter(_.virtues == lift(virtues)))
    }
    case Disciplines(disciplines, exact) => {
      quote(query.filter(_.disciplines == lift(disciplines)))
    }
    case MinCompletions(min) => quote(query.filter(_.completions >= lift(min)))
    case MaxCompletions(max) => quote(query.filter(_.completions <= lift(max)))
    case MinUpvotes(min) => quote(query.filter(_.upvotes >= lift(min)))
    case MinUpvoteRatio(min) => quote(query.filter(q => q.upvotes/(q.downvotes + 1) >= min))
  }
  def filter(filters: List[Filter])(query: ExerciseQuery): ExerciseQuery = filters.map(f => (q => mkFilter(f)(q))).
    foldLeft(query)((q, f) => f(q))
  def convertFilter(filters: List[Filter])(query: ExerciseEQuery): ExerciseQuery = filter(filters)(convert(query))
}

class ExercisesDb() extends ExercisesProvider {
  import Context.ctx._
  import Schema._
  import Modifiers.{filter, convertFilter, mkRank, convertRank}

  def create(exercise: Exercise): Future[Int] = run(quote {
    exercises.insert(lift(exercise)).returning(_.id)
  })

  def log(item: ExerciseLogItem): Future[Int] = run(quote {
    Schema.log.insert(lift(item)).returning(_.id)
  })

  def get(id: Int): Future[Option[Exercise]] = run(quote {
    exercises.filter(_.id == lift(id)).take(1)
  }).map(_.headOption)

  def getLogEntries(userId: Int): Future[Seq[ExerciseLogItem]] = run(quote {
    Schema.log.filter(_.userId == lift(userId))
  })
  def getEntries(userId: Int): Future[Seq[Exercise]] = run(quote {
    for {
      li <- Schema.log if(li.userId == lift(userId))
      exercise <- exercises if(exercise.id == li.exerciseId)
    } yield exercise
  })

  def getTop(rank: Rank = NoRank, count: Int = nToLoad): Future[Seq[Exercise]] = run(quote(convertRank(rank)(exercises).take(lift(count))))
  def getFiltered(filters: List[Filter] = List(), rank: Rank = NoRank, count: Int = nToLoad) = run(quote {
    mkRank(rank)(convertFilter(filters)(exercises)).take(lift(count))
  })

  /** Provides an integer ranking value */
  def rankVal(rank: Rank, e: Exercise): Int = rank match {
    case NoRank => -(e.id) // Newest first
    case StandardRank => (e.downvotes * e.downvotes + 1)/(e.upvotes * e.upvotes * e.completions) // By upvote:downvote ratio and completions
    case SubscribedUsers(users: HashSet[Int]) => if (users.contains(e.ownerId)) -(e.id) else (Int.MaxValue - e.id)
  }

  def completion(id: Int): Future[Unit] = run(quote(exercises.filter(_.id == lift(id)).update(e => e.completions -> (e.completions + 1)))).map(_ => ())
  def downvote(id: Int): Future[Unit] = run(quote { exercises.filter(_.id == lift(id)).update(e => e.downvotes -> (e.downvotes + 1)) }).map(_ => ())
  def upvote(id: Int): Future[Unit] = run(quote { exercises.filter(_.id == lift(id)).update(e => e.upvotes -> (e.upvotes + 1)) }).map(_ => ())
}
