package stoickit.db.users

import stoickit.db.generic._
import scala.concurrent.ExecutionContext.Implicits.global
import slick.jdbc.MySQLProfile.api._
import scala.concurrent.{Future, Await}
import com.github.t3hnar.bcrypt._ // password hashing

case class User(id: Int, identifier: String, password: Option[String], friendsViewable: Boolean, public: Boolean)

class Users(tag: Tag) extends Table[User](tag, "users") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def identifier = column[String]("identifier", O.Unique, O.Length(100))
  def password = column[Option[String]]("password")
  def friendsViewable = column[Boolean]("friends")
  def public = column[Boolean]("public")
  def * = (id, identifier, password, friendsViewable, public) <> (User.tupled, User.unapply)
}

object UsersDb {
  val users = TableQuery[Users]
  import SqlDb._

  def init = db.run(users.schema.create)

  def createUser(identifier: String, password: Option[String] = None) = db.run(users += User(0, identifier, password.map(_.bcrypt), false, false))
  def getUsers: Future[Seq[User]] = db.run(users.result)
  def getUserById(id: Int): Future[Option[User]] = db.run(users.filter(_.id === id).result.headOption)
  def getUserByIdent(ident: String): Future[Option[User]] = db.run(users.filter(_.identifier === ident).result.headOption)
}