package stoickit.db.social

case class Friendship(id: Int, firstUserId: Int, secondUserId: Int)
case class FriendRequest(id: Int, originId: Int, destinationId: Int)

case class Group(id: Int, name: String, description: String, adminId: Int)
case class Thread(id: Int, parentId: Option[Int], ownerId: Int, title: Option[String], content: String)
