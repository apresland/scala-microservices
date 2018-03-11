package ch.presland.data.server

import akka.actor.Props
import akka.stream.ActorMaterializer
import com.datastax.driver.core.ResultSet
import ch.presland.data.domain.Sentiments
import ch.presland.data.server._

import scala.collection.JavaConversions._
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.{ExecutionContext, Future, Promise}

object TweetSentimentActor {
  def props():Props = Props(new TweetSentimentActor())
}


class TweetSentimentActor extends CassandraQuery {

  implicit val executionContext = context.dispatcher
  implicit val actorMaterializer = ActorMaterializer

  val selectSentiments = session.prepare("SELECT * FROM twitter.sentiments")

  override def receive: Receive = {
    case sentimentId:Int => sender() ! retrieveSentiments(sentimentId)
    case _ => log.error("wrong request")
  }

  private def retrieveSentiments(sentimentId: Int)(implicit executionContext: ExecutionContext): Sentiments = {

    log.info(s"sentiment data requested")

    val rows = session
      .execute(selectSentiments.bind())
      .all()

      Sentiments(rows.length, 4,
        rows.map(row => row.getInt("hostile").toDouble),
        rows.map(row => row.getInt("negative").toDouble),
        rows.map(row => row.getInt("neutral").toDouble),
        rows.map(row => row.getInt("positive").toDouble),
        rows.map(row => row.getInt("positive").toDouble))
  }


  def sentiments(id:Int, n: Int, m: Int): Array[Double] = {
    var a = ArrayBuffer.fill[Double](m)(0)
    for (i <- 1 to 10) {a = sentiment(a)}
    a.toArray
  }

  def sentiment(a: ArrayBuffer[Double]): ArrayBuffer[Double] = {

    val y: Double = 2 * Math.random() - 0.5
    val z: Double = 10 / (0.1 + Math.random())

    for (i <- 0 to a.length-1) {
      val w = (i.toDouble/a.length.toDouble - y)*z
      a(i) += Math.exp(-w * w)
    }

    a
  }
}
