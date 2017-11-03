import "dart:async";
import "dart:html";
import "dart:convert";

import "package:angular/angular.dart";
import "package:angular_components/angular_components.dart";

@Component(
  selector: "handbook_main",
  templateUrl: "main_component.html",
  directives: const [
    CORE_DIRECTIVES,
    materialDirectives
  ]
)
class MainHandbookComponent implements OnInit {
  bool loggedIn = false;
  List<Entry> entries = new List();
  String content = "";
  String result = "";

  Future<Null> create() async {
    HttpRequest req = new HttpRequest();
    req.open("POST", "/handbook/");
    req.setRequestHeader("Content-type", "application/json");
    req.onReadyStateChange.listen((_) {
      loadEntries();
    });
    req.send(new Entry(content).toJson());
    content = "";
  }

  Future<Null> loadEntries() async {
    entries = JSON.decode(await HttpRequest.getString("/handbook/")).map((e) =>
      new Entry.fromMap(e)).toList().reversed.toList();
  }

  @override
  Future<Null> ngOnInit() async {
    loggedIn = JSON.decode(await HttpRequest.getString("/user/"))["success"];
    loadEntries();
  }
}

class Entry {
  String content;

  void create(String content) => this.content = content;
  void _fromMap(Map map) => create(map["content"]);

  String toJson() => JSON.encode({"id": 0, "content": content, "userId": 0, "chapterId": null});

  Entry(this.content);
  Entry.fromMap(Map map) {
    _fromMap(map);
  }
  Entry.fromJson(String json) {
    _fromMap(JSON.decode(json));
  }
}