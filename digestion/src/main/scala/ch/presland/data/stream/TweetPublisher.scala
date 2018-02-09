package ch.presland.data.stream

import akka.stream.actor.ActorPublisher
import akka.stream.actor.ActorPublisherMessage.{Cancel, Request}
import ch.presland.data.domain.Tweet

class TweetPublisher extends ActorPublisher[Tweet] {
  override def receive: Receive = {
    case tweet: Tweet => {
      println("************** TweetPublisher: " + tweet.text)
    }
    case Cancel => context.stop(self);
    case Request(_) => {}
  }
}