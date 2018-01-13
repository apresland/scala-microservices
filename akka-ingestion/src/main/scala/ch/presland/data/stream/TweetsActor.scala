package ch.presland.data.stream

import akka.event.Logging
import akka.stream.actor.ActorPublisher
import ch.presland.data.domain.Tweet

class TweetsActor extends ActorPublisher[Tweet] {

  val sub = context.system.eventStream.subscribe(self, classOf[Tweet])

  private val log = Logging(context.system, getClass.getName)

  override def receive: Receive = {
    case tweet: Tweet => {
      //log.info("@" + tweet.name + "- " + tweet.content)
    }
  }

  override def postStop():Unit = context.system.eventStream.unsubscribe(self)
}