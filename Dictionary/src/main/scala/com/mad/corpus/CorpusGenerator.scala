package com.mad.corpus

import java.io._
import org.apache.hadoop.hbase.spark.HBaseContext
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.hadoop.hbase.{TableName, HBaseConfiguration}
import org.apache.hadoop.hbase.util.Bytes
import org.apache.hadoop.hbase.client.{Scan, Put, Get, Result}
import org.apache.hadoop.hbase.spark.HBaseRDDFunctions._
import org.apache.hadoop.hbase.io.ImmutableBytesWritable
import org.apache.spark.rdd._
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.client.Connection
import scala.collection.mutable.{ArrayBuffer}

class CorpusGenerator(implicit sc: SparkContext) {
  private val hbaseConf = HBaseConfiguration.create()
  hbaseConf.addResource(new Path("/usr/local/hadoop-2.5.0-cdh5.3.9/etc/hadoop/core-site.xml"))
  hbaseConf.addResource(new Path("/usr/local/hadoop-2.5.0-cdh5.3.9/etc/hadoop/hbase-site.xml"))
  
  private val hbaseContext = new HBaseContext(sc, hbaseConf)
  
  def run(tableName: String) {
    try{
      val scan = new Scan()
                 .addFamily(Bytes.toBytes("keywords"))
                 .setCaching(100)
      val hbaseRDD = hbaseContext.hbaseRDD(TableName.valueOf(tableName), scan)
      
      hbaseRDD.hbaseForeachPartition(hbaseContext, (it, connection) => {
        val dict = connection.getTable(TableName.valueOf("keyword_list"))
        val urlInfo = connection.getBufferedMutator(TableName.valueOf(tableName))
        
        it.foreach(r => {
          val doc = Bytes.toString(r._2.value()).split(",")
          
          val ids = new StringBuilder()
          val cnts = new StringBuilder()
          doc.foreach { x => {
            val pair = x.split(":")
            val word = pair(0)
            val cnt = pair(1)
            val get = new Get(word.getBytes)
            get.addColumn(Bytes.toBytes("info"), Bytes.toBytes("id"))
            val result = dict.get(get)
            ids.append(Bytes.toString(result.value()) + ":")
            cnts.append(cnt + ":")
          } }
          val newDoc = ids.deleteCharAt(ids.length-1).toString + "," + cnts.deleteCharAt(cnts.length-1).toString
          val put = new Put(r._1.get)
                        .addColumn(Bytes.toBytes("keywords"), Bytes.toBytes("doc"), newDoc.getBytes)
          urlInfo.mutate(put)
        })
        
        dict.close()
        urlInfo.close()
      })
    }
    catch {
      case ex:Exception => println(ex.toString) 
    }
    finally {
      
    }
    
  }
}

object CorpusGenerator {
  def main(args: Array[String]){
    val sparkConf = new SparkConf().setAppName("Corpus")
    implicit val sc = new SparkContext(sparkConf)
    
    val corpus = new CorpusGenerator()
    
    corpus.run(args(0))
    
    sc.stop
  }
}