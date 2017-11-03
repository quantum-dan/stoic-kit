import 'dart:async';
import 'dart:html';
import 'dart:convert';
import "dart:collection";

import 'package:angular/angular.dart';
import 'package:angular_components/angular_components.dart';

const WAIT_TIME = const Duration(milliseconds: 500);
const int NUMBER_TO_ADD = 10;

@Component(
  selector: "quotestream",
  templateUrl: "quote_stream_component.html",
  directives: const [
    CORE_DIRECTIVES,
    materialDirectives
  ]
)
class QuoteStreamComponent implements OnInit {
  DoubleLinkedQueue<Quote> quotes = new DoubleLinkedQueue();
  List<Quote> quotesList = new List();

  void load() {
    HttpRequest req = new HttpRequest();
    req.open("GET", "/quote/quotes");
    req.send();
    new Timer(WAIT_TIME, () {
      String output = req.responseText;
      req.abort();
      String outputModified = "[" + output.replaceAll("}{", "},{") + "]"; // Converts it from a bunch of JSON objects without separation to a list of JSON objects
      for (Map m in JSON.decode(outputModified)) {
        quotes.add(new Quote.fromMap(m));
      }
    });
  }

  void update() {
    for(int i = 0; i < NUMBER_TO_ADD; ++i) {
      if (quotes.length <= NUMBER_TO_ADD * 2) {
        load();
      }
      quotesList.add(quotes.removeFirst());
    }
  }

  @override
  Future<Null> ngOnInit() async {
    load();
    new Timer(WAIT_TIME * 2, update);
  }
}

class Quote {
  String author = "";
  String content = "";
  Quote(this.author, this.content);

  void _fromMap(Map map) {
    this.author = map["author"];
    this.content = map["content"];
  }

  Quote.fromMap(Map map) {
    _fromMap(map);
  }

  Quote.fromJson(String json) {
    Map data = JSON.decode(json);
    _fromMap(data);
  }
}