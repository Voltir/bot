
lazy val commonSettings = Seq(
  version := "0.1.0",
  scalaVersion := "2.11.7"
)

lazy val bot = project.in(file("bot"))
  .settings(commonSettings:_*)
  .settings(
    name:="bot",
    resolvers += Resolver.url("AAAAAAAAAAAAA",url("http://maven.inria.fr/artifactory/repo/")),
    libraryDependencies ++= Seq(
      "com.lihaoyi" %% "scalarx" % "0.3.0"
    )
  )