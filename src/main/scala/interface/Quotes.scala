package stoickit.interface.quotes

import stoickit.db.quotes
import stoickit.db.quotes.Quote
import scala.concurrent.duration._
import scala.concurrent.Await
import scala.collection.immutable.Stream

object Quotes {
  def randomQuote(): Option[Quote] = Await.result(quotes.QuotesDb.randomQuote, 1.second)

  val randomIter: Iterator[Quote] = new Iterator[Quote] {
    def hasNext = true
    def next(): Quote = randomQuote() match {
      case None => next()
      case Some(q) => q
    }
  }

  def quoteStream: Stream[Quote] = randomIter.toStream
}