package ch.presland.data.server

import akka.actor.Props
import akka.stream.ActorMaterializer
import scala.collection.JavaConversions._
import scala.concurrent.ExecutionContext
import ch.presland.data.domain.Hashtags

case object AskHashtagsMessage

object TweetHashtagActor {
  def props():Props = Props(new TweetHashtagActor())
}

class TweetHashtagActor extends CassandraQuery {

  implicit val executionContext = context.dispatcher
  implicit val actorMaterializer = ActorMaterializer

  val selectHashtags = session.prepare("SELECT hashtag FROM twitter.hashtags")

  override def receive: Receive = {
    case AskHashtagsMessage => sender() ! retrieveHashtags()
    case _ => log.error("wrong request")
  }

  private def retrieveHashtags()(implicit executionContext: ExecutionContext): Hashtags = {

    log.info(s"Hashtag data requested")

    val hashtags = session
      .execute(selectHashtags.bind())
      .all()

    Hashtags(hashtags
      .map(row => row.getString(0))
      .mkString(" "))
  }
}
