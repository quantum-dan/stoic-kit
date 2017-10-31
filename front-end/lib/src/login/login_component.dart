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
  LoginComponent();

  Future<Null> login() async {
    Map<String, String> credentials = {"identifier": ident, "password": password};
    String credString = JSON.encode(credentials);
    HttpRequest req = new HttpRequest();
    req.open("POST", "/login");
    req.onReadyStateChange.listen((_) => result = req.responseText);
    req.setRequestHeader("Content-type", "application/json");
    req.send(credString);
  }

  @override
  Future<Null> ngOnInit() async {
    ident = "";
    password = "";
  }
}