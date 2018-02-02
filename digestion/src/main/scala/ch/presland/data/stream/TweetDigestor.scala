package ch.presland.data.stream

import java.util.concurrent.atomic.AtomicLong

import kafka.serializer.StringDecoder

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
//import akka.actor.ActorSystem
import akka.Done
//import akka.kafka.{ConsumerSettings, Subscriptions}
import akka.kafka.scaladsl.Consumer
//import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import org.apache.kafka.clients.consumer.{ConsumerConfig, ConsumerRecord}
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.spark.SparkConf
import org.apache.spark.streaming.kafka010._
import org.apache.spark.streaming.{StreamingContext, Seconds}
import org.apache.spark.streaming.kafka010.KafkaUtils
import org.apache.spark.streaming.kafka010.LocationStrategies.PreferConsistent
import org.apache.spark.streaming.kafka010.ConsumerStrategies.Subscribe

import scala.util.parsing.json.JSON
import scala.collection.immutable.Map
import org.apache.spark.sql.cassandra._
import com.datastax.spark.connector._

object TweetDigestor extends App {

  //implicit val system: ActorSystem = ActorSystem("digest-system")
  //implicit val materializer: ActorMaterializer = ActorMaterializer()

  /*val consumerSettings = ConsumerSettings(system, new StringDeserializer, new StringDeserializer)
    .withBootstrapServers("localhost:9092")
    .withGroupId("digestion")
    .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")*/

  val sparkConf = new SparkConf()
      .setAppName(getClass.getSimpleName)
      .setMaster("local[*]")
      .set("spark.cassandra.connection.host", "localhost")
      .set("spark.cassandra.connection.port", "9042" )
      .set("spark.cassandra.connection.keep_alive_ms", "30000")

  val ssc = new StreamingContext(sparkConf, Seconds(10))

  val kafkaParams = Map[String, Object] (
    "bootstrap.servers" -> "localhost:9092",
    "key.deserializer" -> classOf[StringDeserializer].getName,
    "value.deserializer" -> classOf[StringDeserializer].getName,
    "group.id" -> "digestion",
    "auto.offset.reset" -> "latest",
    "enable.auto.commit" -> (false: java.lang.Boolean)
  )

  val stream = KafkaUtils.createDirectStream[String,String](
    ssc,
    PreferConsistent,
    Subscribe[String,String](Array("tweets"), kafkaParams)
  )

  val tweets = stream
    .map {consumerRecord => consumerRecord.value()}

  val samples = 10

  tweets.foreachRDD(rdd => {

      rdd
        .map(s => {
          val tweet = JSON.parseFull(s)
          val id: String = tweet.get.asInstanceOf[Map[String,Any]]("id_str").asInstanceOf[String]
          val text: String = tweet.get.asInstanceOf[Map[String,Any]]("text").asInstanceOf[String]
          (id,text)})
        .saveToCassandra("twitter", "tweets")
  })

  ssc.start()
  ssc.awaitTermination()
}

class DB {

  private val offset = new AtomicLong

  def save(record: ConsumerRecord[String, String]): Future[Done] = {
    println(s"DB.save: ${record.value}")
    offset.set(record.offset)
    Future.successful(Done)
  }

  def loadOffset(): Future[Long] =
    Future.successful(offset.get)

  def update(data: String): Future[Done] = {
    println(s"DB.update: $data")
    Future.successful(Done)
  }
}
