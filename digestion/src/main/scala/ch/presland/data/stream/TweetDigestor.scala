package ch.presland.data.stream

import java.util.Properties

import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.spark.rdd.RDD
import org.apache.spark.SparkConf
import org.apache.spark.sql.streaming
import org.apache.spark.sql.functions.window
import org.apache.spark.streaming.{Duration, Seconds, StreamingContext}
import org.apache.spark.streaming.kafka010.KafkaUtils
import org.apache.spark.streaming.kafka010.LocationStrategies.PreferConsistent
import org.apache.spark.streaming.kafka010.ConsumerStrategies.Subscribe
import edu.stanford.nlp.ling.CoreAnnotations
import edu.stanford.nlp.neural.rnn.RNNCoreAnnotations
import edu.stanford.nlp.pipeline.{Annotation, StanfordCoreNLP}
import edu.stanford.nlp.sentiment.SentimentCoreAnnotations
import com.datastax.spark.connector._
import com.datastax.spark.connector.streaming._
import com.datastax.spark.connector.cql._
import com.datastax.spark.connector.SomeColumns

import scala.collection.immutable.Map
import ch.presland.data.domain.{Tweet, TweetMetric}

import scala.collection.convert.wrapAll._
import java.util.Date
import java.time.{LocalDateTime, ZoneId, format}
import java.time.format.DateTimeFormatter
import java.sql.{Time, Timestamp}

import scala.reflect.internal.util.TableDef.Column

object TweetDigestor extends App {

  val props = new Properties()
  props.setProperty("annotators", "tokenize, ssplit, pos, lemma, parse, sentiment")

  val pipeline = new StanfordCoreNLP(props)

  val sparkConf = new SparkConf()
      .setAppName(getClass.getSimpleName)
      .setMaster("local[*]")
      .set("spark.cassandra.connection.host", "localhost")
      .set("spark.cassandra.connection.port", "9042" )
      .set("spark.cassandra.connection.keep_alive_ms", "30000")

  val batchDuration = 60
  val ssc = new StreamingContext(sparkConf, Seconds(batchDuration))

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
    Subscribe[String,Tweet](Array("ingested-tweets"), kafkaParams)
  )

  val tweet = stream
    .map(consumerRecord => consumerRecord.value())
    .persist()

  tweet
    .saveToCassandra("twitter", "tweets")

  tweet
    .map(tweet => metric(tweet))
    .saveToCassandra("twitter","metrics")

  val fmt:DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
  var startTime = LocalDateTime.now().minusHours(1)
  val timeStartLimit:LocalDateTime = startTime
  val timeStopLimit = timeStartLimit.plusHours(1)

  val metricsRDD:RDD[TweetMetric] = ssc.cassandraTable[TweetMetric]("twitter","metrics")
    .where("time > ? and time < ?", timeStartLimit.format(fmt), timeStopLimit.format(fmt))

  type SentimentCategory = Int
  type Counter = Map[SentimentCategory,Long]


  val seqOp: (Counter, SentimentCategory) => Counter = {
    case (counter, category) if counter.isDefinedAt(category) => counter.updated(category, counter(category) + 1)
    case (counter, category) => counter + (category -> 1)
  }

  val combOp: (Counter,Counter) => Counter = {
    case (counter1,counter2) => counter1  ++ counter2.map{
      case (k,v) => k -> (v + counter1.getOrElse(k,0L))}
  }

  val dfd = tweet
    .map(tweet=>metric(tweet))
    .map(metric => metric.sentiment)
    .window(Seconds(300))
    .foreachRDD((rdd,time) => saveToDB(time,
      rdd.aggregate(Map(0->0L,1->0L,2->0L,3->0L))(seqOp, combOp))
    )

  ssc.start()
  ssc.awaitTermination()

  def saveToDB(time: org.apache.spark.streaming.Time, senti: Map[SentimentCategory,Long]): Unit = {

    val date = LocalDateTime.now()
    val timestamp = Date.from(date.atZone(ZoneId.systemDefault()).toInstant)
    val row = Seq((timestamp, senti.get(0), senti.get(1), senti.get(2), senti.get(3)))

    println(row)
    ssc.sparkContext.parallelize(row)
      .saveToCassandra("twitter","sentiments", SomeColumns("time", "hostile", "negative", "neutral", "positive"))
  }

  def metric(tweet: Tweet): TweetMetric = {

    val sentiments = pipeline
      .process(tweet.content)
      .get(classOf[CoreAnnotations.SentencesAnnotation])
      .map(sentence => (sentence, sentence.get(classOf[SentimentCoreAnnotations.SentimentAnnotatedTree])))
      .map{case(sentence, tree) => (sentence.toString, RNNCoreAnnotations.getPredictedClass(tree))}
      .toList

    val sentiment = sentiments
      .maxBy{case (sentence,_) => sentence.toString.length}
      ._2

    TweetMetric(tweet.id, tweet.time, sentiment)
  }
}
