# StoicKit
Website and app developed to help Stoics with their practice.

# Features
## Currently implemented
* Quotes
* Meditations log + handbook generator

## To be implemented
* Askesis suggestions (created by users, maybe)
* Askesis log
* Reminders to write down daily meditations (with topic suggestions?)
* Exercises suggestions (maybe from users)
* Exercises log
* Resources page with links to free versions of Stoic texts

# Implementation
* HTTP server tracking all data
* Async website connecting to server
* App connecting to server

## HTTP Server
Implemented with Scala using akka-http for HTTP and Slick for SQL data.  DynamoDB approach to be determined, but
DynamoDB supports JDBC so that should be doable.

Notes:
* Run on AWS EC2 (works well).  FreeBSD is ideal due to its TCP/IP performance, RHEL is also a good option.
* Connect to AWS RDS database, MariaDB is easy and works fine.
* Less structured data (e.g. texts, if stored directly) would work well with AWS DynamoDB, which supports,
notably, Go and Java.

## Website
Implemented with AngularDart--that is, Angular framework, Dart language.
Note on streaming information: grab part of the stream, parse, transfer to a list as necessary.

## App
There is the obvious option of native design, with Java/Kotlin, Swift and C#.  Better performance and a more native look,
at he cost of more learning and maintenance.

Alternatively, Google's Flutter looks promising as a cross-platform framework.
