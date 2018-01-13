package ch.presland.data.stream

import akka.actor.Props
import akka.event.Logging
import akka.actor.ActorSystem
import akka.actor.ActorRef
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import ch.presland.data.domain.Tweet

object TweetStreamer {

  val source = Source.actorPublisher[Tweet](Props[TweetPublisher])
  val sink = Sink.foreach(println)
  val flow = Flow[Tweet]

  implicit val system: ActorSystem = ActorSystem("stream-system")
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  val actor: ActorRef = flow.to(sink).runWith(source)

  def main(args: Array[String]): Unit = {
    val streamer = new TweetStreamer(system, actor)
    streamer.run()
  }
}

class TweetStreamer(system: ActorSystem, actor: ActorRef) {

  private val log = Logging(system, getClass.getName)

  def run(): Unit = {
    log.info("Starting tweet streamer...")
    val twitterClient = new TwitterClient(actor)
    twitterClient.start()
  }
}
