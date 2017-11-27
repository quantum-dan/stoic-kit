import "dart:html";
import "dart:convert";
import "dart:async";

import "package:angular/angular.dart";
import "package:angular_components/angular_components.dart";

import "add_component.dart";
import "view_component.dart";

@Component(
  selector: "exercises",
  templateUrl: "main_component.html",
  directives: const [CORE_DIRECTIVES, materialDirectives,
    ExercisesViewComponent, ExercisesAddComponent]
)
class ExercisesComponent extends OnInit {
  bool loggedIn = false;

  Future<Null> ngOnInit() async {
    loggedIn = JSON.decode(await HttpRequest.getString("/user/"))["success"];
  }
}