package stoickit.interface.users

import stoickit.db.users.UsersDb
import com.github.t3hnar.bcrypt._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Success, Failure}

case class Profile(id: Int, identifier: String, friends: Boolean, public: Boolean)
case class Login(identifier: String, password: Option[String])

object UserTraits {
  sealed trait LoginError
  case object NeedPassword extends LoginError
  case object WrongPassword extends LoginError
  case object NonexistentIdent extends LoginError
  case object UnknownError extends LoginError
  case object NotPasswordAccount extends LoginError // For non-password accounts when a password is given (to avoid exploits)
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
}