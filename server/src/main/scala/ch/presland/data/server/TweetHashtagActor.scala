package ch.presland.data.server

import akka.actor.Props
import akka.stream.ActorMaterializer
import ch.presland.data.domain.Sentiments

import scala.collection.JavaConversions._
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.ExecutionContext

import ch.presland.data.domain.Hashtags

case object AskHashtagsMessage

object TweetHashtagActor {
  def props():Props = Props(new TweetHashtagActor())
}

class TweetHashtagActor extends CassandraQuery {

  implicit val executionContext = context.dispatcher
  implicit val actorMaterializer = ActorMaterializer

  val selectHashtags = session.prepare("SELECT * FROM twitter.hashtags")

  override def receive: Receive = {
    case AskHashtagsMessage => sender() ! retrieveHashtags()
    case _ => log.error("wrong request")
  }

  private def retrieveHashtags()(implicit executionContext: ExecutionContext): Hashtags = {

    log.info(s"hashtag data requested")

    val rows = session
      .execute(selectHashtags.bind())
      .all()

      Hashtags(rows.length, 4,
        rows.map(row => row.getTupleValue("tag0").getString(0)),
        rows.map(row => row.getTupleValue("tag1").getString(0)),
        rows.map(row => row.getTupleValue("tag2").getString(0)),
        rows.map(row => row.getTupleValue("tag3").getString(0)),
        rows.map(row => row.getTupleValue("tag4").getString(0)),
        rows.map(row => row.getTupleValue("tag0").getInt(1).toDouble),
        rows.map(row => row.getTupleValue("tag1").getInt(1).toDouble),
        rows.map(row => row.getTupleValue("tag2").getInt(1).toDouble),
        rows.map(row => row.getTupleValue("tag3").getInt(1).toDouble),
        rows.map(row => row.getTupleValue("tag4").getInt(1).toDouble)
      )
  }
}
