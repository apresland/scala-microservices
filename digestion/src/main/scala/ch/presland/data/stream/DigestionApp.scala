package ch.presland.data.stream

import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.spark.SparkConf
import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.apache.spark.streaming.kafka010.KafkaUtils
import org.apache.spark.streaming.kafka010.LocationStrategies.PreferConsistent
import org.apache.spark.streaming.kafka010.ConsumerStrategies.Subscribe
import com.datastax.spark.connector.streaming._


import scala.collection.immutable.Map
import ch.presland.data.domain.Tweet

object DigestionApp extends App {

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

  new SentimentAnalyser(ssc, tweets)
  .analyse()

  new HashtagAnalyser(ssc, tweets)
    .analyse()

  new StatisticsAnalyser(ssc, tweets)
    .analyse()

  tweets
    .map(tweet => println(tweet))
    .count()

  tweets
    .saveToCassandra("twitter", "tweets")

  ssc.start()
  ssc.awaitTermination()
}
