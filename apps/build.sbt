scalaVersion in ThisBuild := "2.12.1"

organization in ThisBuild := "stanford-ppl"

version in ThisBuild := "1.0"

publishArtifact := false

val paradiseVersion = "2.1.0"

val commonSettings = assemblySettings ++ Seq(
  //paradise
  libraryDependencies += "stanford-ppl" %% "spatial" % version.value,
  
  resolvers += Resolver.sonatypeRepo("snapshots"),
  resolvers += Resolver.sonatypeRepo("releases"),
  addCompilerPlugin("org.scalamacros" % "paradise" % paradiseVersion cross CrossVersion.full)
)

lazy val apps = project
  .settings(commonSettings)
