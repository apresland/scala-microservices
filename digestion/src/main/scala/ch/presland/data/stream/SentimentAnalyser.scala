package ch.presland.data.stream

import java.time.{LocalDateTime, ZoneId}
import java.util.Date

import ch.presland.data.domain.Tweet
import ch.presland.data.stream.DigestionApp.stream
import ch.presland.data.stream.SentimentAnalyser.{combOp, seqOp}
import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.apache.spark.streaming.dstream.{DStream, InputDStream}
import com.datastax.spark.connector._
import com.datastax.spark.connector.SomeColumns
import com.datastax.spark.connector.writer.{TTLOption, WriteConf}
import org.apache.kafka.clients.consumer.ConsumerRecord

import scala.collection.immutable.Map

object SentimentAnalyser {

  type Category = Int
  type SentimentCounter = Map[Category,Long]

  val seqOp: (SentimentCounter, Category) => SentimentCounter = {
    case (counter, category) if counter.isDefinedAt(category) => counter.updated(category, counter(category) + 1)
    case (counter, category) => counter + (category -> 1)
  }

  val combOp: (SentimentCounter,SentimentCounter) => SentimentCounter = {
    case (counter1,counter2) => counter2.map{
      case (k,v) => k -> (v + counter1.getOrElse(k,0L))}
  }
}

class SentimentAnalyser(ssc: StreamingContext, val tweets: DStream[Tweet]){

  val columnHeaders = SomeColumns("date", "hostile", "negative", "neutral", "positive")
  val initialValue = Map(0 -> 0L, 1 -> 0L, 2 -> 0L, 3 -> 0L)

  def analyse(): Unit = {

    tweets
      .map(tweet => NLP.sentiment(tweet))
      .map(sentiment => sentiment.score)
      .window(Seconds(60))
      .foreachRDD((rdd, time) => saveSentimentToCassandra(ssc, time,
        rdd.aggregate(initialValue)(seqOp, combOp))
      )
  }

  def saveSentimentToCassandra(ssc: StreamingContext, time: org.apache.spark.streaming.Time, sentiment: Map[SentimentAnalyser.Category,Long]): Unit = {

    val date = LocalDateTime.now()
    val timestamp = Date.from(date.atZone(ZoneId.systemDefault()).toInstant)
    val row = Seq((timestamp, sentiment.get(0), sentiment.get(1), sentiment.get(2), sentiment.get(3)))

    ssc.sparkContext.parallelize(row)
      .saveToCassandra("twitter","sentiments", columnHeaders, writeConf = WriteConf(ttl = TTLOption.constant(86400)))
  }
}
