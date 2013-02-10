name := "restrecord"

liftVersion <<= liftVersion ?? "2.5-M4"

version <<= liftVersion apply { _ + "-1.1" }

organization := "net.liftmodules"
 
scalaVersion := "2.9.2"
  
crossScalaVersions := Seq("2.9.2")

resolvers += "CB Central Mirror" at "http://repo.cloudbees.com/content/groups/public"

resolvers += "Java.net Maven2 Repository" at "http://download.java.net/maven/2/"

libraryDependencies <++= liftVersion { v =>
  "net.liftweb"             %% "lift-record"        % v        % "compile->default" ::
  "net.databinder.dispatch" %% "dispatch-core"      % "0.9.4"  % "compile->default" ::
  "net.databinder.dispatch" %% "dispatch-lift-json" % "0.9.4"  % "compile->default" ::
   Nil
}


//libraryDependencies <++= scalaVersion { sv =>
//  "org.apache.sanselan" % "sanselan" % "0.97-incubator" ::
//   (sv match {
//     case "2.9.2" | "2.9.1" | "2.9.1-1" => "org.scala-tools.testing" % "specs_2.9.1" % "1.6.9" % "test"
//     case _ => "org.scala-tools.testing" %% "specs" % "1.6.8" % "test"
//     }) ::
//   (sv match {
//     case "2.9.2" => "org.scalacheck" % "scalacheck_2.9.1" % "1.9" % "test"
//     case _ => "org.scalacheck" %% "scalacheck" % "1.9" % "test"
//     }) ::
//     Nil
//}

publishTo <<= version { _.endsWith("SNAPSHOT") match {
  case true  => Some("snapshots" at "https://oss.sonatype.org/content/repositories/snapshots")
  case false => Some("releases" at "https://oss.sonatype.org/service/local/staging/deploy/maven2")
 }
} 

credentials += Credentials( file("/private/liftmodules/sonatype.credentials") )

publishMavenStyle := true

publishArtifact in Test := false

pomIncludeRepository := { _ => false }


pomExtra := (
        <url>https://github.com/kaiserpelagic/restrecord</url>
        <licenses>
            <license>
              <name>Apache 2.0 License</name>
              <url>http://www.apache.org/licenses/LICENSE-2.0.html</url>
              <distribution>repo</distribution>
            </license>
         </licenses>
         <scm>
            <url>git@github.com:kaiserpelagic/restrecord.git</url>
            <connection>scm:git:git@github.com:kaiserpelagic/restrecord.git</connection>
         </scm>
         <developers>
            <developer>
              <id>kaiserpelagic</id>
              <name>Greg</name>
              <url>https://github.com/kaiserpelagic</url>
            </developer>
         </developers> 
 )
