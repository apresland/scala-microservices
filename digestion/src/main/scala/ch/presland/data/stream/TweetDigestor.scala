package ch.presland.data.stream

import java.util.Properties

import akka.actor.{ActorSystem, Props}
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.spark.SparkConf
import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.apache.spark.streaming.kafka010.KafkaUtils
import org.apache.spark.streaming.kafka010.LocationStrategies.PreferConsistent
import org.apache.spark.streaming.kafka010.ConsumerStrategies.Subscribe
import com.datastax.spark.connector.streaming._
import edu.stanford.nlp.ling.CoreAnnotations
import edu.stanford.nlp.neural.rnn.RNNCoreAnnotations
import edu.stanford.nlp.pipeline.Annotation
import edu.stanford.nlp.pipeline.StanfordCoreNLP
import edu.stanford.nlp.sentiment.SentimentCoreAnnotations
import edu.stanford.nlp.trees.Tree
import edu.stanford.nlp.util.CoreMap

import edu.stanford.nlp.ling.CoreAnnotations
import edu.stanford.nlp.neural.rnn.RNNCoreAnnotations
import edu.stanford.nlp.pipeline.{Annotation, StanfordCoreNLP}
import edu.stanford.nlp.sentiment.SentimentCoreAnnotations

import scala.collection.immutable.Map
import ch.presland.data.domain.{Sentiment, Tweet}

import scala.collection.convert.wrapAll._

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
    Subscribe[String,Tweet](Array("ingested-tweets"), kafkaParams)
  )

  val tweet = stream
    .map(consumerRecord => consumerRecord.value()).cache()

  //tweet
  //  .saveToCassandra("twitter", "tweets")

  tweet
    .map(tweet => sentiment(tweet))
    .saveToCassandra("twitter", "sentiments")

  ssc.start()
  ssc.awaitTermination()

  def sentiment(tweet: Tweet): Sentiment = {

    val sentiments = pipeline
      .process(tweet.content)
      .get(classOf[CoreAnnotations.SentencesAnnotation])
      .map(sentence => (sentence, sentence.get(classOf[SentimentCoreAnnotations.SentimentAnnotatedTree])))
      .map{case(sentence, tree) => (sentence.toString, RNNCoreAnnotations.getPredictedClass(tree))}
      .toList

    val score = sentiments
      .maxBy{case (sentence,_) => sentence.toString.length}
      ._2

    Sentiment(tweet.id, tweet.time, score)
  }
}
