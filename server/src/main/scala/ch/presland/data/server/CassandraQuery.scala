package ch.presland.data.server

import akka.actor.{Actor, ActorLogging}
import com.datastax.driver.core.Session

trait CassandraQuery extends Actor with ActorLogging {
  val session: Session = CassandraConnector.connect()

  override def postStop(): Unit = {
    CassandraConnector.close(session)
  }

}
