name := "Dictionary"
 
version := "1.0"
 
scalaVersion := "2.10.5"

resolvers += "Apache-HBase-Spark-snapshots" at "https://repository.apache.org/content/repositories/snapshots"

libraryDependencies ++= Seq(
  //"com.github.scopt" % "scopt_2.10" % "3.3.0",
  //"org.scalanlp" %% "breeze" % "0.11.2",
  //"org.scalanlp" %% "breeze-natives" % "0.11.2",
  //"org.scalanlp" %% "breeze-viz" % "0.11.2",
  "org.apache.spark" %% "spark-core" % "1.6.0" % "provided",
  "org.apache.spark" %% "spark-sql" % "1.6.0" % "provided",
  //"org.apache.spark" %% "spark-hive" % "1.6.0" % "provided",
  "org.apache.spark" %% "spark-streaming" % "1.6.0" % "provided",
  //"org.apache.spark" %% "spark-streaming-kafka" % "1.6.0" % "provided",
  //"org.apache.spark" %% "spark-streaming-flume" % "1.6.0" % "provided",
  //"org.apache.spark" %% "spark-mllib" % "1.6.0" % "provided"
  //"org.scala-lang" % "scala-swing" % "2.11.0-M7"
  "org.apache.hbase" % "hbase-spark" % "2.0.0-SNAPSHOT",
  "org.apache.hbase" % "hbase-common" % "2.0.0-SNAPSHOT",
  "org.apache.hbase" % "hbase-server" % "2.0.0-SNAPSHOT",
  "org.apache.hbase" % "hbase-hadoop-compat" % "1.2.2",
  "org.apache.hbase" % "hbase-hadoop2-compat" % "1.2.2"
)

assemblyMergeStrategy in assembly := {
  case PathList("javax", "servlet", xs @ _*)         => MergeStrategy.first
  case PathList(ps @ _*) if ps.last endsWith ".thrift" => MergeStrategy.first
  case PathList(ps @ _*) if ps.last endsWith ".html" => MergeStrategy.first
  case PathList(ps @ _*) if ps.last endsWith ".properties" => MergeStrategy.first
  case PathList(ps @ _*) if ps.last endsWith ".xml" => MergeStrategy.first
  case PathList(ps @ _*) if ps.last endsWith ".types" => MergeStrategy.first
  case PathList(ps @ _*) if ps.last endsWith ".class" => MergeStrategy.first
  case "application.conf"                            => MergeStrategy.concat
  case "unwanted.txt"                                => MergeStrategy.discard
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}

//assemblyOption in assembly := (assemblyOption in assembly).value.copy(includeScala = false)
