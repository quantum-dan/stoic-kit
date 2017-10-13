package stoickit.interface.generic

import slick.basic.DatabasePublisher
import scala.concurrent.ExecutionContext.Implicits.global

/* Note: to convert from DatabasePublisher to Stream:
* Make a handler function that wraps Publisher.foreach and give it a Websocket from the API end--send
* items as they come in
 */

object ReadDb {
  def handleStream[T, U](fn: T => U, stream: DatabasePublisher[T]): Unit = stream.foreach(fn)
}