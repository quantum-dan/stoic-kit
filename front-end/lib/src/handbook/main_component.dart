import "dart:async";
import "dart:html";
import "dart:convert";

import "package:angular/angular.dart";
import "package:angular_components/angular_components.dart";

@Component(
    selector: "handbook_main",
    templateUrl: "main_component.html",
    directives: const [CORE_DIRECTIVES, materialDirectives])
class MainHandbookComponent implements OnInit {
  bool loggedIn = false;
  List<Entry> entries = new List();
  List<Chapter> chapters = new List();
  String content = "";
  String result = "";
  String title = "";
  Chapter selectedChapter = null;
  bool showChapters = false;

  void toggleShowChapters() {
    showChapters = !showChapters;
  }

  String chooseChapterLabel() =>
      showChapters ? "Collapse Chapters" : "Show Chapters";

  void setChapter(Chapter chapter) {
    selectedChapter = chapter;
    toggleShowChapters();
  }

  Future<Null> createChapter() async {
    // Max chapter number + 1
    int number = chapters
        .map((chapter) => chapter.number)
        .fold(0, (a, b) => a > b ? a : b) +
        1;
    String json =
    JSON.encode({"userId": 0, "id": 0, "number": number, "title": title});
    title = "";
    HttpRequest req = new HttpRequest();
    req.open("POST", "/handbook/chapter");
    req.setRequestHeader("Content-type", "application/json");
    req.onReadyStateChange.listen((_) => loadEntries());
    req.send(json);
  }

  Future<Null> create() async {
    HttpRequest req = new HttpRequest();
    req.open("POST", "/handbook/");
    req.setRequestHeader("Content-type", "application/json");
    req.onReadyStateChange.listen((_) {
      loadEntries();
    });
    req.send(new Entry(content, selectedChapter != null ? selectedChapter.id : null).toJson());
    content = "";
  }

  Future<Null> loadEntries() async {
    entries = JSON
        .decode(await HttpRequest.getString("/handbook/"))
        .map((e) => new Entry.fromMap(e))
        .toList()
        .reversed
        .toList();
    chapters = JSON
        .decode(await HttpRequest.getString("/handbook/chapters"))
        .map((c) => new Chapter.fromMap(c))
        .toList();
  }

  @override
  Future<Null> ngOnInit() async {
    loggedIn = JSON.decode(await HttpRequest.getString("/user/"))["success"];
    loadEntries();
  }
}

class Entry {
  String content;
  int chapterId = null;

  void create(String content, [int chapterId = null]) {
    this.content = content;
    this.chapterId = chapterId;
  }

  void _fromMap(Map map) => create(map["content"], map["chapterId"]);

  String toJson() => JSON.encode(
      {"id": 0, "content": content, "userId": 0, "chapterId": chapterId});

  Entry(this.content, [this.chapterId = null]);
  Entry.fromMap(Map map) {
    _fromMap(map);
  }
  Entry.fromJson(String json) {
    _fromMap(JSON.decode(json));
  }
}

class Chapter {
  String title;
  int number;
  int id;

  void create(String title, int number, int id) {
    this.title = title;
    this.number = number;
    this.id = id;
  }

  Chapter(this.title, this.number, this.id);
  Chapter.fromMap(Map map) {
    create(map["title"], map["number"], map["id"]);
  }
}

