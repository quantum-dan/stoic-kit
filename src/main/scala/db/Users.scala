package stoickit.db

import stoickit.db.generic._
import scala.concurrent.ExecutionContext.Implicits.global
import slick.jdbc.MySQLProfile.api._
import scala.concurrent.{Future, Await}

case class User(id: Int, identifier: String, password: Option[String], friendsViewable: Boolean, public: Boolean)

class Users(tag: Tag) extends Table[User](tag, "users") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def identifier = column[String]("identifier")
  def password = column[Option[String]]("password")
  def friendsViewable = column[Boolean]("friends")
  def public = column[Boolean]("public")
  def * = (id, identifier, password, friendsViewable, public) <> (User.tupled, User.unapply)
}

object UsersDb {
  import SqlDb.db
  val users = TableQuery[Users]

  def init = db.run(users.schema.create)

  def createUser(identifier: String, password: Option[String] = None) = db.run(users += User(0, identifier, password, false, false)) // No I am not leaving it with plain text passwords!  This is just for testing
  def getUsers: Future[Seq[User]] = db.run(users.result)
}