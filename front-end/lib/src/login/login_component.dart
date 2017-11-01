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
    if (dataString[0] != "{") { // not JSON
      loggedIn = true;
      identifier = dataString;
    } else {
      loggedIn = false;
      identifier = "";
    }
  }

  Future<Null> login() async {
    Map<String, String> credentials = {"identifier": ident, "password": password};
    String credString = JSON.encode(credentials);
    HttpRequest req = new HttpRequest();
    req.open("POST", "/user/");
    req.onReadyStateChange.listen((_) {
      result = req.responseText;
      check();
    });
    req.setRequestHeader("Content-type", "application/json");
    req.send(credString);
  }

  Future<Null> logout() async {
    HttpRequest.getString("/user/logout");
    check();
  }

  @override
  Future<Null> ngOnInit() async {
    ident = "";
    password = "";
    check();
  }
}