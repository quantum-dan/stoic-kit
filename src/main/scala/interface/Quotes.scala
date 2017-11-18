package stoickit.interface.quotes

import stoickit.interface.users.Users
import stoickit.db.users.Implicits._

import stoickit.db.quotes
import scala.concurrent.duration._
import scala.concurrent.{Future, Await}
import scala.collection.immutable.Stream
import scala.concurrent.ExecutionContext.Implicits.global

case class Quote(id: Int, author: String, content: String)

abstract class QuotesProvider {
  def randomQuote(): Future[Option[Quote]]
  def get(id: Int): Future[Option[Quote]]
  def create(quote: Quote): Future[Int]
}

class Quotes(duration: Duration = Duration.Inf)(implicit quotesDb: QuotesProvider) {
  def usersHandler() = new Users()
  def randomQuote(): Option[Quote] = Await.result(quotesDb.randomQuote(), duration)

  def randomIter: Iterator[Quote] = new Iterator[Quote] {
    def hasNext = true

    def next(): Quote = randomQuote() match {
      case None => next()
      case Some(q) => q
    }
  }

  def randomQuoteStream: Stream[Quote] = randomIter.toStream

  def getQuote(id: Int): Option[Quote] = Await.result(quotesDb.get(id), duration)

  def addQuote(quote: Quote, userId: Int): Either[Unit, Unit] = {
    if (usersHandler.isAdmin(userId)) {
      quotesDb.create(quote)
      Right(Unit)
    } else Left(Unit)
  }
  def addQuote(quote: Quote, identifier: String): Either[Unit, Unit] = {
    if (usersHandler.isAdmin(identifier)) {
      quotesDb.create(quote)
      Right(Unit)
    } else Left(Unit)
  }
}
