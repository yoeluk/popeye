
package com.popeyepdf

import akka.actor._
import akka.event.Logging
import akka.testkit.{TestKit, ImplicitSender}
import com.typesafe.config.ConfigFactory
import scala.concurrent.duration._
import org.scalatest._

import akka.util.Timeout
import scala.collection.parallel.immutable.ParVector

class PopeyeSpec extends TestKit(ActorSystem("testPopeye")) with ImplicitSender
with WordSpecLike with Matchers with StopSystemAfterAll {

  implicit val ec = system.dispatcher

  val config = ConfigFactory.load("application")
  val ops   = config.getString("testing.jobOps").toInt

  val log = Logging.getLogger(system, this)

  // create and start our parser actor for testing
  val parser = system.actorOf(Props[PopeyeParser], "testparser")

  "The Popeye parser" must {

    implicit val timeout = Timeout(60.seconds)

    s"reply with a parsed result $ops times" in {

      def requests(o: Int)(v: ParVector[ParseRequest]): ParVector[ParseRequest] =
        if (o < ops)
          requests(o + 1)(ParseRequest(fileName = s"TestPDF$o") +: v)
        else ParseRequest(fileName = s"TestPDF$o") +: v

      requests(1)(ParVector()).foreach(r => parser ! r)

      receiveN(n = ops,  max = timeout.duration)
    }

    s"add the file name as metatag to the document" in {

      def requests(o: Int)(v: List[ParseRequest]): List[ParseRequest] =
        if (o < ops)
          requests(o + 1)(ParseRequest(fileName = s"TestPDF$o") +: v)
        else ParseRequest(fileName = s"TestPDF$o") +: v

      requests(1)(List()).foreach(r => parser ! r)

      receiveWhile(max = timeout.duration, idle = timeout.duration, messages = ops) {
        case Result(_,id) => log.info(s"result message received for $id")
      }
    }
  }
}