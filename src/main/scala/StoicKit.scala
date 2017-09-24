package stoickit

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import scala.io.StdIn

object StoicKit {
  def main(args: Array[String]) {
    implicit val system = ActorSystem("stoic-actor-system")
    implicit val materializer = ActorMaterializer()
    implicit val executionContext = system.dispatcher

    val route =
      path("") {
        get {
          complete("Hello, world!")
        }
      }

    val binding = Http().bindAndHandle(route, "localhost", 8080)
    println("RETURN to stop server")
    StdIn.readLine()
    binding
      .flatMap(_.unbind())
      .onComplete(_ => system.terminate)
  }
}