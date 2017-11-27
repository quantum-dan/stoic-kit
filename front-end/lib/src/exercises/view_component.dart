import "dart:html";
import "dart:async";
import "dart:convert";

import "package:angular/angular.dart";
import "package:angular_components/angular_components.dart";

@Component(
  selector: "exercises_view",
  templateUrl: "view_component.html",
  directives: const [CORE_DIRECTIVES, materialDirectives]
)
class ExercisesViewComponent extends OnInit {
  Map<String, String> filters;
  List<Exercise> exercises = new List();
  bool loggedIn = false;

  Future<Null> load() async {
    String json = await HttpRequest.getString("/exercises/");
    List<Map> data = JSON.decode(json);
    exercises = new List();
    data.forEach((d) => exercises.add(new Exercise.fromMap(d)));
  }

  Future<Null> log(int id) async {
    HttpRequest.getString("/exercises/log/$id");
  }

  @override
  Future<Null> ngOnInit() async {
    loggedIn = JSON.decode(await HttpRequest.getString("/user/"))["success"];
    load();
  }
}

class Filters {
  int owner;
  String title;
}

class Exercise {
  int id;
  String title;
  String description;
  List<String> types;
  List<String> virtues;
  List<String> disciplines;
  int duration;
  bool recommended;
  int ownerId = 0;
  int completions = 0;
  int upvotes = 0;
  int downvotes = 0;

  void create(int id, String title, String description, List<String> types,
      List<String> virtues, List<String> disciplines, int duration, bool recommended,
    [int ownerId = 0, int completions = 0, int upvotes = 0, int downvotes = 0]) {
    this.id = id;
    this.title = title;
    this.description = description;
    this.types = types;
    this.virtues = virtues;
    this.disciplines = disciplines;
    this.duration = duration;
    this.recommended = recommended;
    this.ownerId = ownerId;
    this.completions = completions;
    this.upvotes = upvotes;
    this.downvotes = downvotes;
  }

  void _fromMap(Map data) {
    create(
      data["id"], data["title"], data["description"], Conversions.fromTypes(data["types"]),
      Conversions.fromVirtues(data["virtues"]), Conversions.fromDisciplines(data["disciplines"]),
      data["duration"], data["recommended"], data["ownerId"], data["completions"],
      data["upvotes"], data["downvotes"]
    );
  }

  Exercise.fromJson(String json) {
    Map data = JSON.decode(json);
    _fromMap(data);
  }

  Exercise.fromMap(Map data) {
    _fromMap(data);
  }
}

class Conversions {
  static List<String> fromTypes(int types) {
    List<String> result = new List();
    if (types >= 4) result.add("Askesis");
    if (types % 4 >= 2) result.add("Meditation");
    if (types % 6 == 1) result.add("General");
    return result;
  }
  static int toTypes(List<String> types) {
    int result = 0;
    if (types.contains("Askesis")) result += 4;
    if (types.contains("Meditation")) result += 2;
    if (types.contains("General")) result += 1;
    return result;
  }

  static List<String> fromVirtues(int virtues) {
    List<String> result = new List();
    if (virtues >= 8) result.add("Wisdom");
    if (virtues % 8 >= 4) result.add("Justice");
    if (virtues % 12 >= 2) result.add("Fortitude");
    if (virtues % 14 == 1) result.add("Temperance");
    return result;
  }
  static int toVirtues(List<String> virtues) {
    int result = 0;
    if (virtues.contains("Wisdom")) result += 8;
    if (virtues.contains("Justice")) result += 4;
    if (virtues.contains("Fortitude"))result += 2;
    if (virtues.contains("Temperance")) result += 1;
    return result;
  }

  static List<String> fromDisciplines(int disciplines) {
    List<String> result = new List();
    if (disciplines >= 4) result.add("Desire");
    if (disciplines % 4 >= 2) result.add("Assent");
    if (disciplines % 6 == 1) result.add("Action");
    return result;
  }
  static int toDisciplines(List<String> disciplines) {
    int result = 0;
    if (disciplines.contains("Desire")) result += 4;
    if (disciplines.contains("Assent")) result += 2;
    if (disciplines.contains("Action")) result += 1;
    return result;
  }
}