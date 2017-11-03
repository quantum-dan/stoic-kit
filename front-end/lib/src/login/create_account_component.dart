import "dart:async";
import "dart:convert";
import "dart:html";

import "package:angular_components/angular_components.dart";
import "package:angular/angular.dart";

@Component(
  selector: "accountform",
  templateUrl: "create_account_component.html",
  directives: const [
    CORE_DIRECTIVES,
    materialDirectives
  ]
)
class AccountFormComponent extends OnInit {
  String identifier = "";
  String password = "";
  String password2 = "";
  String result = "";

  Future<Null> create() async {
    if (identifier.isEmpty || password.isEmpty || password2.isEmpty) {
      result = "Error: all fields must be completed.";
    } else if (password != password2) {
      result = "Error: passwords do not match.";
    } else {
      HttpRequest req = new HttpRequest();
      Map data = {
        "identifier": identifier,
        "password": password
      };
      String dataString = JSON.encode(data);
      req.open("POST", "/user/create");
      req.setRequestHeader("Content-type", "application/json");
      req.onReadyStateChange.listen((_) {
        String res = req.responseText;
        Map resData = JSON.decode(res);
        if (resData["success"]) {
          result = "Success!  Go ahead and log in.";
        } else {
          result = "Error: ${resData['result']}";
        }
        identifier = "";
        password = "";
        password2 = "";
      });
      req.send(dataString);
    }
  }

  @override
  Future<Null> ngOnInit() async {}
}