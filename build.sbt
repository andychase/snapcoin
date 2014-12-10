

packageArchetype.java_application

name := "photomoney"

resolvers +=
    "Blockchain Snapshots" at "https://raw.githubusercontent.com/blockchain/api-v1-client-java/mvn-repo/"

libraryDependencies ++= Seq(
    "commons-lang" % "commons-lang" % "2.6",
    "com.google.zxing" % "core" % "3.1.0",
    "javax.mail" % "mail" % "1.4.7",
    "info.blockchain" % "api" % "1.0.2",
    "com.googlecode.libphonenumber" % "libphonenumber" % "7.0",
    "org.bitcoinj" % "bitcoinj-core" % "0.12.2",
    "commons-codec" % "commons-codec" % "1.10",
    "com.google.zxing" % "javase" % "3.1.0",
    "com.mashape.unirest" % "unirest-java" %  "1.3.27"
)


libraryDependencies ++= {
    val akkaV = "2.3.6"
    val sprayV = "1.3.2"
    Seq(
        "io.spray"            %%  "spray-can"     % sprayV,
        "io.spray"            %%  "spray-routing" % sprayV,
        "io.spray"            %%  "spray-testkit" % sprayV  % "test",
        "com.typesafe.akka"   %%  "akka-actor"    % akkaV,
        "com.typesafe.akka"   %%  "akka-testkit"  % akkaV   % "test",
        "org.specs2"          %%  "specs2-core"   % "2.3.7" % "test"
    )
}
