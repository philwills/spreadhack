organization := "com.gu"

name := "newslist"

version := "0.1.0-SNAPSHOT"

libraryDependencies ++= Seq(
   "net.databinder" %% "unfiltered-filter" % "0.5.0",
   "net.databinder" %% "unfiltered-jetty" % "0.5.0",
   "org.clapper" %% "avsl" % "0.3.6",
   "com.google.guava" % "guava" % "10.0.1",
   "net.databinder" %% "unfiltered-spec" % "0.5.0" % "test"
)

resolvers ++= Seq(
  "java m2" at "http://download.java.net/maven/2"
)
