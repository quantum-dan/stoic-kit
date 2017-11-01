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
  val index: String = configFile.getString("index")

  def main(args: Array[String] = Array()) {
    implicit val system = ActorSystem("stoic-actor-system")
    implicit val materializer = ActorMaterializer()
    implicit val executionContext = system.dispatcher

    val route =
      path("")(getFromFile(index)) ~
      pathPrefix("html")(getFromDirectory("front-end/build/web")) ~
      pathPrefix("quote")(stoickit.api.quotes.Route.route) ~
      pathPrefix("user")(stoickit.api.users.Route.route)
    val binding = Http().bindAndHandle(route, host, port)
    println("RETURN to stop server")
    StdIn.readLine()
    binding
      .flatMap(_.unbind())
      .onComplete(_ => system.terminate)
  }
}
