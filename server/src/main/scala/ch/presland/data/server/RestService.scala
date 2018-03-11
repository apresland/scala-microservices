package ch.presland.data.server

import akka.http.scaladsl.server.{Directive0, Directives, Route}
import scala.concurrent.ExecutionContext
import akka.actor.{ActorRef,ActorSystem}

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.ws.{Message,TextMessage}
import akka.pattern.ask
import akka.util.Timeout
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.headers._
import spray.json.DefaultJsonProtocol
import scala.concurrent.Await
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._
import com.datastax.driver.core.{ResultSet, Session}

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.{ExecutionContext,Future}

import ch.presland.data.domain.Sentiments

trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val sentimentsFormat = jsonFormat7(Sentiments)
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

    val service =
      get {
        path("sentiments") {
          corsHandler {
            complete(
              ask(sentimentsActor,0).mapTo[Sentiments]
            )
          }
        }
      }

    def index = (path("") | pathPrefix("index.htm")) {
      getFromResource("index.html")
    }

    get {
      index
    } ~ service
  }



  def sentiments(id:Int, n: Int, m: Int): Array[Double] = {
    var a = ArrayBuffer.fill[Double](m)(0)
    for (i <- 1 to 10) {a = sentiment(a)}
    a.toArray
  }

  def sentiment(a: ArrayBuffer[Double]): ArrayBuffer[Double] = {

    val y: Double = 2 * Math.random() - 0.5
    val z: Double = 10 / (0.1 + Math.random())

    for (i <- 0 to a.length-1) {
        val w = (i.toDouble/a.length.toDouble - y)*z
        a(i) += Math.exp(-w * w)
    }

    a
  }
}
