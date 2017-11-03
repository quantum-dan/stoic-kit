package stoickit.interface.handbook

import stoickit.db.handbook
import handbook.{ChaptersDb, EntriesDb}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Await}
import scala.collection.immutable.HashSet
import scala.concurrent.duration._

case class Chapter(id: Int, userId: Int, number: Int, title: Option[String] = None)
case class Entry(id: Int, userId: Int, content: String, chapterId: Option[Int] = None)
case class FullChapter(chapter: Chapter, entries: Seq[Entry])

object Chapters {
  def toDb(chapter: Chapter): handbook.Chapter = Chapter.unapply(chapter).map(Function.tupled(handbook.Chapter)).get
  def fromDb(chapter: handbook.Chapter): Chapter = handbook.Chapter.unapply(chapter).map(Function.tupled(Chapter)).get

  def create(chapter: Chapter): Future[Int] = ChaptersDb.create(toDb(chapter))
  def getChapters(userId: Int): Future[Seq[Chapter]] = ChaptersDb.getChapters(userId).map(_.map(fromDb))

  def get(id: Int): Future[Option[Chapter]] = ChaptersDb.getChapter(id).map(_.map(fromDb))
  def get(userId: Int, number: Int): Future[Option[Chapter]] = ChaptersDb.getChapter(userId, number).map(_.map(fromDb))
  def get(userId: Int, title: String): Future[Option[Chapter]] = ChaptersDb.getChapter(userId, title).map(_.map(fromDb))
}

object Entries {
  def toDb(entry: Entry): handbook.Entry =
    Entry.unapply(entry).map(Function.tupled(handbook.Entry)).get
  def fromDb(entry: handbook.Entry): Entry =
    handbook.Entry.unapply(entry).map(Function.tupled(Entry)).get

  def create(entry: Entry): Future[Int] = EntriesDb.create(toDb(entry))

  def get(id: Int): Future[Option[Entry]] = EntriesDb.get(id).map(_.map(fromDb))
  def getByUser(userId: Int): Future[Seq[Entry]] = EntriesDb.getByUser(userId).map(_.map(fromDb))
  def getByChapter(chapterId: Int): Future[Seq[Entry]] = EntriesDb.getByChapter(chapterId).map(_.map(fromDb))

  def byChapters(entries: Seq[Entry]): Seq[FullChapter] = {
    val chapterIds: Seq[Int] = entries.map(_.chapterId).filter(id => !(id.isEmpty)).map(_.get). // Note: by non-empty filter, can't throw an error
      toSet[Int].toSeq.sortWith((a, b) => a < b)
    val chapters: Seq[FullChapter] = chapterIds.map(chId => Await.result(Chapters.get(chId), 1.second)).
      filter(ch => !(ch.isEmpty)).map(ch => FullChapter(ch.get, Await.result(getByChapter(ch.get.id), 1.second)))
    chapters
  }
  def unChaptered(entries: Seq[Entry]): Seq[Entry] = entries.filter(_.chapterId.isEmpty)
}