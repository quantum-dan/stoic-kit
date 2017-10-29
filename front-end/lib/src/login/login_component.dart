import 'dart:async';

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

  void login() {
    if (ident == "abc" && password == "password") {
      result = "Success!";
    } else {
      result = "Nope!";
    }
  }

  @override
  Future<Null> ngOnInit() async {
    ident = "";
    password = "";
  }
}