package ch.presland.data.stream

import akka.actor.{ActorSystem, Props}
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.spark.SparkConf
import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.apache.spark.streaming.kafka010.KafkaUtils
import org.apache.spark.streaming.kafka010.LocationStrategies.PreferConsistent
import org.apache.spark.streaming.kafka010.ConsumerStrategies.Subscribe

import scala.collection.immutable.Map

import ch.presland.data.domain.Tweet

object TweetDigestor extends App {

  val system = ActorSystem()
  val publish = system.actorOf(Props(new TweetPublisher()))

  val sparkConf = new SparkConf()
      .setAppName(getClass.getSimpleName)
      .setMaster("local[*]")
      .set("spark.cassandra.connection.host", "localhost")
      .set("spark.cassandra.connection.port", "9042" )
      .set("spark.cassandra.connection.keep_alive_ms", "30000")

  val batchDuration = Seconds(10)
  val ssc = new StreamingContext(sparkConf, batchDuration)

  val kafkaParams = Map[String, Object] (
    "bootstrap.servers" -> "localhost:9092",
    "key.deserializer" -> classOf[StringDeserializer].getName,
    "value.deserializer" -> classOf[TweetDeserializer].getName,
    "group.id" -> "digestion",
    "auto.offset.reset" -> "latest",
    "enable.auto.commit" -> (false: java.lang.Boolean)
  )

  val stream = KafkaUtils.createDirectStream[String,Tweet](
    ssc,
    PreferConsistent,
    Subscribe[String,Tweet](Array("mytweets"), kafkaParams)
  )

  val tweets = stream
    .map {consumerRecord => consumerRecord.value()}
    .foreachRDD(rdd => { rdd
      .map(println)
      .collect()
      //.saveToCassandra("twitter", "tweets")
  })

  ssc.start()
  ssc.awaitTermination()
}
