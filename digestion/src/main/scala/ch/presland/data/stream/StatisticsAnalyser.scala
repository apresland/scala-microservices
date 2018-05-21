package ch.presland.data.stream

import ch.presland.data.domain.Tweet
import com.datastax.spark.connector.streaming._
import com.datastax.spark.connector.writer.{TTLOption, WriteConf}
import org.apache.spark.streaming.dstream.DStream
import org.apache.spark.streaming.{StreamingContext}

class StatisticsAnalyser(ssc: StreamingContext, val tweets: DStream[Tweet]){

  def analyse(): Unit = {
      tweets
        .map(tweet => (tweet.id, tweet.date, tweet.user))
        .saveToCassandra("twitter", "statistics", writeConf = WriteConf(ttl = TTLOption.constant(86400)))
  }
}
