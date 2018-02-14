package ch.presland.data.stream

import java.util

import org.apache.kafka.common.serialization.Deserializer
import ch.presland.data.domain.Tweet
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule

class TweetDeserializer extends  Deserializer[Tweet] {

  override def configure(configs: util.Map[String, _], isKey: Boolean): Unit = {}

  override def close(): Unit = {}

  override def deserialize(topic: String, data: Array[Byte]): Tweet = {
    val mapper: ObjectMapper = new ObjectMapper()
    mapper.registerModule(DefaultScalaModule)
    mapper.readValue(data, classOf[Tweet])
  }

}
