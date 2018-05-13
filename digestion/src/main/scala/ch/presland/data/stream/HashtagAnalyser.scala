package ch.presland.data.stream

import scala.collection.convert.wrapAll._
import java.time.{LocalDateTime, ZoneId}
import java.util.Date

import ch.presland.data.domain.Tweet
import com.datastax.driver.core.utils.UUIDs
import com.datastax.spark.connector._
import com.datastax.spark.connector.streaming._
import com.datastax.driver.core.utils._
import com.datastax.spark.connector.SomeColumns
import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.apache.spark.streaming.dstream.DStream

import scala.collection.immutable.Map

object HashtagAnalyser {

  type Hashtag = String
  type HashtagRank = Int
  type HashtagCounter = Map[String, Long]

  val seqOp: (HashtagCounter, (String,Int)) => HashtagCounter = {
    case (counter, (hashtag,count)) if counter.isDefinedAt(hashtag) => counter.updated(hashtag, counter(hashtag) + count)
    case (counter, (hashtag,count)) => counter + (hashtag -> count)
  }

  val combOp: (HashtagCounter,HashtagCounter) => HashtagCounter = {
    case (counter1,counter2) => counter2.map{
      case (k,v) => k -> (v + counter1.getOrElse(k,0L))}
  }
}

class HashtagAnalyser(ssc: StreamingContext){

  val columnHeaders = SomeColumns("id","time", "tag0", "tag1", "tag2", "tag3", "tag4", "tag5", "tag6", "tag7", "tag8", "tag9")

  def analyse(tweets: DStream[Tweet]): Unit = {

    tweets
      .map(t => t.content)
      .flatMap(text => text.toLowerCase().split(" ").filter(_.startsWith("#")))
      .map((_, 1))
      .reduceByKeyAndWindow(_ + _, Seconds(600))
      .map { case (tag, count) => (tag, count) }
      .transform(_.sortBy(p => p._2, ascending = false))
      .foreachRDD(rdd => saveHashtagToCassandra(
        ssc, rdd.take(5).aggregate(Map.empty[String, Long])(HashtagAnalyser.seqOp, HashtagAnalyser.combOp))
      )
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
      (keys.get(4), hashtags.get(keys.get(4)))
    ))

    ssc.sparkContext.parallelize(row)
      .saveToCassandra("twitter","hashtags", SomeColumns("id","time", "tag0", "tag1", "tag2", "tag3", "tag4"))
  }
}
