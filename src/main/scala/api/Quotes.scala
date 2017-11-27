package stoickit.api.quotes

import stoickit.interface.quotes
import stoickit.db.quotes.Implicits._
import stoickit.api.generic.Success._

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

  def quotesHandler() = new quotes.Quotes()

  val randomQuotes = Source.fromIterator(() => quotesHandler.randomQuoteStream.toIterator)
  val chunkedQuotes = randomQuotes.map({q =>
    val jsonString = q.toJson.compactPrint
    HttpEntity.Chunk(ByteString(jsonString))
  })

  val route: Route =
    path("") {
      get {
        val quoteOption: Option[quotes.Quote] = quotesHandler.randomQuote()
        complete(
          quoteOption match {
            case None => StatusCodes.NotFound
            case Some(quote) => quote
          }
        )
      } ~
      post(cookie("identifier")(identCookie => entity(as[quotes.Quote]) { quote =>
        quotesHandler.addQuote(quote, identCookie.value) match {
          case Left(_) => complete(Success(false))
          case Right(_) => complete(Success(true))
        }
      }))
    } ~
    pathPrefix("id" / IntNumber) { id =>
      get {
        val quoteOption: Option[quotes.Quote] = quotesHandler.getQuote(id)
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