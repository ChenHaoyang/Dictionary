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
import scala.math.{log}

class TFIDF(implicit sc: SparkContext) {
  private val hbaseConf = HBaseConfiguration.create()
  private val hbaseContext = new HBaseContext(sc, hbaseConf)
  
  def run(tableName: String){
    try{
      val scan = new Scan()
                      .addColumn(Bytes.toBytes("corpus"), Bytes.toBytes("doc"))
                      .setCaching(100)
      val corpus = hbaseContext.hbaseRDD(TableName.valueOf(tableName), scan)
      
      val df = corpus.map(pair => {
        val ids = Bytes.toString(pair._2.value()).split(",")(0).split(":").map(id => (id.toLong, 1))
        ids
      }).flatMap{x => x}
        .reduceByKey(_+_).collect.toMap
      
      val dfBd = sc.broadcast(df)
      val docCnt = corpus.count.toDouble
      println(docCnt)
      
      corpus.hbaseForeachPartition(hbaseContext, (it, connection) => {
        val localdf = dfBd.value
        val urlInfo = connection.getBufferedMutator(TableName.valueOf(tableName))
        it.foreach(pair => {
          val doc = Bytes.toString(pair._2.value()).split(",")
          val ids = doc(0).split(":").map(_.toLong)
          val cnts = doc(1).split(":").map(_.toLong)
          val cnts_sum = cnts.sum.toDouble
          val tf = cnts.map(cnt => cnt.toDouble)
          val idf = ids.map(id => log(docCnt) - log(localdf(id).toDouble))
          val tfidf = tf.zip(idf).map{case(tf,idf) => (tf * idf).toString}
          val newdoc = ids.mkString(":") + "," + tfidf.mkString(":")
          
          val put = new Put(pair._1.get)
                        .addColumn(Bytes.toBytes("corpus"), Bytes.toBytes("newdoc"), newdoc.getBytes)
          urlInfo.mutate(put)
        })
        urlInfo.close()
      })
                      
    }
    catch{
      case ex:Exception => println(ex.toString) 
    }
    finally{
      
    }
  }
}

object TFIDF {
  def main(args: Array[String]){
    val sparkConf = new SparkConf().setAppName("TFIDF")
    implicit val sc = new SparkContext(sparkConf)
    
    val tfidf = new TFIDF()
    
    tfidf.run(args(0))
    
    sc.stop
  }
}