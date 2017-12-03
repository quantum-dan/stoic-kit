# StoicKit
Website and app developed to help Stoics with their practice.

# Features
## Currently implemented
* Quotes
* Meditations log + handbook HTML generator
* Exercises (askesis, meditation, etc) suggestions contributed by users
* Exercises log

## To be implemented
* Reminders to write down daily meditations (with topic suggestions?)
* Resources page with links to free versions of Stoic texts

# Implementation
* HTTP server tracking all data
* Async website connecting to server
* App connecting to server

## HTTP Server
Implemented with Scala using akka-http for HTTP and Slick for SQL data (exercises done with Quill instead).  DynamoDB approach (for texts, if stored on the server instead of just links) to be determined, but
DynamoDB supports JDBC so that should be doable.

Notes:
* Run on AWS EC2 (works well).  FreeBSD is ideal due to its TCP/IP performance, RHEL is also a good option.
* Connect to AWS RDS database, MariaDB is easy and works fine.
* Less structured data (e.g. texts, if stored directly) would work well with AWS DynamoDB, which supports,
notably, Go and Java.

## Website
Implemented with AngularDart--that is, Angular framework, Dart language.
Note on streaming information: grab part of the stream, parse, transfer to a list as necessary (there's not really any reason to stream, it turns out, and Dart doesn't seem to provide a good way to read HTTP streams anyway).

## App
There is the obvious option of native design, with Java/Kotlin, Swift and C#.  Better performance and a more native look,
at he cost of more learning and maintenance.

Alternatively, Google's Flutter looks promising as a cross-platform framework.
