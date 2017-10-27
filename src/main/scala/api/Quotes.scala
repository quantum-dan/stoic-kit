package stoickit.api.quotes

import stoickit.interface.quotes

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import spray.json.DefaultJsonProtocol._
import spray.json._
import akka.stream.scaladsl.Source
import akka.util.ByteString

object Route {
  implicit val quoteFormat = jsonFormat3(quotes.Quote)

  val randomQuotes = Source.fromIterator(() => quotes.Quotes.randomQuoteStream.toIterator)
  val chunkedQuotes = randomQuotes.map({q =>
    val jsonString = q.toJson.compactPrint
    HttpEntity.Chunk(ByteString(jsonString))
  })

  val route: Route =
    path("quote") {
      get {
        val quoteOption: Option[quotes.Quote] = quotes.Quotes.randomQuote()
        complete(
          quoteOption match {
            case None => StatusCodes.NotFound
            case Some(quote) => quote
          }
        )
      }
    } ~
    pathPrefix("quote" / IntNumber) { id =>
      get {
        val quoteOption: Option[quotes.Quote] = quotes.Quotes.getQuote(id)
        complete(quoteOption match {
          case None => StatusCodes.NotFound
          case Some(quote) => quote
        })
      }
    } ~
    path("quotes") {
      get {
        complete(
          HttpEntity.Chunked(
            ContentTypes.`application/json`,
            chunkedQuotes
          )
        )
      }
    }
}