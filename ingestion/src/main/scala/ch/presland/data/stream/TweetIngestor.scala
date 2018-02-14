package ch.presland.data.stream

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.kafka.scaladsl.{Consumer, Producer}
import akka.kafka.{ConsumerSettings, ProducerSettings, Subscriptions}
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.{ByteArrayDeserializer, StringDeserializer, ByteArraySerializer}
import spray.json.JsonParser
import ch.presland.data.domain.Tweet

object TweetIngestor extends App with TweetMarshaller {

  implicit val system: ActorSystem = ActorSystem("ingest-system")
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  val producerSettings = ProducerSettings(system, new ByteArraySerializer, new TweetSerializer)
    .withBootstrapServers("localhost:9092")

  val consumerSettings = ConsumerSettings(system, new ByteArrayDeserializer, new StringDeserializer)
    .withBootstrapServers("localhost:9092")
    .withGroupId("ingestion")
    .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")

  Consumer.plainSource(consumerSettings, Subscriptions.topics("tweets"))
    .map(record => {TweetUnmarshaller(JsonParser(record.value()).asJsObject).right.get})
    .map(tweet => new ProducerRecord[Array[Byte], Tweet]("mytweets",tweet))
    .runWith(Producer.plainSink(producerSettings))
}