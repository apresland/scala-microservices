package ch.presland.data.stream

import scala.collection.immutable.Map

object SentimentAggregator {

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
