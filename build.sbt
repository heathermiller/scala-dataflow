

name := "scala-dataflow"

version := "0.1"

scalaVersion := "2.9.2"

scalacOptions ++= Seq("-deprecation", "-optimise")

libraryDependencies += "org.scalatest" %% "scalatest" % "1.8" % "test"

libraryDependencies += "junit" % "junit" % "4.10" % "test"

libraryDependencies  += "org.scalanlp" %% "breeze-math" % "0.1"
