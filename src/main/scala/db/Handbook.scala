package stoickit.db.handbook

import stoickit.db.generic._
import stoickit.db.users.UsersDb
import scala.concurrent.ExecutionContext.Implicits.global
import slick.jdbc.MySQLProfile.api._
import scala.concurrent.{Future, Await}

case class Chapter(id: Int, userId: Int, number: Int, title: Option[String] = None)
/** @param chapterId If `None`, won't be associated with any chapter */
case class Entry(id: Int, userId: Int, content: String, chapterId: Option[Int] = None)

class Chapters(tag: Tag) extends Table[Chapter](tag, "chapters") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def userId = column[Int]("user_id") // Set up foreign key constraint
  def number = column[Int]("number")
  def title = column[Option[String]]("title")
  def * = (id, userId, number, title) <> (Chapter.tupled, Chapter.unapply)
}

class Entries(tag: Tag) extends Table[Entry](tag, "entries") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def userId = column[Int]("user_id")
  def content = column[String]("content");
  def chapterId = column[Option[Int]]("chapter_id")
  def * = (id, userId, content, chapterId) <> (Entry.tupled, Entry.unapply)

  def user = foreignKey("user_fk", userId, UsersDb.users)(_.id, onDelete=ForeignKeyAction.Cascade)
  def chapter = foreignKey("chapter_fk", chapterId, ChaptersDb.chapters)(_.id, onDelete=ForeignKeyAction.Cascade)
}

// Entries should move to DynamoDB, but will use SQL for now

object ChaptersDb {
  import SqlDb._
  val chapters = TableQuery[Chapters]

  def init = db.run(chapters.schema.create)

  def create(chapter: Chapter) = db.run(chapters += chapter)
  def getChapter(id: Int): Future[Option[Chapter]] = db.run(chapters.filter(_.id === id).result.headOption)
  def getChapter(userId: Int, number: Int): Future[Option[Chapter]] =
    db.run(chapters.filter(ch => ch.userId === userId && ch.number === number).result.headOption)
  def getChapter(userId: Int, title: String): Future[Option[Chapter]] =
    db.run(chapters.filter(ch => ch.userId === userId && ch.title.map(_ === title)).result.headOption)

  def getChapters(userId: Int) = db.run(chapters.filter(_.userId === userId).result)
}

object EntriesDb {
  import SqlDb._
  val entries = TableQuery[Entries]

  def init = db.run(entries.schema.create)

  def create(entry: Entry): Future[Int] = db.run(entries += entry);
  def create(userId: Int, content: String, chapterId: Option[Int] = None): Future[Int] = create(Entry(0, userId, content, chapterId))

  def get(id: Int) = db.run(entries.filter(_.id === id).result.headOption)
  def getByUser(userId: Int) = db.run(entries.filter(_.userId === userId).result)
  def getByChapter(chapterId: Int) = db.run(entries.filter(_.chapterId === chapterId).result)
}
