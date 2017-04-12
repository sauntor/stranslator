name := "stranslator"
organization := "com.lingcreative"
organizationName := "LingCreative Studio"
version := "1.2.0"

scalaVersion := "2.12.1"
crossScalaVersions := Seq("2.11.8", "2.12.1")

description := "A most lightweight library for translating you application to the local languages."
homepage := Some(url(s"https://github.com/sauntor/stranslator"))
licenses := Seq(("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.html")))
scmInfo := Some(ScmInfo(
  url("https://github.com/sauntor/stranslator"),
  "https://github.com/sauntor/stranslator.git",
  Some("https://github.com/sauntor/stranslator.git")
))
developers := List(
  Developer("sauntor", "适然(Sauntor)", "sauntor@yeah.net", url("http://github.com/sauntor"))
)

sonatypeProfileName := organization.value
useGpg := true

libraryDependencies += "org.scala-lang.modules" %% "scala-xml" % "1.0.6"
libraryDependencies += "org.specs2" %% "specs2-core" % "3.8.8" % Test
