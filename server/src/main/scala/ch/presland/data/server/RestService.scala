package ch.presland.data.server

import akka.http.scaladsl.server.{Directive0, Directives, Route}

import scala.concurrent.ExecutionContext
import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.pattern.ask
import akka.util.Timeout
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.headers._
import spray.json.DefaultJsonProtocol

import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.duration._
import com.datastax.driver.core.{ResultSet, Session}

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.{ExecutionContext, Future}
import ch.presland.data.domain.{Hashtags, Sentiments, Tweets, Statistics}

trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val sentimentsFormat = jsonFormat6(Sentiments)
  implicit val tweetsFormat = jsonFormat3(Tweets)
  implicit val statisticsFormat = jsonFormat3(Statistics)
  implicit val wordcloudFormat = jsonFormat1(Hashtags)
}

trait CorsSupport extends JsonSupport {

  lazy val allowedOriginHeader = `Access-Control-Allow-Origin`.*

  private def addAccessControlHeaders: Directive0 = {
    mapResponseHeaders { headers =>
      allowedOriginHeader +:
        `Access-Control-Allow-Credentials`(true) +:
        `Access-Control-Allow-Headers`("Content-Type", "X-Requested-With", "Authorization", "Token") +:
        headers
    }
  }

  private def preflightRequestHandler: Route = options {
    complete(HttpResponse(200).withHeaders(
      `Access-Control-Allow-Methods`(OPTIONS, POST, PUT, GET, DELETE)
    ))
  }

  def corsHandler(r: Route) = addAccessControlHeaders {
    preflightRequestHandler ~ r
  }
}

trait RestService extends CorsSupport {

  val session: Session

  def route()(implicit system: ActorSystem, ec: ExecutionContext) : Route = {

    import akka.http.scaladsl.server.Directives._
    implicit val timeout = Timeout(5 seconds)

    val sentimentsActor = system.actorOf(TweetSentimentActor.props(), "tweet-sentiments")
    val hashtagsActor = system.actorOf(TweetHashtagActor.props(), "tweet-hashtags")
    val statisticsActor = system.actorOf(TweetStatisticsActor.props(), "tweet-statistics")
    val tweetsActor = system.actorOf(TweetActor.props(), "tweet-tweets")

    val sentiments =
      get {
        path("sentiments") {
          corsHandler {
            complete(
              ask(sentimentsActor, AskSentimentsMessage).mapTo[Sentiments]
            )
          }
        }
      }

    val hashtags =
      get {
        path("hashtags") {
          complete(
            ask(hashtagsActor, AskHashtagsMessage).mapTo[Hashtags]
          )
        }
      }

    val statistics =
      get {
        path("statistics") {
          complete(
            ask(statisticsActor, AskStatisticsMessage).mapTo[Statistics]
          )
        }
      }

    val tweets =
      get {
        path("tweets") {
          complete(
            ask(tweetsActor, AskTweetsMessage).mapTo[Tweets]
          )
        }
      }

    def timeline = path("timeline") {
      getFromResource("sentiments.html")
    }

    def tagcloud = path("tagcloud") {
      getFromResource("wordcloud.html")
    }

    def histogram = path("histogram") {
      getFromResource("histogram.html")
    }

    get {
      histogram
    } ~ timeline ~ tagcloud ~ hashtags ~ tweets ~ sentiments ~ statistics
  }
}
