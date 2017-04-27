name := "reactify"
organization in ThisBuild := "com.outr"
version in ThisBuild := "1.5.3"
scalaVersion in ThisBuild := "2.12.1"
crossScalaVersions in ThisBuild := List("2.12.1", "2.11.8")

lazy val root = project.in(file("."))
  .aggregate(js, jvm)
  .settings(
    publish := {},
    publishLocal := {}
  )

lazy val reactify = crossProject.in(file("."))
  .settings(
    libraryDependencies += "org.scalatest" %%% "scalatest" % "3.0.3" % "test"
  )
lazy val js = reactify.js
lazy val jvm = reactify.jvm