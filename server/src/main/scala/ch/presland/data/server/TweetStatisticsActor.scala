package ch.presland.data.server

import akka.actor.Props
import akka.stream.ActorMaterializer
import ch.presland.data.domain.Statistics
import scala.concurrent.ExecutionContext
import scala.collection.JavaConversions._


case object AskStatisticsMessage

object TweetStatisticsActor {
  def props():Props = Props(new TweetStatisticsActor())
}

class TweetStatisticsActor extends CassandraQuery {

  implicit val executionContext = context.dispatcher
  implicit val actorMaterializer = ActorMaterializer

  val selectStatistics = session.prepare("SELECT * FROM twitter.statistics")

  override def receive: Receive = {
    case AskStatisticsMessage => sender() ! retrieveStatistics()
    case _ => log.error("wrong request")
  }

  private def retrieveStatistics()(implicit executionContext: ExecutionContext): Statistics = {

    log.info(s"Statistics data requested")

    val rows = session
      .execute(selectStatistics.bind())
      .all()

    Statistics(
      rows.map(row => row.getString("id")),
      rows.map(row => row.getTimestamp("date").toString),
      rows.map(row => row.getString("user")))
  }
}
