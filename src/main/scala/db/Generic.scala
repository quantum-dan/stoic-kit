package stoickit.db.generic
// Code used by all parts of db--not intended to be used outside of db, except for closing connection pools
// on server shutdown

import slick.jdbc.MySQLProfile.api._
import com.typesafe.config.ConfigFactory
import scala.concurrent.Future
import stoickit.db._

/** Functions for interaction with the SQL database */
object SqlDb {
  val config = ConfigFactory.load()
  /** Which database to use */
  val dbConfig = if(config.getBoolean("production")) "rdsMaria" else "localDb"
  val db = Database.forConfig(dbConfig)
  def close = db.close()
}

/** Initialize all database tables currently in use */
object Initialize {
  def apply(): Unit = {
    users.UsersDb.init
    users.UsersDb.initAdmin
    quotes.QuotesDb.init
    handbook.ChaptersDb.init
    handbook.EntriesDb.init
    exercises.ExercisesDb.init
    exercises.ExerciseLogDb.init
  }
}