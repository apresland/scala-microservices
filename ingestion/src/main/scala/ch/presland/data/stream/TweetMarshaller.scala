package ch.presland.data.stream

import ch.presland.data.domain.{Tweet, User}
import spray.httpx.unmarshalling.MalformedContent
import spray.httpx.unmarshalling.Deserialized
import spray.json.{JsObject, JsValue, JsString, JsNumber}

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

trait TweetMarshaller {

  implicit object TweetUnmarshaller {

    val dateFormat: SimpleDateFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss ZZZZZ yyyy", Locale.ENGLISH)

    def deserializeTweet(json: JsObject): Deserialized[Tweet] = {

      val user = json.fields.get("user") match {
        case Some(u) => deserializeUser(u)
        case None => Left(MalformedContent("malformed user field"))
      }

      ( json.fields.get("id_str"),
        json.fields.get("created_at"),
        json.fields.get("text")) match {
        case (Some(JsString(id)),
              Some(JsString(date)),
              Some(JsString(text))) =>
                Right(Tweet(id, dateFormat.parse(date), user.right.get.name, text))
        case _ => Left(MalformedContent("malformed tweet"))
      }
    }

    def deserializeUser(user: JsValue): Deserialized[User] = user match {

      case JsObject(fields) =>
        (fields.get("id"),
          fields.get("name"),
          fields.get("screen_name"),
          fields.get("lang"),
          fields.get("followers_count")) match {
          case (Some(JsNumber(id)),
          Some(JsString(name)),
          Some(JsString(screen_name)),
          Some(JsString(lang)),
          Some(JsNumber(followers))) => Right(User(id.toInt, name, screen_name, lang, followers.toInt))

          case _ => Left(MalformedContent("malformed user"))
        }
      case _ => Left(MalformedContent("malformed tweet"))

    }

    def apply(json: JsObject): Deserialized[Tweet] = {
      deserializeTweet(json)
    }
  }
}
