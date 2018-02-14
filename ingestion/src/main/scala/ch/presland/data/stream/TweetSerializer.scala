package ch.presland.data.stream

import java.util
import org.apache.kafka.common.serialization.Serializer
import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper}
import com.fasterxml.jackson.module.scala.DefaultScalaModule

import ch.presland.data.domain.Tweet

class TweetSerializer extends Serializer[Tweet] {

  override def serialize(topic: String, data: Tweet): Array[Byte] = {
    val mapper: ObjectMapper = new ObjectMapper()
    mapper.registerModule(DefaultScalaModule)

    mapper.writeValueAsBytes(data)
  }

  override def configure(configs: util.Map[String, _], isKey: Boolean): Unit = {}

  override def close(): Unit = {}

}
