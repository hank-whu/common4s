organization  := "common4s"

name		  := "common4s"

version       := "0.1"

scalaVersion  := "2.11.7"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

javacOptions ++= Seq("-encoding", "UTF-8")

resolvers ++= Seq(
  "Central Repository" 	at 	"http://repo.maven.apache.org/maven2",
  "typesafe" 	at 	"http://repo.typesafe.com/typesafe/repo/"
)

libraryDependencies ++= Seq(
  "org.scala-lang" 					% 	"scala-library" 		% "2.11.7",
  "org.scala-lang" 					% 	"scala-compiler" 		% "2.11.7",
  "org.scala-lang" 					% 	"scala-reflect" 		% "2.11.7",
  "org.javassist" 					% 	"javassist" 			% "3.20.0-GA"
)
