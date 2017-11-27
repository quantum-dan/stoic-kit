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
  List<Exercise> exercises = new List();
  bool loggedIn = false;
  Filters filters = new Filters();

  List<String> allTypes = Conversions.fromTypes(7);
  List<String> allVirtues = Conversions.fromVirtues(15);
  List<String> allDisciplines = Conversions.fromDisciplines(7);

  Future<Null> loadFiltered() async {
    exercises = await filters.send();
  }

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
  UseFilter owner = new UseFilter(new Filter("owner", "Owner", true, false, false));
  UseFilter title = new UseFilter(new Filter("title", "Title", false, true, false));
  UseFilter types = new UseFilter(new Filter("types", "Exercise Types", true, true, false));
  UseFilter virtues = new UseFilter(new Filter("virtues", "Virtues", true, true, false));
  UseFilter disciplines = new UseFilter(new Filter("disciplines", "Disciplines", true, true, false));
  UseFilter minCompletions = new UseFilter(new Filter("min-completions", "Minimum Completions", true, false, false));
  UseFilter maxCompletions = new UseFilter(new Filter("max-completions", "Maximum Completions", true, false, false));
  UseFilter upvotes = new UseFilter(new Filter("upvotes", "Minimum Upvotes", true, false, false));
  UseFilter ratio = new UseFilter(new Filter("ratio", "Minimum upvotes:total votes ratio", false, false, true));

  int getInt(String id) => int.parse(document.getElementById(id).value);
  double getDouble(String id) => double.parse(document.getElementById(id).value);

  void setMinCompletions() {
    try {
      int val = getInt("min-comp");
      if (val > 0) {
        minCompletions.filter.intData = val;
        minCompletions.use = true;
      } else minCompletions.use = false;
    } catch (e) {
      minCompletions.use = false;
      print(e);
    }
  }
  void setMaxCompletions() {
    try {
      int val = getInt("max-comp");
      if (val > 0) {
        maxCompletions.filter.intData = val;
        maxCompletions.use = true;
      } else maxCompletions.use = false;
    } catch (e) {
      maxCompletions.use = false;
      print(e);
    }
  }
  void setUpvotes() {
    try {
      int val = getInt("upvotes");
      if (val > 0) {
        upvotes.filter.intData = val;
        upvotes.use = true;
      } else upvotes.use = false;
    } catch (e) {
      upvotes.use = false;
      print(e);
    }
  }
  void setRatio() {
    try {
      double val = getDouble("ratio");
      if (val > 0.0) {
        ratio.filter.doubleData = val;
        ratio.use = true;
      } else ratio.use = false;
    } catch (e) {
      ratio.use = false;
      print(e);
    }
  }

  List<String> virtueItems = new List();
  List<String> typeItems = new List();
  List<String> disciplineItems = new List();

  void encodeLists() {
    virtues.filter.intData = Conversions.toVirtues(virtueItems);
    if (virtues.filter.intData > 0) virtues.use = true;
    else virtues.use = false;
    types.filter.intData = Conversions.toTypes(typeItems);
    if (types.filter.intData > 0) types.use = true;
    else types.use = false;
    disciplines.filter.intData = Conversions.toDisciplines(disciplineItems);
    if (disciplines.filter.intData > 0) disciplines.use = true;
    else disciplines.use = false;
  }

  Future<List<Exercise>> send() async {
    print("Running filters...");
    encodeLists();
    print("Lists encoded");
    setMaxCompletions();
    print("Max completions set");
    setMinCompletions();
    print("Min completions set");
    setRatio();
    print("Ratio set");
    setUpvotes();
    print("Upvotes set");
    List<UseFilter> filters = [owner, title, types, virtues, disciplines, minCompletions,
      maxCompletions, upvotes, ratio];
    String query = "?";
    filters.retainWhere((f) => f.use || (!f.filter.use_int && !f.filter.use_double && f.filter.strData.isNotEmpty));
    print("Filters filtered");
    filters.forEach((f) => query += f.filter.queryData());
    print("Query prepared; query is $query");
    String response = await HttpRequest.getString("/exercises/filter/$query");
    List<Map> data = JSON.decode(response);
    print("Data parsed");
    return data.map((item) => new Exercise.fromMap(item)).toList();
  }

  Filters();
}

class UseFilter {
  bool use = false;
  Filter filter;

  UseFilter(this.filter);
}

class Filter {
  String name;
  String display;
  String strData = "";
  int intData;
  double doubleData;
  bool use_int;
  bool use_double;
  bool use_exact;
  bool exact = false;

  String data() => use_int ? intData.toString() : use_double ? doubleData.toString() : strData;
  String exactData() => use_exact ? "$name-exact=${exact.toString()}&" : "";
  String mainData() => "$name=${data()}&";

  String queryData() => mainData() + exactData();

  Filter(this.name, this.display, this.use_int, this.use_exact, this.use_double);
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