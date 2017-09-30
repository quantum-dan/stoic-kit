package stoickit.db.generic
// Code used by all parts of db--not intended to be exported outside of db

import slick.jdbc.MySQLProfile.api._

object SqlDb {
  val db = Database.forConfig("localDb") // localDb for testing, rdsMaria for production
  def close = db.close()
}
