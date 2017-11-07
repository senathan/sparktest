package org.jennings.estest

import java.io.File

import org.apache.spark.{SparkConf, SparkContext}
import org.elasticsearch.spark.rdd.EsSpark

/**
  * Created by david on 11/4/17.
  */
object SendFolderElasticsearch {

  // spark-submit --class org.jennings.estest.SendFileElasticsearch target/estest.jar planes00001 a1 9200 local[16] planes/planes

  // java -cp target/estest.jar org.jennings.estest.SendFileElasticsearchFile

  def main(args: Array[String]): Unit = {

    TestLogging.setStreamingLogLevels()

    val appName = getClass.getName

    val numargs = args.length

    if (numargs != 5) {
      System.err.println("Usage: SendFileElasticsearchFile Foldername ESServer ESPort SpkMaster")
      System.err.println("        Folder: Folder containing json files to send")
      System.err.println("        ESServer: Elasticsearch Server Name or IP")
      System.err.println("        ESPort: Elasticsearch Port (e.g. 9200)")
      System.err.println("        SpkMaster: Spark Master (e.g. local[8])")
      System.err.println("        IndexType: Index/Type (e.g. planes/events")
      System.exit(1)

    }

    val Array(foldername,esServer,esPort,spkMaster,indexAndType) = args

    println("Sending files from folder " + foldername + " to " + esServer + ":" + esPort + " using " + spkMaster)

    val sparkConf = new SparkConf().setAppName(appName).setMaster(spkMaster)
    sparkConf.set("es.index.auto.create", "true")
    sparkConf.set("spark.es.nodes",esServer)
    sparkConf.set("spark.es.port", esPort)
    // Without the following it would not create the index on single-node mode
    sparkConf.set("es.nodes.discovery", "false")
    sparkConf.set("es.nodes.data.only", "false")
    // Without setting es.nodes.wan.only the index was created but loading data failed (5.5.1)
    sparkConf.set("es.nodes.wan.only","true")

    val sc = new SparkContext(sparkConf)


    val folder = new File(foldername)

    val files = folder.listFiles().iterator

    while (files.hasNext) {
      // For each file in the folder
      val file = files.next
      val filename = foldername + File.separator + file.getName
      println("Sending: " + filename)
      val textFile =  sc.textFile(filename)

      EsSpark.saveJsonToEs(textFile, indexAndType)

    }
  }
}
