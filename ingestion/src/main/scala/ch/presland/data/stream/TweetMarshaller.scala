package ch.presland.data.stream

import akka.actor.{Actor, ActorRef}
import ch.presland.data.domain.{Place, Tweet, User}
import spray.httpx.unmarshalling.MalformedContent
import spray.httpx.unmarshalling.Deserialized
import spray.json.{JsObject, _}

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
