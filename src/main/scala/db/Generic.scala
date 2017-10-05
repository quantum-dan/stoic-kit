package stoickit.db.generic
// Code used by all parts of db--not intended to be used outside of db, except for closing connection pools
// on server shutdown

import slick.jdbc.MySQLProfile.api._
import com.typesafe.config.ConfigFactory

object SqlDb {
  val config = ConfigFactory.load()
  val dbConfig = if(config.getBoolean("production")) "rdsMaria" else "localDb"
  val db = Database.forConfig(dbConfig)
  def close = db.close()
}
