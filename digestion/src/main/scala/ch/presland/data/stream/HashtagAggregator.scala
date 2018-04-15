package ch.presland.data.stream

import scala.collection.immutable.Map

object HashtagAggregator {

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
