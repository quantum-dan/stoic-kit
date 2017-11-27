import 'package:angular/angular.dart';
import 'package:angular_components/angular_components.dart';
import 'package:angular_router/angular_router.dart';

import 'src/todo_list/todo_list_component.dart';
import "src/login/login_component.dart";
import "src/quotes/quotes_component.dart";
import "src/login/create_account_component.dart";
import "src/quotes/quotes_stream_component.dart";
import "src/handbook/main_component.dart";
import "src/exercises/main_component.dart";

// AngularDart info: https://webdev.dartlang.org/angular
// Components info: https://webdev.dartlang.org/components

@Component(
  selector: 'my-app',
  styleUrls: const ['app_component.css'],
  templateUrl: 'app_component.html',
  directives: const [materialDirectives, TodoListComponent, LoginComponent,
    QuoteComponent, ROUTER_DIRECTIVES, AccountFormComponent, QuoteStreamComponent,
    MainHandbookComponent, ExercisesComponent],
  providers: const [materialProviders],
)
@RouteConfig(const[
  const Route(path: "/login", name: "Login", component: LoginComponent),
  const Route(path: "/quotes", name: "Quotes", component: QuoteComponent),
  const Route(path: "/create", name: "Create Account", component: AccountFormComponent),
  const Route(path: "/handbook", name: "Handbook", component: MainHandbookComponent),
  const Route(path: "/exercises", name: "Exercises", component: ExercisesComponent)
])
class AppComponent {
}
