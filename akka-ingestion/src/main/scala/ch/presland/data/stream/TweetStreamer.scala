package ch.presland.data.stream

import akka.actor.ActorSystem
import akka.actor.Props
import akka.event.Logging
import akka.stream.ActorMaterializer
import org.json4s.jackson
import org.json4s.jackson.Serialization

object TweetStreamer {

  implicit val system: ActorSystem = ActorSystem("stream-system")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  val actor = system.actorOf(Props(new TweetsActor))

  def main(args: Array[String]): Unit = {
    val streamer = new TweetStreamer(system)
    streamer.run()
  }
}

class TweetStreamer(system: ActorSystem) {

  implicit val serialization: Serialization.type = jackson.Serialization
  private val log = Logging(system, getClass.getName)

  def run(): Unit = {
    log.info("Starting tweet streamer...")
    val twitterClient = new TwitterClient(system)
    twitterClient.start()
  }
}
