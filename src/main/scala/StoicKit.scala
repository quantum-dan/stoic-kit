package stoickit

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import scala.io.StdIn

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
      }

    val binding = Http().bindAndHandle(route, host, port)
    println("RETURN to stop server")
    StdIn.readLine()
    binding
      .flatMap(_.unbind())
      .onComplete(_ => system.terminate)
  }
}
