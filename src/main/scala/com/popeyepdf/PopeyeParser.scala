package com.popeyepdf

import akka.actor._
import spray.json._
import PopeyeJsonProtocol._
import scala.annotation.tailrec
import scala.collection.generic.CanBuildFrom
import scala.collection.parallel.immutable.ParVector
import scala.concurrent.duration._
import scala.collection.{GenSeq, GenSeqLike, immutable}
import akka.util.Timeout

import com.typesafe.config.ConfigFactory
import java.io.FileInputStream
import org.apache.pdfbox.util.{PDFStreamEngine, PDFTextStripperByArea, PDFTextStripper}
import org.apache.pdfbox.pdmodel._
import extra.queuemanager.Manager._
import scala.util.{Success, Failure}

import akka.pattern.{ask, pipe}

/**
 * Created by yoelusa on 24/03/15.
 */

case class JobRequest(request: ParseRequest, requester: ActorRef)
case class JobPackage(document: PDDocument, requester: ActorRef)

class PopeyeParser extends Actor with ActorLogging {

  import context._
  import ManagerInitializer._
  implicit val timeout = Timeout(60 seconds)

  val workManager = context.actorOf(Props[ManagerActor], name = "manager")

  def receive = {
    case request: ParseRequest =>
      val jobDispatcher = createDispatcher
      jobDispatcher ! JobRequest(request, sender())
    case pack: JobPackage =>
      val parser = createParser
      val job = Job(pack.document, parser)
      workManager ? job pipeTo pack.requester
    case _ =>
      sender() ! FailedResult(failedMsg = "Unknown request!")
  }

  def createDispatcher = {
    context.actorOf(Props[JobDispatcher])
  }
  def createParser = {
    context.actorOf(Props[Parser])
  }
}

object ManagerInitializer {

  class ManagerActor extends AbstractManager(eval compose pages) {}

  val config = ConfigFactory.load("application")
  val parOps   = config.getString("running.parOps").toInt

  def eval: Int => Boolean = { case p => parOps >= p }

  def pages: immutable.HashMap[PDDocument, Work[PDDocument]] => Int =
    _.foldLeft(0)((ac, w) => w match {
      case (j,_) => j.getNumberOfPages + ac
      case _ => ac
    })
}

class JobDispatcher extends Actor {

  implicit val timeout = Timeout(60 seconds)

  def receive = {
    case r: JobRequest =>
      val config = ConfigFactory.load("application")
      val f = new FileInputStream(System.getProperty("user.home") + config.getString("pdfs_dir") + r.request.fileName + ".pdf")
      if (f != null) {
        f.available()
        val document = PDDocument.load(f)
        val info = document.getDocumentInformation
        info.setCustomMetadataValue("fileName", r.request.fileName)
        sender() ! JobPackage(document, r.requester)
        self ! PoisonPill
      } else {
        sender() ! FailedResult(failedMsg = s"The file ${r.request.fileName} was not found!")
        self ! PoisonPill
      }
    case _ =>
      sender() ! FailedResult(failedMsg = "A job dispatch could not be constructed from the request!")
      self ! PoisonPill
  }
}

class Parser extends Actor with ActorLogging {
  //import Stripper._

//  implicit object implicitStripper extends Strippable[ParVector[PDFTextStripper], PDFTextStripper] {
//    def goStripper(init: ParVector[PDFTextStripper])(p: Int): ParVector[PDFTextStripper] =
//      (0 to p-1).reverse.foldLeft(init)((ac, i) => {
//        val str = new PDFTextStripper
//        str.setStartPage(i)
//        str.setEndPage(i)
//        str +: ac
//      })
//  }

  def receive = {
    case doc: PDDocument =>
      val info = doc.getDocumentInformation
      val pages = doc.getNumberOfPages

      /**
       * the lines below show how one would use fpinscala.parallelism library
       */
//      import fpinscala.parallelism.Nonblocking.Par._
//      import java.util.concurrent.Executors
//      val stripperSeq = goStripper(IndexedSeq.empty[PDFTextStripper])(pages)
//      val parStripper = parMap(stripperSeq)(_.getText(doc))
//      val extrText = run(Executors.newFixedThreadPool(2))(parStripper)
      /**
       * the next two lines build a ParVector with the pages and process them in parallel
       * this is probably more efficient as well as more concise
       */

      val parStripper: ParVector[PDFTextStripper] = goStripper(ParVector.empty[PDFTextStripper])(pages) //Stripper(ParVector.empty[PDFTextStripper])(pages)
      val extrText = parStripper.map(_.getText(doc)).toList

      sender() ! JobDone(doc, Result(result = extrText.toJson, id = info.getCustomMetadataValue("fileName")))

      doc.close()
      self ! PoisonPill
    case _ =>
      log.error("impossible to extract text from unknown file type... ")
      self ! PoisonPill
  }

  def goStripper[Repr[T] <: GenSeqLike[T, Repr[T]]](init: Repr[PDFTextStripper])(p: Int)
  (implicit cbf: CanBuildFrom[Repr[PDFTextStripper], PDFTextStripper, Repr[PDFTextStripper]]): Repr[PDFTextStripper] =
    (0 to p-1).reverse.foldLeft(init)((ac, i) => {
      val str = new PDFTextStripper
      str.setStartPage(i)
      str.setEndPage(i)
      str +: ac
    })
}

//object Stripper {
//  trait Strippable[Repr[T], A] {
//    def goStripper(p: Int)(implicit cbf: CanBuildFrom[Repr[A], A, Repr[A]]): Repr[A]
//  }
//  def apply[A, B, Repr[T] <: GenSeqLike[T, Repr[T]]: Strippable](init: Repr[A])(p: Int)(implicit r: Strippable[Repr[B], A])
//   = implicitly[Strippable[Repr[B], A]].goStripper(p)
//}