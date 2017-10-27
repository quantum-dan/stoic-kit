package stoickit.db.users

import stoickit.db.generic._
import scala.concurrent.ExecutionContext.Implicits.global
import slick.jdbc.MySQLProfile.api._
import scala.concurrent.{Future, Await}
import com.github.t3hnar.bcrypt._ // password hashing
import slick.basic.DatabasePublisher

/** @param password `Some(password)` for username/password logins, `None` for G+/Facebook logins.
  * @param friendsViewable Whether friends can view their profile
  * @param public Whether their profile is publicly visible
  */
case class User(id: Int, identifier: String, password: Option[String], friendsViewable: Boolean, public: Boolean)
case class Admin(userId: Int, isAdmin: Boolean = true)

class Users(tag: Tag) extends Table[User](tag, "users") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def identifier = column[String]("identifier", O.Unique, O.Length(100))
  def password = column[Option[String]]("password")
  def friendsViewable = column[Boolean]("friends")
  def public = column[Boolean]("public")
  def * = (id, identifier, password, friendsViewable, public) <> (User.tupled, User.unapply)
}

class Admins(tag: Tag) extends Table[Admin](tag, "admins") {
  def userId = column[Int]("user_id", O.PrimaryKey)
  def isAdmin = column[Boolean]("is_admin")
  def * = (userId, isAdmin) <> (Admin.tupled, Admin.unapply)

  def user = foreignKey("user_fk", userId, UsersDb.users)(_.id, onDelete=ForeignKeyAction.Cascade)
}

/** Contains functions for working with users */
object UsersDb {
  val users = TableQuery[Users]
  val admins = TableQuery[Admins]
  import SqlDb._

  def init = db.run(users.schema.create)
  def initAdmin = db.run(admins.schema.create)

  def createUser(identifier: String, password: Option[String] = None) = db.run(users += User(0, identifier, password.map(_.bcrypt), false, false))
  def getUsers: Future[Seq[User]] = db.run(users.result)
  def getUserById(id: Int): Future[Option[User]] = db.run(users.filter(_.id === id).result.headOption)
  def getUserByIdent(ident: String): Future[Option[User]] = db.run(users.filter(_.identifier === ident).result.headOption)

  def userStream: DatabasePublisher[User] = db.stream(users.result)

  def getAdminData(user: User): Future[Option[Admin]] = getAdminData(user.id)
  def getAdminData(userId: Int): Future[Option[Admin]] = db.run(admins.filter(_.userId === userId).result.headOption)
}