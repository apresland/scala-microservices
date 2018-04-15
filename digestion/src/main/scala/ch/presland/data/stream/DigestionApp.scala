package ch.presland.data.stream

import java.util.Properties

import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.spark.SparkConf
import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.apache.spark.streaming.kafka010.KafkaUtils
import org.apache.spark.streaming.kafka010.LocationStrategies.PreferConsistent
import org.apache.spark.streaming.kafka010.ConsumerStrategies.Subscribe
import com.datastax.spark.connector._
import com.datastax.spark.connector.streaming._
import com.datastax.spark.connector.SomeColumns
import com.datastax.driver.core.utils._

import scala.collection.immutable.Map
import ch.presland.data.domain.Tweet

import scala.collection.convert.wrapAll._
import java.util.Date
import java.time.{LocalDateTime, ZoneId}

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, OverflowStrategy}
import akka.stream.scaladsl._


object DigestionApp extends App {

  implicit val system = ActorSystem("reactive-tweets")
  implicit val materializer = ActorMaterializer()

  val sparkConf = new SparkConf()
    .setAppName(getClass.getSimpleName)
    .setMaster("local[*]")
    .set("spark.cassandra.connection.host", "localhost")
    .set("spark.cassandra.connection.port", "9042")
    .set("spark.cassandra.connection.keep_alive_ms", "30000")

  val batchDuration = 60
  val ssc = new StreamingContext(sparkConf, Seconds(batchDuration))

  val kafkaParams = Map[String, Object](
    "bootstrap.servers" -> "localhost:9092",
    "key.deserializer" -> classOf[StringDeserializer].getName,
    "value.deserializer" -> classOf[TweetDeserializer].getName,
    "group.id" -> "digestion",
    "auto.offset.reset" -> "latest",
    "enable.auto.commit" -> (false: java.lang.Boolean)
  )

  val stream = KafkaUtils.createDirectStream[String, Tweet](
    ssc,
    PreferConsistent,
    Subscribe[String, Tweet](Array("ingested-tweets"), kafkaParams)
  )

  val tweets = stream
    .map(consumerRecord => consumerRecord.value())
    .persist()

  tweets
    .saveToCassandra("twitter", "tweets")

  // Tweet sentiment analysis
  tweets
    .map(tweet => TweetAnalyser.sentiment(tweet))
    .map(sentiment => sentiment.score)
    .window(Seconds(60))
    .foreachRDD((rdd, time) => saveSentimentToCassandra(ssc, time,
      rdd.aggregate(Map(0 -> 0L, 1 -> 0L, 2 -> 0L, 3 -> 0L))(SentimentAggregator.seqOp, SentimentAggregator.combOp))
    )

  // Tweet hashtag trending
  tweets
    .map(t => t.content)
    .flatMap(text => text.toLowerCase().split(" ").filter(_.startsWith("#")))
    .map((_, 1))
    .reduceByKeyAndWindow(_ + _, Seconds(600))
    .map { case (tag, count) => (tag, count) }
    .transform(_.sortBy(p => p._2, ascending = false))
    .foreachRDD(rdd => saveHashtagToCassandra(
      ssc, rdd.take(10).aggregate(Map.empty[String, Long])(HashtagAggregator.seqOp, HashtagAggregator.combOp))
    )

  ssc.start()
  ssc.awaitTermination()

  def saveSentimentToCassandra(ssc: StreamingContext, time: org.apache.spark.streaming.Time, sentiment: Map[SentimentAggregator.Category,Long]): Unit = {

    val date = LocalDateTime.now()
    val timestamp = Date.from(date.atZone(ZoneId.systemDefault()).toInstant)
    val row = Seq((timestamp, sentiment.get(0), sentiment.get(1), sentiment.get(2), sentiment.get(3)))

    ssc.sparkContext.parallelize(row)
      .saveToCassandra("twitter","sentiments", SomeColumns("time", "hostile", "negative", "neutral", "positive"))
  }

  def saveHashtagToCassandra(ssc: StreamingContext, hashtags: Map[String,Long]): Unit = {

    val date = LocalDateTime.now()
    val timestamp = Date.from(date.atZone(ZoneId.systemDefault()).toInstant)

    val keys = hashtags.keySet.toList

    val row = Seq((UUIDs.timeBased(), timestamp,
      (keys.get(0), hashtags.get(keys.get(0))),
      (keys.get(1), hashtags.get(keys.get(1))),
      (keys.get(2), hashtags.get(keys.get(2))),
      (keys.get(3), hashtags.get(keys.get(3))),
      (keys.get(4), hashtags.get(keys.get(4))),
      (keys.get(5), hashtags.get(keys.get(5))),
      (keys.get(6), hashtags.get(keys.get(6))),
      (keys.get(7), hashtags.get(keys.get(7))),
      (keys.get(8), hashtags.get(keys.get(8))),
      (keys.get(9), hashtags.get(keys.get(9)))
    ))

    ssc.sparkContext.parallelize(row)
      .saveToCassandra("twitter","hashtags",
        SomeColumns("id","time", "tag0", "tag1", "tag2", "tag3", "tag4", "tag5", "tag6", "tag7", "tag8", "tag9"))
  }
}
