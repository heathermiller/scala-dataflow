

name := "scala-dataflow"

version := "0.1"

scalaVersion := "2.10.0-RC5"

scalacOptions ++= Seq("-deprecation", "-optimise")

libraryDependencies += "org.scalatest" % "scalatest_2.9.2" % "1.8" % "test"

libraryDependencies += "junit" % "junit" % "4.10" % "test"

libraryDependencies  += "org.scalanlp" % "breeze-math_2.9.2" % "0.1"

resolvers += "Sonatype OSS Snapshots" at
  "https://oss.sonatype.org/content/repositories/snapshots"

libraryDependencies += "com.github.axel22" %% "scalameter" % "0.2"

testFrameworks += new TestFramework("org.scalameter.ScalaMeterFramework")
