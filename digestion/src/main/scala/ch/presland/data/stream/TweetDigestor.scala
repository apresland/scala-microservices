package ch.presland.data.stream

import akka.actor.Props
import akka.actor.ActorSystem
import akka.actor.{Actor, ActorRef}
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.spark.SparkConf
import org.apache.spark.streaming.{StreamingContext, Seconds}
import org.apache.spark.streaming.kafka010.KafkaUtils
import org.apache.spark.streaming.kafka010.LocationStrategies.PreferConsistent
import org.apache.spark.streaming.kafka010.ConsumerStrategies.Subscribe
import scala.collection.immutable.Map
import spray.json._
import spray.httpx.unmarshalling.{MalformedContent, Deserialized}

import ch.presland.data.domain.{Tweet,User, Place}

trait TweetMarshaller {

implicit object TweetUnmarshaller {

  def deserializeTweet(json: JsObject): Deserialized[Tweet] = {

    (json.fields.get("created_at"),
      json.fields.get("id_str"),
      json.fields.get("text")) match {
      case (Some(JsString(created)), Some(JsString(id)), Some(JsString(text))) => Right(Tweet(created, id, text))
      case _ => Left(MalformedContent("malformed tweet"))
    }
  }

  def deserializeUser(user: JsValue): Deserialized[Option[User]] = user match {
    case JsObject(fields) =>
      (fields.get("id"),
        fields.get("name"),
        fields.get("lang"),
        fields.get("followers_count"),
        fields.get("geo_enabled")) match {
        case (Some(JsNumber(id)),
        Some(JsString(name)),
        Some(JsString(lang)),
        Some(JsNumber(followers)),
        Some(JsBoolean(geo))) => Right(Some(User(id.toInt, name, lang, followers.toInt, geo)))

        case _ => Left(MalformedContent("malformed user"))
      }
    case JsNull => Right(None)
    case _ => Left(MalformedContent("malformed tweet"))

  }

  def deserializePlace(place: JsValue): Deserialized[Option[Place]] = place match {
    case JsObject(fields) =>
      (fields.get("id"),
        fields.get("country"),
        fields.get("full_name")) match {
        case (Some(JsString(id)),
        Some(JsString(country)),
        Some(JsString(fullname))) => Right(Some(Place(id, country, fullname)))

        case _ => Left(MalformedContent("malformed place"))
      }
    case JsNull => Right(None)
    case _ => Left(MalformedContent("malformed tweet"))
  }

  def apply(json: JsObject): Deserialized[Tweet] = {

    val tweet = deserializeTweet(json)
    val user = json.fields.get("user") match {
      case Some(u) => deserializeUser(u)
    }
    val place = json.fields.get("place") match {
      case Some(p) => deserializePlace(p)
    }
    tweet
  }
}

}

class TweetDigestionActor(processor: ActorRef) extends Actor with TweetMarshaller {

  override def receive: Receive = {
    case json: JsObject => {
      TweetUnmarshaller(json).fold(_=>(), processor !)
    }
  }
}

object TweetDigestor extends App {

  val system = ActorSystem()
  val publish = system.actorOf(Props(new TweetPublisher()))
  val digestion = system.actorOf(Props(new TweetDigestionActor(publish)))

  val sparkConf = new SparkConf()
      .setAppName(getClass.getSimpleName)
      .setMaster("local[*]")
      .set("spark.cassandra.connection.host", "localhost")
      .set("spark.cassandra.connection.port", "9042" )
      .set("spark.cassandra.connection.keep_alive_ms", "30000")

  val batchDuration = Seconds(10)
  val ssc = new StreamingContext(sparkConf, batchDuration)

  val kafkaParams = Map[String, Object] (
    "bootstrap.servers" -> "localhost:9092",
    "key.deserializer" -> classOf[StringDeserializer].getName,
    "value.deserializer" -> classOf[StringDeserializer].getName,
    "group.id" -> "digestion",
    "auto.offset.reset" -> "latest",
    "enable.auto.commit" -> (false: java.lang.Boolean)
  )

  val stream = KafkaUtils.createDirectStream[String,String](
    ssc,
    PreferConsistent,
    Subscribe[String,String](Array("tweets"), kafkaParams)
  )

  val tweets = stream
    .map {consumerRecord => consumerRecord.value()}
    .foreachRDD(rdd => { rdd
      .map(s => {digestion ! JsonParser(s).asJsObject})
      .collect()
      //.saveToCassandra("twitter", "tweets")
  })

  ssc.start()
  ssc.awaitTermination()
}
