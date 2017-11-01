import 'package:angular/angular.dart';
import 'package:angular_router/angular_router.dart';

import 'package:front_end/app_component.dart';

void main() {
  bootstrap(AppComponent, [ROUTER_PROVIDERS, provide(LocationStrategy, useClass: HashLocationStrategy)]);
}
