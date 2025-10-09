import sbtassembly.AssemblyPlugin.autoImport.assembly
import sbtassembly.MergeStrategy
import sbtassembly.PathList

import org.mojoz.metadata.ViewDef
import org.mojoz.querease.Querease
import scala.collection.immutable
import org.mojoz.metadata.out.DdlGenerator

import sbt.Project.inConfig
import sbt.Defaults.testSettings

ThisBuild / scalaVersion := "2.13.16"
ThisBuild / version      := "0.1.0-SNAPSHOT"

val wabaseVersion      = "8.0.0-RC15-SNAPSHOT"
val pekkoHttpVersion   = "1.1.0"
val comSunActivationV  = "2.0.1"
val comSunMailV        = "2.0.1"

lazy val dependencies = Seq(
  "org.wabase"                  %% "wabase"               % wabaseVersion,
  "org.bouncycastle"            %  "bcprov-jdk18on"       % "1.80",
  "org.bouncycastle"            %  "bcpkix-jdk18on"       % "1.80",
  "com.github.jwt-scala"        %% "jwt-core"             % "10.0.4",
  "com.github.jwt-scala"        %% "jwt-json-common"      % "10.0.4",
  "io.github.samueleresca"      %% "pekko-quartz-scheduler"% "1.3.0-pekko-1.1.x",

  // swagger: exclude alternate jakarta activation if it is pulled
  "io.swagger.core.v3"          %  "swagger-jaxrs2-jakarta" % "2.2.34" exclude("jakarta.activation","jakarta.activation-api"),

  "org.xhtmlrenderer"           %  "flying-saucer-pdf"     % "9.11.2",

  // Keep simple-java-mail: exclude other mail/activation
  "org.simplejavamail"          %  "simple-java-mail"      % "8.12.6"
    exclude("org.eclipse.angus","angus-mail")
    exclude("org.eclipse.angus","angus-activation")
    exclude("jakarta.mail","jakarta.mail-api")
    exclude("jakarta.activation","jakarta.activation-api")
    exclude("com.sun.activation","jakarta.activation"),

  "org.graalvm.js"              %  "js"                    % "22.3.5",
  "org.graalvm.js"              %  "js-scriptengine"       % "22.3.5",

  // activation + mail implementation --> keep (com.sun.*)
  "com.sun.activation"          %  "jakarta.activation"    % comSunActivationV,
  "com.sun.mail"                %  "jakarta.mail"          % comSunMailV
)

lazy val testsDependencies = Seq(
  "org.scalatest" %% "scalatest" % "3.2.19" % Test
)

lazy val integrationTestDependencies = Seq(
  "org.wabase" %% "wabase" % wabaseVersion % Test classifier "tests"
)

lazy val commonSettings = Seq(
  scalacOptions := Seq(
    "-unchecked",
    "-deprecation",
    "-feature",
    "-encoding", "utf8"
  ),
  Test / fork := true,
  Test / testOptions += Tests.Argument(TestFrameworks.ScalaTest, "-oDS")
)

lazy val mojozSettings = Seq(
  mojozDbNaming := identity,
  mojozDtosPackage := "dto",
  mojozDtosImports := Seq(
    "org.tresql._",
    "org.wabase.{ Dto, DtoWithId }"
  ),
  mojozShouldCompileViews := true,
  mojozQuerease := new Querease {
    override lazy val tableMetadata = mojozTableMetadata.value
    override lazy val yamlMetadata = mojozRawYamlMetadata.value
    override lazy val metadataConventions = mojozMdConventions.value
    override lazy val typeDefs = mojozTypeDefs.value
    override lazy val resourceLoader = mojozResourceLoader.value
    override lazy val viewDefLoader = new org.mojoz.metadata.in.YamlViewDefLoader(
      tableMetadata, yamlMetadata, joinsParser, metadataConventions, uninheritableExtras, typeDefs
    ) {
      override protected def transformRawViewDefs(raw: immutable.Seq[ViewDef]): immutable.Seq[ViewDef] = raw
    }
  }
)

lazy val assemblySettings = Seq(
  assembly / assemblyMergeStrategy := {
    case PathList("META-INF", "versions", rest @ _*) if rest.lastOption.contains("module-info.class") =>
      MergeStrategy.discard

    case PathList("META-INF", "versions", "9", "OSGI-INF", "MANIFEST.MF") =>
      MergeStrategy.last

    case "module-info.class" | "application.conf" | "LICENSE-2.0.txt" =>
      MergeStrategy.discard

    case "reference.conf" =>
      MergeStrategy.concat

    case PathList("jakarta", "mail", _ @ _*) =>
      MergeStrategy.first
    case PathList("jakarta", "activation", _ @ _*) =>
      MergeStrategy.first

    case x =>
      val oldStrategy = (assembly / assemblyMergeStrategy).value
      oldStrategy(x)
  }
)

lazy val root = (project in file("."))
  .enablePlugins(MojozPlugin, MojozGenerateSchemaPlugin)
  .settings(
    name := "$name$",
    libraryDependencies ++= dependencies ++ testsDependencies,

    // dependency overrides: force the single com.sun.* activation + mail impl
    dependencyOverrides ++= Seq(
      "com.sun.activation" % "jakarta.activation" % comSunActivationV,
      "com.sun.mail"       % "jakarta.mail"       % comSunMailV
    ),

    resolvers ++= Seq(
      "snapshots" at "https://central.sonatype.com/repository/maven-snapshots",
      "Typesafe Simple Repository" at "https://repo.typesafe.com/typesafe/simple/maven-releases",
      "MavenRepository" at "https://mvnrepository.com"
    ),

    // avoid snapshot churn in CI
    ThisBuild / updateOptions := updateOptions.value.withCachedResolution(true).withLatestSnapshots(false),

    commonSettings,
    mojozSettings,
    assemblySettings,

    mojozSchemaSqlFiles := Seq(
      (LocalRootProject / baseDirectory).value / "db" / "db-schema.sql"
    ),
    mojozSchemaSqlGenerators := Seq(
      DdlGenerator.postgresql(typeDefs = mojozTypeDefs.value)
    ),
    Compile / mainClass := Some("org.wabase.WabaseServer")
  )

lazy val IntegrationTest = config("it") extend Test

lazy val it = (project in file("src/it"))
  .configs(IntegrationTest)
  .settings(inConfig(IntegrationTest)(testSettings))
  .dependsOn(root % "test->test;compile->compile")
  .settings(
    publish / skip := true,
    libraryDependencies ++= (integrationTestDependencies ++ testsDependencies),
    IntegrationTest / javaOptions := Seq("-Xmx2G"),
    IntegrationTest / parallelExecution := false,
    IntegrationTest / resourceDirectory := baseDirectory.value / "resources",
    IntegrationTest / scalaSource := baseDirectory.value / "scala",
    IntegrationTest / testOptions += Tests.Argument(TestFrameworks.ScalaTest, "-h", name.value + "-it-report"),
    IntegrationTest / fork := true,
    IntegrationTest / testOptions += Tests.Argument(TestFrameworks.ScalaTest, "-oDS")
  )
