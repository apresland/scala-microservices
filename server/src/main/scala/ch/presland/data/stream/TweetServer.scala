package ch.presland.data.stream

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.HttpMethods._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success}



object TweetServer extends RestService {

  implicit val system = ActorSystem("service-api-http")
  implicit val executor = system.dispatcher
  implicit val materializer = ActorMaterializer()


  def main(args: Array[String]): Unit = {

    Http().bindAndHandle(route(), "localhost", 9090)
      .onComplete {
        case Success(_) => println(s"Successfully bound")
        case Failure(e) => println(s"Failed !!!!")
      }

    Await.ready(system.whenTerminated, Duration.Inf)
  }
}
