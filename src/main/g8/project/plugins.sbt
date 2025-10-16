addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "2.1.1")

resolvers ++= Seq(
  "snapshots" at "https://central.sonatype.com/repository/maven-snapshots",
)
libraryDependencies += "org.wabase" %% "wabase" % "8.0.0-RC11-SNAPSHOT"

addSbtPlugin("org.mojoz" % "sbt-mojoz" % "5.0.0-RC3-SNAPSHOT")

addSbtPlugin("nl.gn0s1s" % "sbt-dotenv" % "3.2.0")

addSbtPlugin("io.spray" % "sbt-revolver" % "0.10.0")



