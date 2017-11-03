import 'dart:async';
import 'dart:html';
import 'dart:convert';

import 'package:angular/angular.dart';
import 'package:angular_components/angular_components.dart';

import "quotes_form_component.dart";
import "quotes_stream_component.dart";

@Component(
  selector: "quote",
  templateUrl: "quotes_component.html",
  directives: const [
    CORE_DIRECTIVES,
    materialDirectives,
    QuoteFormComponent,
    QuoteStreamComponent
  ]
)
class QuoteComponent implements OnInit {
  String author = "";
  String content = "";
  QuoteComponent();
  bool isAdmin = false;

  Future<Null> load() async {
    String jsonString = await HttpRequest.getString("/quote/");
    Map data = JSON.decode(jsonString);
    author = data["author"];
    content = data["content"];
  }

  @override
  Future<Null> ngOnInit() async {
    load();
    isAdmin = JSON.decode(await HttpRequest.getString("/user/admin"))["success"];
  }
}
