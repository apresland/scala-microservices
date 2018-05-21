package ch.presland.data.stream

import akka.actor.{Actor, ActorRef}
import org.apache.spark.streaming.{StreamingContext, receiver}
import ch.presland.data.domain.Tweet

case class SubscribeReciever(ref: ActorRef)
case class UnsubscribeReceiver(ref: ActorRef)

class CustomActor() extends Actor {

  override def preStart(): Unit = {super.preStart()}
  override def postStop(): Unit = {super.postStop()}


  def receive: Actor.Receive = {
    case Tweet => println("XXXXXXXXXXX recieved tweet: ")
  }


}
