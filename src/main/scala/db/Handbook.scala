package stoickit.db.handbook

import stoickit.db.generic._
import scala.concurrent.ExecutionContext.Implicits.global
import slick.jdbc.MySQLProfile.api._
import scala.concurrent.{Future, Await}

case class Chapter(id: Int, userId: Int, number: Int, title: Option[String] = None)
case class Entry(id: Int, content: String, chapterId: Option[Int] = None)

class Chapters(tag: Tag) extends Table[Chapter](tag, "chapters") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def userId = column[Int]("user_id") // Set up foreign key constraint
  def number = column[Int]("number")
  def title = column[Option[String]]("title")
  def * = (id, userId, number, title) <> (Chapter.tupled, Chapter.unapply)
}

// Entries should use DynamoDB

object ChaptersDb {
  import SqlDb._
  val chapters = TableQuery[Chapters]

  def create(chapter: Chapter) = db.run(chapters += chapter)
  def getChapter(id: Int) = db.run(chapters.filter(_.id === id).result)
  def getChapters(userId: Int) = db.run(chapters.filter(_.userId === userId).result)
}

