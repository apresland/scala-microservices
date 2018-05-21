package ch.presland.data.stream

import java.time.{LocalDateTime, ZoneId}
import java.util.Date

import ch.presland.data.domain.Tweet
import com.datastax.spark.connector._
import com.datastax.spark.connector.writer._
import com.datastax.spark.connector.streaming._
import com.datastax.driver.core.utils._
import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.apache.spark.streaming.dstream.DStream

class HashtagAnalyser(ssc: StreamingContext, val tweets: DStream[Tweet]){

  def analyse(): Unit = {

      tweets
        .map(tweet => tweet.content)
        .flatMap(content => content
          .toLowerCase
          .replaceAll("[,.!?:;]", "")
          .replaceAll("""\n""", " ")
          .split(" ")
          .filter(_.startsWith("#")))
        .map((_, 1))
        .reduceByKeyAndWindow(_+ _, Seconds(60))
        .map({case (hashtag,count) => (hashtag,Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant) ,count)})
        .saveToCassandra("twitter", "hashtags", writeConf = WriteConf(ttl = TTLOption.constant(86400)))
  }
}
