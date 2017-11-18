package stoickit.interface.users

import stoickit.db.users
import users.UsersDb
import com.github.t3hnar.bcrypt._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Success, Failure}
import scala.concurrent.duration._
import scala.concurrent.Await

case class Profile(id: Int, identifier: String, friends: Boolean, public: Boolean)
case class User(id: Int, identifier: String, password: Option[String], friends: Boolean, public: Boolean) {
  def profile(): Profile = Profile(id, identifier, friends, public)
}
case class Login(identifier: String, password: Option[String])

abstract class UsersProvider {
  def get(id: Int): Future[Option[User]]
  def get(ident: String): Future[Option[User]]
  def create(login: Login): Future[Int]
  def isAdmin(id: Int): Future[Boolean]
  def makeAdmin(userId: Int): Future[Int]
}

object UserTraits {
  sealed trait LoginError
  case object NeedPassword extends LoginError
  case object WrongPassword extends LoginError
  case object NonexistentIdent extends LoginError
  case object UnknownError extends LoginError
  case object NotPasswordAccount extends LoginError // For non-password accounts when a password is given (to avoid exploits)

  sealed trait CreationError
  case object IdentExists extends CreationError // Only the one for now, but this way it's more extensible than just using an Option
}

class Users(duration: Duration = Duration.Inf)(implicit usersDb: UsersProvider) {
  import UserTraits._
  def login(info: Login): Either[LoginError, Profile] = {
    val result = usersDb.get(info.identifier).map({
      case None => Left(NonexistentIdent)
      case Some(userInfo) => userInfo.password match {
        case None => info.password match {
            /* Otherwise, someone could use the username/password login, with any password, to get into
            * an account with G+/Facebook authentication
            * as the ident would match and there would be no password check
            */
          case None => Right(userInfo.profile)
          case Some(_) => Left(NotPasswordAccount)
        }
        case Some(password) => info.password.map(_.isBcrypted(password)) match {
          case None => Left(NeedPassword)
          case Some(true) => Right(userInfo.profile)
          case _ => Left(WrongPassword)
        }
      }
    })
    Await.result(result, duration)
  }

  def create(info: Login): Either[CreationError, Unit] = {
    val result = usersDb.get(info.identifier).map({
      case Some(_) => Left(IdentExists)
      case None => {
        usersDb.create(info)
        Right(()) // Why does it have to be () here but Unit works fine down in the object?  So confused
      }
    })
    Await.result(result, duration)
  }

  def getId(identifier: String): Option[Int] = Await.result(usersDb.get(identifier), duration).map(_.id)

  def isAdmin(userId: Int): Boolean = Await.result(usersDb.isAdmin(userId), duration)
  def isAdmin(identifier: String): Boolean = getId(identifier) match {
    case None => false
    case Some(userId) => Await.result(usersDb.isAdmin(userId), duration)
  }

  def makeAdmin(userId: Int) = usersDb.makeAdmin(userId)
}
