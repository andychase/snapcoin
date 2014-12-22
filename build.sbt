name := "photomoney"

resolvers +=
    "Blockchain Snapshots" at "https://raw.githubusercontent.com/blockchain/api-v1-client-java/mvn-repo/"

libraryDependencies ++= Seq(
    "commons-lang" % "commons-lang" % "2.6",
    "org.apache.commons" % "commons-lang3" % "3.3.2",
    "com.google.zxing" % "core" % "3.1.0",
    "javax.mail" % "mail" % "1.4.7",
    "info.blockchain" % "api" % "1.0.2",
    "com.googlecode.libphonenumber" % "libphonenumber" % "7.0",
    "org.bitcoinj" % "bitcoinj-core" % "0.12.2",
    "commons-codec" % "commons-codec" % "1.10",
    "com.google.zxing" % "javase" % "3.1.0",
    "net.databinder.dispatch" %% "dispatch-core" % "0.11.2",
    "org.specs2" % "specs2_2.10" % "2.3.7"  % "test",
    "org.slf4j" % "slf4j-simple" % "1.6.4",
    "redis.clients" % "jedis" % "2.6.1"
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
