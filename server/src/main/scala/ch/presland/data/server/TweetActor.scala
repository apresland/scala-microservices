package ch.presland.data.server

import java.util.Date

import akka.actor.Props
import akka.stream.ActorMaterializer
import ch.presland.data.domain.Tweets

import scala.collection.JavaConversions._
import scala.concurrent.ExecutionContext

object TweetActor {
  def props():Props = Props(new TweetActor())
}

class TweetActor extends CassandraQuery {

  implicit val executionContext = context.dispatcher
  implicit val actorMaterializer = ActorMaterializer

  val selectTweets = session.prepare("SELECT * FROM twitter.tweets LIMIT 100")

  override def receive: Receive = {
    case rank:Int => sender() ! retrieveTweets(rank)
    case _ => log.error("wrong request")
  }

  private def retrieveTweets(TweetId: Int)(implicit executionContext: ExecutionContext): Tweets = {

    log.info(s"tweet data requested")

    val rows = session
      .execute(selectTweets.bind())
      .all()

    Tweets(rows.length, rows.map(row => row.getTimestamp("time").toString), rows.map(row => row.getString("content")))
  }
}
