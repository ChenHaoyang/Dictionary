package com.mad.dictionary

import java.io._
import org.apache.hadoop.hbase.spark.HBaseContext
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.hadoop.hbase.{TableName, HBaseConfiguration}
import org.apache.hadoop.hbase.util.Bytes
import org.apache.hadoop.hbase.client.{Scan, Put, Result}
import org.apache.spark.rdd._
import org.apache.hadoop.fs.Path;
//import org.apache.hadoop.hbase.io.ImmutableBytesWritable

class IDGenerator(implicit sc: SparkContext) {
  private val hbaseConf = HBaseConfiguration.create()
  hbaseConf.addResource(new Path("/usr/local/hadoop-2.5.0-cdh5.3.9/etc/hadoop/core-site.xml"))
  hbaseConf.addResource(new Path("/usr/local/hadoop-2.5.0-cdh5.3.9/etc/hadoop/hbase-site.xml"))

  private val hbaseContext = new HBaseContext(sc, hbaseConf)
  

  def run(urlThreshold: Int, tableName: String, familyName: String = null, qualifier: String = null) {
    try{
      val scan = new Scan()
                 .addFamily(familyName.getBytes)
                 .setCaching(100)
      val wordListOfUrls = hbaseContext.hbaseRDD(TableName.valueOf(tableName), scan)
                            .map(v => {
                              Bytes.toString(v._2.value()).split(",")
                              .map(wordFreq => {
                                val valPair = wordFreq.split(":")
                                (valPair(0), valPair(1).toLong)
                              })
                            })
      val urlFreqOfWords = wordListOfUrls.flatMap{ words => words }
                                        .reduceByKey(_+_)
                                        .filter(_._2 >= urlThreshold)
                                        .sortBy(_._2, false)
      val keywordList = urlFreqOfWords.zipWithIndex()//.map(_.swap)
                                      
      hbaseContext.bulkPut[((String, Long), Long)](keywordList, TableName.valueOf(Bytes.toBytes("keyword_list")),
                    (record) => {
                      val put = new Put(Bytes.toBytes(record._1._1))
                      put.addColumn(Bytes.toBytes("info"), Bytes.toBytes("id"), Bytes.toBytes(record._2))
                      put.addColumn(Bytes.toBytes("info"), Bytes.toBytes("freq"), Bytes.toBytes(record._1._2))
                    })
    }
    catch {
      case ex: Exception => println(ex.toString())
    }
    finally {
  
    }
  } 
}

object IDGenerator {
  
  def main(args: Array[String]){
    val sparkConf = new SparkConf().setAppName("Dictionary")
    .setMaster("local[2]")
    implicit val sc = new SparkContext(sparkConf)
      
    val idg = new IDGenerator()
      
    idg.run(args(0).toInt,args(1), "keywords")
    
    sc.stop()
  }
}