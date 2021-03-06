package org.jennings.estest

import org.apache.spark.{SparkConf, SparkContext}
import org.elasticsearch.spark.rdd.EsSpark

/**
  * Created by david on 11/4/17.
  */
object SendS3FileElasticsearch {

  // spark-submit --class org.jennings.estest.SendFileElasticsearch target/estest.jar planes00001 a1 9200 local[16] planes/planes

  // java -cp target/estest.jar org.jennings.estest.SendFileElasticsearch

  def main(args: Array[String]): Unit = {

    TestLogging.setStreamingLogLevels()

    val appName = getClass.getName

    val numargs = args.length

    if (numargs != 7 && numargs != 9) {
      System.err.println("Usage: SendFileElasticsearchFile Access-Key Secret-Key Filename ESServer ESPort SpkMaster (Username) (Password)")
      System.err.println("        Access-Key: AWS Access Key")
      System.err.println("        Access-Key: AWS Secret Key")
      System.err.println("        Filename: JsonFile to Process")
      System.err.println("        ESServer: Elasticsearch Server Name or IP")
      System.err.println("        ESPort: Elasticsearch Port (e.g. 9200)")
      System.err.println("        SpkMaster: Spark Master (e.g. local[8] or - to use default)")
      System.err.println("        IndexType: Index/Type (e.g. planes/events")
      System.err.println("        Username: Elasticsearch Username (optional)")
      System.err.println("        Password: Elasticsearch Password (optional)")
      System.exit(1)

    }

    val accessKey = args(0)
    val secretKey = args(1)
    val filename = args(2)
    val esServer = args(3)
    val esPort = args(4)
    val spkMaster = args(5)
    val indexAndType = args(6)

    //val Array(filename,esServer,esPort,spkMaster,indexAndType) = args

    println("Sending " + filename + " to " + esServer + ":" + esPort + " using " + spkMaster)

    val sparkConf = new SparkConf().setAppName(appName)
    if (!spkMaster.equalsIgnoreCase("-")) {
      sparkConf.setMaster(spkMaster)
    }
    sparkConf.set("es.index.auto.create", "true")
    sparkConf.set("es.nodes", esServer)
    sparkConf.set("es.port", esPort)
    // Without the following it would not create the index on single-node mode
    sparkConf.set("es.nodes.discovery", "false")
    sparkConf.set("es.nodes.data.only", "false")
    // Without setting es.nodes.wan.only the index was created but loading data failed (5.5.1)
    sparkConf.set("es.nodes.wan.only", "true")

//    sparkConf.set("spark.hadoop.fs.s3a.impl", "org.apache.hadoop.fs.s3a.S3AFileSystem")
//    sparkConf.set("spark.hadoop.fs.s3a.access.key", accessKey)
//    sparkConf.set("spark.hadoop.fs.s3a.secret.key", secretKey)


    if (numargs == 9) {
      val username = args(7)
      val password = args(8)
      sparkConf.set("es.net.http.auth.user", username)
      sparkConf.set("es.net.http.auth.pass", password)
    }


    val sc = new SparkContext(sparkConf)

    sc.hadoopConfiguration.set("fs.s3a.impl", "org.apache.hadoop.fs.s3a.S3AFileSystem")
    sc.hadoopConfiguration.set("fs.s3a.access.key", accessKey)
    sc.hadoopConfiguration.set("fs.s3a.secret.key", secretKey)
    sc.hadoopConfiguration.setInt("fs.s3a.connection.maximum",5000)

    val textFile = sc.textFile(filename)

    EsSpark.saveJsonToEs(textFile, indexAndType)

  }
}
