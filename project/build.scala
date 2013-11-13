import sbt._
import Keys._

import sbtassembly.Plugin._
import AssemblyKeys._

import com.typesafe.sbt.SbtScalariform
import com.typesafe.sbt.SbtScalariform.ScalariformKeys

object Settings {

	lazy val formatSettings = SbtScalariform.scalariformSettings ++ Seq(
		  ScalariformKeys.preferences in Compile := formattingPreferences,
		  ScalariformKeys.preferences in Test := formattingPreferences
		)

  import scalariform.formatter.preferences._

  def formattingPreferences =
    FormattingPreferences()
      .setPreference(RewriteArrowSymbols, true)
      .setPreference(AlignParameters, true)
      .setPreference(AlignSingleLineCaseStatements, true)
      .setPreference(DoubleIndentClassDeclaration, true)
      .setPreference(PlaceScaladocAsterisksBeneathSecondAsterisk, true)

  lazy val scalameter = new TestFramework("org.scalameter.ScalaMeterFramework")

  lazy val common = Defaults.defaultSettings ++ formatSettings ++ Seq(
    version := "0.1",
    scalaVersion := "2.10.3",
    javacOptions ++= Seq("-encoding", "UTF-8", "-source", "1.6", "-target", "1.6"),
    scalacOptions ++= Seq("-Xlint", "-unchecked", "-deprecation", "-feature"),
    parallelExecution in Test := false,
    unmanagedBase <<= baseDirectory(_/"libs"),
    unmanagedResourceDirectories in Compile += file("common/assets"),
    resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",
    libraryDependencies ++= Seq(
      "com.badlogicgames.gdx" % "gdx" % "1.0-SNAPSHOT"
    )
  )

  lazy val desktop = common ++ assemblySettings ++ Seq(
    unmanagedResourceDirectories in Compile += file("common/assets"),
    fork in Compile := true,
    libraryDependencies ++= Seq(
      "com.badlogicgames.gdx" % "gdx-backend-lwjgl" % "1.0-SNAPSHOT",
      "com.badlogicgames.gdx" % "gdx-platform" % "1.0-SNAPSHOT" classifier "natives-desktop"
    )
  )


  lazy val assemblyOverrides = Seq(
    mainClass in assembly := Some("com.voxzom.Main"),
    AssemblyKeys.jarName in assembly := "voxelzombie-0.1.jar"
  )

  lazy val nativeExtractions = SettingKey[Seq[(String, NameFilter, File)]]("native-extractions", "(jar name partial, sbt.NameFilter of files to extract, destination directory)")
  lazy val extractNatives = TaskKey[Unit]("extract-natives", "Extracts native files")
  lazy val natives = Seq(
    ivyConfigurations += config("natives"),
    nativeExtractions := Seq.empty,
    extractNatives <<= (nativeExtractions, update) map { (ne, up) =>
      val jars = up.select(configurationFilter("natives"))
      ne foreach { case (jarName, fileFilter, outputPath) =>
        jars find(_.getName.contains(jarName)) map { jar =>
            IO.unzip(jar, outputPath, fileFilter)
        }
      }
    },
    compile in Compile <<= (compile in Compile) dependsOn (extractNatives)
  )
}

object LibgdxBuild extends Build {
  lazy val common = Project(
    "common",
    file("common"),
    settings = Settings.common)

  lazy val desktop = Project(
    "desktop",
    file("desktop"),
    settings = Settings.desktop)
    .dependsOn(common)
    .settings(Settings.assemblyOverrides: _*)


  lazy val all = Project(
    "all-platforms",
    file("."),
    settings = Settings.common
  ) aggregate(common, desktop)
}
