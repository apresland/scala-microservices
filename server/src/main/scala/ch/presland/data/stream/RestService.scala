package ch.presland.data.stream

import akka.http.scaladsl.server.{Directive0, Directives, Route}
import scala.concurrent.ExecutionContext
import akka.actor.ActorSystem

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.headers._
import spray.json.DefaultJsonProtocol

import scala.collection.mutable.ArrayBuffer

case class Sentiments(length: Int, dimension: Int, zero: Iterable[Double], one: Iterable[Double], two: Iterable[Double], three: Iterable[Double], four: Iterable[Double])

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


  def route()(implicit system: ActorSystem, ec: ExecutionContext) : Route = {

    import akka.http.scaladsl.server.Directives._

    val service =
      get {
        path("sentiments") {
          corsHandler {
            complete(Sentiments(
              200,
              5,
              sentiments(10,200),
              sentiments(10,200),
              sentiments(10,200),
              sentiments(10,200),
              sentiments(10,200)))
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



  def sentiments(n: Int, m: Int): Array[Double] = {
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
