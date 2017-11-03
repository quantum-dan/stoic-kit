import 'dart:async';
import 'dart:html';
import 'dart:convert';

import 'package:angular/angular.dart';
import 'package:angular_components/angular_components.dart';

@Component(
  selector: 'login',
  templateUrl: 'login_component.html',
  directives: const [
    CORE_DIRECTIVES,
    materialDirectives
  ]
)
class LoginComponent implements OnInit {
  String ident = "";
  String password = "";
  String result = "Enter credentials";
  bool loggedIn = false;
  String identifier = "";
  LoginComponent();

  Future<Null> check() async {
    String dataString = await HttpRequest.getString("/user/");
    Map data = JSON.decode(dataString);
    if (data["success"]) {
      loggedIn = true;
      identifier = data["result"];
    } else {
      loggedIn = false;
      identifier = "";
      result = "Enter credentials";
    }
  }

  Future<Null> login() async {
    Map<String, String> credentials = {"identifier": ident, "password": password};
    String credString = JSON.encode(credentials);
    HttpRequest req = new HttpRequest();
    req.open("POST", "/user/");
    req.onReadyStateChange.listen((_) {
      Map data = JSON.decode(req.responseText);
      if (data["success"] != false) { // Because it will be null, not true, if successful
        result = "Success!";
        check();
      } else {
        result = "Error: ${data['result']}";
      }
    });
    req.setRequestHeader("Content-type", "application/json");
    req.send(credString);
  }

  Future<Null> logout() async {
    ident = "";
    password = "";
    HttpRequest req = new HttpRequest();
    req.open("GET", "/user/logout");
    req.onReadyStateChange.listen((_) => check());
    req.send();
  }

  @override
  Future<Null> ngOnInit() async {
    ident = "";
    password = "";
    check();
  }
}