name := "sohu-registry-machine"

version := "1.0"

lazy val `registry-machine` = (project in file(".")).enablePlugins(PlayJava)

scalaVersion := "2.11.1"

//libraryDependencies ++= Seq(javaEbean)

libraryDependencies ++= Seq(
  "commons-io" % "commons-io" % "2.2",
  "com.jayway.jsonpath" % "json-path" % "1.2.0",
  "org.apache.httpcomponents" % "httpclient" % "4.3.2",
  "org.webjars" %% "webjars-play" % "2.3.0-2",
  "org.webjars" % "bootstrap" % "3.3.1",
  "org.webjars" % "jquery" % "2.1.1",
  "org.webjars" % "backbonejs" % "1.1.2-2",
  "org.webjars" % "underscorejs" % "1.7.0",
  "org.webjars" % "dustjs-linkedin" % "2.4.0-1",
  "org.slf4j" % "log4j-over-slf4j" % "1.7.7",
  "com.adrianhurt" %% "play-bootstrap3" % "0.1.1",
  "net.java.dev.jna" % "jna" % "3.4.0"
)

//不发布api doc
sources in (Compile,doc) := Seq.empty

publishArtifact in (Compile, packageDoc) := false

mappings in Universal ++=
  (baseDirectory.value / "scripts" * "*" get) map
    (x => x -> ("scripts/" + x.getName))

unmanagedResourceDirectories in Test <+= baseDirectory(_ / "target/web/public/test")
