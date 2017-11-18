package stoickit.interface.handbook

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Await}
import scala.collection.immutable.HashSet
import scala.concurrent.duration._

case class Chapter(id: Int, userId: Int, number: Int, title: Option[String] = None)
case class Entry(id: Int, userId: Int, content: String, chapterId: Option[Int] = None)
case class FullChapter(chapter: Chapter, entries: Seq[Entry])

sealed trait EntriesSelector
case class UserId(userId: Int) extends EntriesSelector
case class ChapterId(chapterId: Int) extends EntriesSelector

abstract class ChaptersProvider {
  def get(id: Int): Future[Option[Chapter]]
  def get(userId: Int, number: Int): Future[Option[Chapter]]
  def get(userId: Int, title: String): Future[Option[Chapter]]
  def create(chapter: Chapter): Future[Int]
  def getChapters(userId: Int): Future[Seq[Chapter]]
}

abstract class EntriesProvider {
  def create(entry: Entry): Future[Int]

  def get(id: Int): Future[Option[Entry]]

  def getBy(selector: EntriesSelector): Future[Seq[Entry]]
}

class Chapters(duration: Duration = Duration.Inf)(implicit provider: ChaptersProvider) {
  def create(chapter: Chapter): Future[Int] = provider.create(chapter)

  def get(id: Int): Option[Chapter] = Await.result(provider.get(id), duration)
  def get(userId: Int, number: Int): Option[Chapter] = Await.result(provider.get(userId, number), duration)
  def get(userId: Int, title: String): Option[Chapter] = Await.result(provider.get(userId, title), duration)
  def getChapters(userId: Int): Seq[Chapter] = Await.result(provider.getChapters(userId), duration)
}

class Entries(duration: Duration = Duration.Inf)(implicit provider: EntriesProvider, chaptersProvider: ChaptersProvider) {
  def create(entry: Entry): Future[Int] = provider.create(entry)

  def get(id: Int): Option[Entry] = Await.result(provider.get(id), duration)

  def getByUser(userId: Int): Seq[Entry] = Await.result(provider.getBy(UserId(userId)), duration)
  def getByChapter(chapterId: Int): Seq[Entry] = Await.result(provider.getBy(ChapterId(chapterId)), duration)

  def getByChapter(userId: Int, chapterId: Int): Option[Seq[Entry]] = new Chapters(duration).get(chapterId).
    flatMap({chapter =>
      if (chapter.userId == userId) Some(getByChapter(chapter.id))
      else None
  })

  def byChapters(entries: Seq[Entry]): Seq[FullChapter] = {
    val chapterIds: Seq[Int] = entries.map(_.chapterId).filter(id => !(id.isEmpty)).map(_.get). // Note: by non-empty filter, can't throw an error
      toSet[Int].toSeq.sortWith((a, b) => a < b)
    val chapters: Seq[FullChapter] = chapterIds.map(chId => new Chapters(duration).get(chId)).
      filter(ch => !(ch.isEmpty)).map(ch => FullChapter(ch.get, getByChapter(ch.get.id)))
    chapters
  }
  def unChaptered(entries: Seq[Entry]): Seq[Entry] = entries.filter(_.chapterId.isEmpty)
}

/** HTML generators for a handbook people can download */
object HandbookHtml {
  def entries(entries: Seq[Entry]): String = {
    if (entries.nonEmpty) entries.map(e => s"<p>${e.content}</p>").reduceLeft((a, b) => a ++ b)
    else ""
  }

  def chapter(entries: Seq[Entry], title: Option[String], number: Int): String = {
    val entriesString = this.entries(entries)
    val titleString = s"<h4>Chapter $number" ++ (title match { case None => "" case Some(str) => s": $str"}) ++ "</h4>"
    s"$titleString$entriesString"
  }

  def fullChapters(chapters: Seq[FullChapter]): String = {
    if (chapters.nonEmpty) chapters.map(fch =>
      s"<div>${chapter(fch.entries, fch.chapter.title, fch.chapter.number)}</div>").reduceLeft((a, b) => a ++ b)
    else ""
  }

  def all(chapters: Seq[FullChapter], unchaptered: Seq[Entry]) =
    fullChapters(chapters) ++ "<h3>Unchaptered:</h3>" ++ entries(unchaptered)

  def htmlChapter(entries: Seq[Entry], title: Option[String], number: Int) = s"<html><body>${chapter(entries, title, number)}</body></html>"
  def htmlAll(chapters: Seq[FullChapter], unchaptered: Seq[Entry]) =
    s"<html><body>${all(chapters, unchaptered)}</body></html>"
}