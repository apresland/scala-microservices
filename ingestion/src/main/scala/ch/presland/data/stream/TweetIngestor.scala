package ch.presland.data.stream

import akka.actor.Props
import akka.actor.ActorSystem
import akka.actor.ActorRef
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import akka.kafka.scaladsl.Producer
import akka.kafka.ProducerSettings
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.{ByteArraySerializer, StringSerializer}
import ch.presland.data.domain.Tweet

object TweetIngestor extends App{

  implicit val system: ActorSystem = ActorSystem("ingest-system")
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  val producerSettings = ProducerSettings(system, new ByteArraySerializer, new StringSerializer)
    .withBootstrapServers("localhost:9092")

  val source = Source
    .actorPublisher[Tweet](Props[TweetPublisher])
    .map{_.content}
    .map{ new ProducerRecord[Array[Byte], String]("tweets", _)}

  val actor: ActorRef = source
    .to(Producer.plainSink(producerSettings))
    .run()

  val twitterClient = new TwitterClient(actor)
  twitterClient.start()
}