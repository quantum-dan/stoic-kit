# StoicKit
Website and app developed to help Stoics with their practice.

# Features
## Currently implemented

## To be implemented
* Askesis suggestions (created by users, maybe)
* Askesis log
* Reminders to write down daily meditations (with topic suggestions?)
* Meditations log
* Exercises suggestions (maybe from users)
* Exercises log
* Resources page with links to free versions of Stoic texts
* Quotes

# Implementation
* HTTP server tracking all data
* Async website connecting to server
* App connecting to server

## HTTP Server
Tech possibilities:
* akka-http (Scala/Java library): Allows for efficient asynchronous code on the JVM.
* Yesod (Haskell framework): Good performance with all the benefits and costs of Haskell (safety vs learning curve).
* Golang libraries: Simple and straightforward, good performance, not especially elegant but it gets the job done.

Notes:
* Run on AWS EC2 (works well).  FreeBSD is ideal due to its TCP/IP performance, RHEL is also a good option.
* Connect to AWS RDS database, MariaDB is easy and works fine.
* Less structured data (e.g. texts, if stored directly) would work well with AWS DynamoDB, which supports, notably, Go and Java.

## Website
Dart is a solid choice of a language.  Static types can be used to enforce safety, it's compiled, it's easy to learn,
clean, pragmatic design.  Works well with just the standard library (direct DOM manipulations), also has first-class
support with Angular.  I would lean towards using Angular as the full site is likely to be moderately complicated.

## App
There is the obvious option of native design, with Kotlin, Swift and C#.  Better performance and a more native look,
at he cost of more learning and maintenance.

Alternatively, Google's Flutter looks promising as a cross-platform framework.
