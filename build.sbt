import java.net.URL

name          := "Popeye"
organization  := "com.popeyepdf"
version       := "0.1-SNAPSHOT"
scalaVersion  := "2.11.6"
developers    := List(
  Developer(
    name  = "Yoel",
    email = "yoeluk@gmail.com",
    id    = "yrgd",
    url   = new URL("http://")
  )
)

scalacOptions := Seq(
  "-unchecked",
  "-deprecation",
  "-target:jvm-1.7",
  "-encoding", "UTF-8",
  "-Ywarn-dead-code",
  "-language:implicitConversions",
  "-language:postfixOps",
  "-language:higherKinds",
  "-feature"
)

resolvers ++= Seq(
  "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
  "Sonatype snapshots"  at "http://oss.sonatype.org/content/repositories/snapshots/",
  "Spray Repository"    at "http://repo.spray.io",
  "Spray Nightlies"     at "http://nightlies.spray.io/"
)

libraryDependencies ++= {
  val akkaV = "2.3.9"
  val sprayV = "1.3.3"
  Seq(
    "io.spray"            %%  "spray-can"                %  sprayV,
    "io.spray"            %%  "spray-client"             %  sprayV,
    "io.spray"            %%  "spray-routing"            %  sprayV,
    "io.spray"            %%  "spray-testkit"            %  sprayV    % "test",
    "io.spray"            %%  "spray-json"               %  "1.3.1",
    "com.typesafe.akka"   %%  "akka-actor"               %  akkaV,
    "com.typesafe.akka"   %%  "akka-testkit"             %  akkaV     % "test",
    "com.typesafe.akka"   %%  "akka-slf4j"               %  akkaV,
    "org.scalatest"       %%  "scalatest"                %  "2.2.0"   % "test",
    "org.apache.pdfbox"    %  "pdfbox"                   %  "1.8.8",
    "org.bouncycastle"     %  "bcprov-jdk15"             %  "1.44",
    "org.bouncycastle"     %  "bcmail-jdk15"             %  "1.44",
    "com.ibm.icu"          %  "icu4j"                    %  "3.8",
    "org.apache.lucene"    %  "lucene-core"              %  "5.0.0",
    "org.apache.lucene"    %  "lucene-analyzers-common"  %  "5.0.0",
    "org.apache.lucene"    %  "lucene-queryparser"       %  "5.0.0",
    "org.apache.lucene"    %  "lucene-queries"           %  "5.0.0",
    "org.apache.lucene"    %  "lucene-test-framework"    %  "5.0.0",
    "ch.qos.logback"       %  "logback-classic"          %  "1.1.2"
  )
}

enablePlugins(JavaAppPackaging)

Seq(Revolver.settings: _*)

assemblyJarName in assembly := "Popeyepdf-0.1-SNAPSHOT.jar"
mainClass in assembly       := Some("com.popeyepdf.Boot")
test in assembly            := {}
fork in Test                := true