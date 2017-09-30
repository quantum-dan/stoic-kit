name := "StoicKit"
version := "0.1"
scalaVersion := "2.12.3"

libraryDependencies ++= Seq(
	"com.typesafe.akka" %% "akka-http" % "10.0.10",
	"com.typesafe.akka" %% "akka-http-spray-json" % "10.0.10",
	"com.typesafe.akka" %% "akka-stream" % "2.5.4",
	"com.typesafe.akka" %% "akka-actor" % "2.5.4",
  "com.typesafe" % "config" % "1.3.1",
	"com.typesafe.slick" %% "slick" % "3.2.1",
	"org.slf4j" % "slf4j-nop" % "1.6.4", // For slick
	"com.typesafe.slick" %% "slick-hikaricp" % "3.2.1", // For Slick
  "mysql" % "mysql-connector-java" % "5.1.+"
)

mainClass in Compile := Some("stoickit.StoicKit")

exportJars := true

artifactName := { (sv: ScalaVersion, module: ModuleID, artifact: Artifact) =>
    artifact.name + "-" + module.revision + "." + artifact.extension
}

assemblyJarName in assembly := "stoic-kit.jar"
mainClass in assembly := Some("stoickit.StoicKit")
