package stoickit.interface.quotes

import stoickit.db.quotes
import stoickit.interface.users
import scala.concurrent.duration._
import scala.concurrent.Await
import scala.collection.immutable.Stream
import scala.concurrent.ExecutionContext.Implicits.global

case class Quote(id: Int, author: String, content: String)

object Db {
  def dbToApi(quote: quotes.Quote): Quote = Quote(quote.id, quote.author, quote.content)
  def apiToDb(quote: Quote): quotes.Quote = quotes.Quote(quote.id, quote.author, quote.content)
}

object Quotes {
  def randomQuote(): Option[Quote] = Await.result(quotes.QuotesDb.randomQuote, 1.second).map(Db.dbToApi)

  val randomIter: Iterator[Quote] = new Iterator[Quote] {
    def hasNext = true

    def next(): Quote = randomQuote() match {
      case None => next()
      case Some(q) => q
    }
  }

  def randomQuoteStream: Stream[Quote] = randomIter.toStream

  def quoteStream[U](author: Option[String] = None)(handler: Quote => U): Unit = quotes.QuotesDb.stream(author).foreach(q => handler(Db.dbToApi(q)))

  def addQuote(quote: Quote, userId: Int): Either[Unit, Unit] = {
    if (users.Users.isAdmin(userId)) {
      quotes.QuotesDb.addQuote(Db.apiToDb(quote))
      Right(Unit)
    }
    else Left(Unit)
  }
}
