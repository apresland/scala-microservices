package ch.presland.data.server

import akka.actor.Props
import akka.stream.ActorMaterializer
import com.datastax.driver.core.ResultSet
import ch.presland.data.domain.Sentiments
import ch.presland.data.server._

import scala.collection.JavaConversions._
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.{ExecutionContext, Future, Promise}

case object AskSentimentsMessage

object TweetSentimentActor {
  def props():Props = Props(new TweetSentimentActor())
}


class TweetSentimentActor extends CassandraQuery {

  implicit val executionContext = context.dispatcher
  implicit val actorMaterializer = ActorMaterializer

  val selectSentiments = session.prepare("SELECT * FROM twitter.sentiments")

  override def receive: Receive = {
    case AskSentimentsMessage => sender() ! retrieveSentiments()
    case _ => log.error("wrong request")
  }

  private def retrieveSentiments()(implicit executionContext: ExecutionContext): Sentiments = {

    log.info(s"sentiment data requested")

    val rows = session
      .execute(selectSentiments.bind())
      .all()

      Sentiments(rows.length,
        rows.map(row => row.getTimestamp("time").toString),
        rows.map(row => row.getInt("hostile").toDouble),
        rows.map(row => row.getInt("negative").toDouble),
        rows.map(row => row.getInt("neutral").toDouble),
        rows.map(row => row.getInt("positive").toDouble))
  }
}
