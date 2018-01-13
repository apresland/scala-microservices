package ch.presland.data.stream

import akka.actor.ActorSystem
import akka.actor.Props
import akka.event.Logging
import akka.stream.ActorMaterializer

object TweetStreamer {

  implicit val system: ActorSystem = ActorSystem("stream-system")
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  def main(args: Array[String]): Unit = {
    val streamer = new TweetStreamer(system)
    streamer.run()
  }
}

class TweetStreamer(system: ActorSystem) {

  private val log = Logging(system, getClass.getName)

  def run(): Unit = {
    log.info("Starting streamer...")
  }
}
