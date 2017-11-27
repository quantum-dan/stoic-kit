package stoickit.api.generic

import spray.json.DefaultJsonProtocol._
import spray.json._

object Success {

  case class Success(success: Boolean = true, result: String = "")
  implicit val successFormat = jsonFormat2(Success)

}
