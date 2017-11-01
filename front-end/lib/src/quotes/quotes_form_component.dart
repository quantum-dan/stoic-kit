import 'dart:async';
import 'dart:html';
import 'dart:convert';

import 'package:angular/angular.dart';
import 'package:angular_components/angular_components.dart';

@Component(
    selector: "quoteform",
    templateUrl: "quote_form.html",
    directives: const [CORE_DIRECTIVES, materialDirectives])
class QuoteFormComponent implements OnInit {
  String author = "";
  String content = "";
  String result = "";

  QuoteFormComponent();

  Future<Null> submit() async {
    if (author.isEmpty || content.isEmpty) {
      result = "Error: author and quote must be filled out.";
    } else {
      Map<String, dynamic> jsonMap = {
        "author": author,
        "content": content,
        "id": 0
      };
      String jsonString = JSON.encode(jsonMap);
      HttpRequest req = new HttpRequest();
      req.open("POST", "/quote/");
      req.onReadyStateChange.listen((_) => result = req.responseText);
      req.setRequestHeader("Content-type", "application/json");
      req.send(jsonString);
    }
  }

  @override
  Future<Null> ngOnInit() async { }
}
