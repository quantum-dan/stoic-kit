import 'dart:async';
import 'dart:html';
import 'dart:convert';

import 'package:angular/angular.dart';
import 'package:angular_components/angular_components.dart';

@Component(
  selector: "quote",
  templateUrl: "quotes_component.html",
  directives: const [
    CORE_DIRECTIVES,
    materialDirectives
  ]
)
class QuoteComponent implements OnInit {
  String author = "";
  String content = "";
  QuoteComponent();

  Future<Null> load() async {
    String jsonString = await HttpRequest.getString("/quote");
    Map data = JSON.decode(jsonString);
    author = data["author"];
    content = data["content"];
  }

  @override
  Future<Null> ngOnInit() async {
    author = "";
    content = "";
  }
}