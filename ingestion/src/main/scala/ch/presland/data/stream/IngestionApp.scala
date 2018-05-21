package ch.presland.data.stream

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.kafka.scaladsl.{Consumer, Producer}
import akka.kafka.{ConsumerSettings, ProducerSettings, Subscriptions}
import akka.stream.scaladsl.Sink
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.serialization.{ByteArrayDeserializer, ByteArraySerializer, StringDeserializer}
import spray.json.JsonParser
import ch.presland.data.domain.Tweet

object IngestionApp extends App with TweetMarshaller {

  implicit val system: ActorSystem = ActorSystem("ingest-system")
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  val producerSettings = ProducerSettings(system, new ByteArraySerializer, new TweetSerializer)
    .withBootstrapServers("localhost:9092")

  val consumerSettings = ConsumerSettings(system, new ByteArrayDeserializer, new StringDeserializer)
    .withBootstrapServers("localhost:9092")
    .withGroupId("ingestion")
    .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")

  val source = Consumer
    .plainSource(consumerSettings, Subscriptions.topics("tweets"))

  val sink = Producer
    .plainSink(producerSettings)

  def toTweet(record: ConsumerRecord[Array[Byte],String]): Tweet = {
    {TweetUnmarshaller(JsonParser(record.value()).asJsObject).right.get}
  }

  def toRecord(tweet: Tweet): ProducerRecord[Array[Byte], Tweet] = {
    new ProducerRecord[Array[Byte], Tweet]("ingested-tweets",tweet)
  }

  source
    .map(json => toTweet(json))
    .map(tweet => println(tweet))
    .runWith(Sink.ignore)

  source
    .map(json => toTweet(json))
    .map(tweet => toRecord(tweet))
    .runWith(sink)
}