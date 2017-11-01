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
case class Login(identifier: String, password: Option[String])

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

object Users {
  import UserTraits._
  def login(info: Login): Future[Either[LoginError, Profile]] = {
    UsersDb.getUserByIdent(info.identifier).map({
      case None => Left(NonexistentIdent)
      case Some(userInfo) => userInfo.password match {
        case None => info.password match {
            /* Otherwise, someone could use the username/password login, with any password, to get into
            * an account with G+/Facebook authentication
            * as the ident would match and there would be no password check
            */
          case None => Right(Profile (userInfo.id, userInfo.identifier, userInfo.friendsViewable, userInfo.public))
          case Some(_) => Left(NotPasswordAccount)
        }
        case Some(password) => info.password.map(_.isBcrypted(password)) match {
          case None => Left(NeedPassword)
          case Some(true) => Right(Profile(userInfo.id, userInfo.identifier, userInfo.friendsViewable, userInfo.public))
          case _ => Left(WrongPassword)
        }
      }
    })
  }

  def create(info: Login): Future[Either[CreationError, Unit]] = {
    UsersDb.getUserByIdent(info.identifier).map({
      case Some(_) => Left(IdentExists)
      case None => {
        UsersDb.createUser(info.identifier, info.password)
        Right(Unit)
      }
    })
  }

  def isAdmin(userId: Int): Boolean = Await.result(UsersDb.getAdminData(userId).map({
    case None => false
    case Some(adm) => adm.isAdmin
  }), Duration.Inf)
  def isAdmin(identifier: String): Boolean = Await.result(UsersDb.getUserByIdent(identifier), Duration.Inf) match {
    case None => false
    case Some(profile) => isAdmin(profile.id)
  }

  def makeAdmin(userId: Int) = UsersDb.makeAdmin(userId)
}