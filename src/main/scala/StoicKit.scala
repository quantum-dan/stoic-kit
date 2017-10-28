package stoickit

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.directives._
import akka.stream.ActorMaterializer
import scala.io.StdIn
import ContentTypeResolver.Default

import com.typesafe.config.ConfigFactory

object StoicKit {
  val configFile = ConfigFactory.load()
  val port: Int = configFile.getInt("server.port")
  val host: String = configFile.getString("server.host")

  def main(args: Array[String] = Array()) {
    implicit val system = ActorSystem("stoic-actor-system")
    implicit val materializer = ActorMaterializer()
    implicit val executionContext = system.dispatcher

    val route =
      path("") {
        get {
          complete("Hello, world!")
        }
      } ~ path("a") {
        get {
          complete("Hello again!")
        }
      } ~ stoickit.api.quotes.Route.route ~
      stoickit.api.users.Route.route ~
      path("b") {
        get {
          complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "<html> <body> <script src=\"/c\"></script> </body> </html>"))
        }
      } ~
      path("c") {
        getFromFile("/home/daniel/dart/sk.js")
      }
    val binding = Http().bindAndHandle(route, host, port)
    println("RETURN to stop server")
    StdIn.readLine()
    binding
      .flatMap(_.unbind())
      .onComplete(_ => system.terminate)
  }
}
