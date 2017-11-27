import "dart:html";
import "dart:async";
import "dart:convert";

import "package:angular/angular.dart";
import "package:angular_components/angular_components.dart";

import "view_component.dart";

@Component(
  selector: "exercises_add",
  templateUrl: "add_component.html",
  directives: const [CORE_DIRECTIVES, materialDirectives]
)
class ExercisesAddComponent extends OnInit {
  ExerciseForm formData = new ExerciseForm();
  bool alsoLog = false;
  String url() => alsoLog ? "/exercises/" : "/exercises/new";
  List<String> virtueList = ["Wisdom", "Justice", "Fortitude", "Temperance"];
  List<String> disciplineList = ["Desire", "Assent", "Action"];
  List<String> typeList = ["Askesis", "Meditation", "General"];

  Element durationStr() => document.getElementById("duration");
  Element alsoLogCheck() => document.getElementById("log");
  Element recommended() => document.getElementById("recommend");

  bool incomplete() => formData.title.isEmpty || formData.description.isEmpty ||
      formData.types.isEmpty || durationStr().value.isEmpty;

  Future<Null> send() async {
    alsoLog = alsoLogCheck().checked;
    formData.durationStr = durationStr().value;
    formData.recommended = recommended().checked;
    String data = formData.toJson();
    print(data);
    HttpRequest req = new HttpRequest();
    req.open("POST", url());
    req.setRequestHeader("Content-type", "application/json");
    req.onReadyStateChange.listen((_) => print(req.responseText));
    req.send(data);
    formData = new ExerciseForm();
    alsoLogCheck().checked = false;
    recommended().checked = false;
    durationStr().value = "";

  }

  Future<Null> ngOnInit() async {}
}

class ExerciseForm {
  String title = "";
  String description = "";
  List<String> types = new List();
  List<String> virtues = new List();
  List<String> disciplines = new List();
  bool recommended = false;
  String durationStr = "";
  ExerciseForm() {}

  String toJson() {
    Map data = {
      "title": title, "description": description,
      "types": Conversions.toTypes(types),
      "virtues": Conversions.toVirtues(virtues),
      "disciplines": Conversions.toDisciplines(disciplines),
      "duration": int.parse(durationStr),
      "recommended": recommended
    };
    return JSON.encode(data);
  }
}