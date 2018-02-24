package ch.presland.data.stream

import akka.actor.ActorSystem
import akka.event.LoggingAdapter
import akka.http.scaladsl.server.{Directive0, Directives, Route}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.marshalling.{Marshaller, ToEntityMarshaller}
import akka.util.Timeout

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext
import akka.actor.ActorSystem
import akka.event.LoggingAdapter
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.marshalling.{Marshaller, ToEntityMarshaller}
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.stream.Materializer
import com.typesafe.config.{Config, ConfigFactory}
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.headers._
import spray.httpx.marshalling.ToResponseMarshallable
import spray.json._
import spray.json.DefaultJsonProtocol

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.collection.mutable.ArrayBuffer

case class Person(name: String, age: Int)
case class Sentiments(data: Iterable[Double])

trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val sentimentsFormat = jsonFormat1(Sentiments)
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


  def route()(implicit system: ActorSystem, ec: ExecutionContext) : Route = {

    import akka.http.scaladsl.server.Directives._

    // format: OFF
    val service =
      get {
        path("sentiments") {
          corsHandler {
            complete(sentiments(6,200))
          }
        }
      }
    // format: ON

    def index = (path("") | pathPrefix("index.htm")) {
      getFromResource("index.html")
    }

    get {
      index
    } ~ service
  }



  def sentiments(n: Int, m: Int): Array[Double] = {

    val k = 10   // number of bumps per layer
    var a = ArrayBuffer.fill[Double](m)(0)

    a = sentiment(a,k)
    a = sentiment(a,k)
    a = sentiment(a,k)
    a = sentiment(a,k)
    a = sentiment(a,k)
    a = sentiment(a,k)
    a = sentiment(a,k)
    a = sentiment(a,k)
    a = sentiment(a,k)
    a = sentiment(a,k)
    a = sentiment(a,k)
    a.toArray
  }

  def sentiment(a: ArrayBuffer[Double], n: Int): ArrayBuffer[Double] = {

    val x: Double = 1 / (0.1 + Math.random())
    val y: Double = 2 * Math.random() - 0.5
    val z: Double = 10 / (0.1 + Math.random())
    var w: Double = 0.0

    for (i <- 0 to a.length-1) {
        w = ((i.toDouble/a.length.toDouble) -y)*z
        a(i) += x + Math.exp(-w * w)
    }
    a
  }
}
