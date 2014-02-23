import sbt._
import sbt.Keys._

object LiftModuleBuild extends Build {

  val liftVersion = SettingKey[String]("liftVersion", "2.5.1")

  val project = Project("LiftModule", file("."))

}
