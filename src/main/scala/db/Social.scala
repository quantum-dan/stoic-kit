package stoickit.db.social

case class Friendship(id: Int, firstUserId: Int, secondUserId: Int)
case class FriendRequest(id: Int, originId: Int, destinationId: Int)
