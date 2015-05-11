package extra.queuemanager

import akka.actor._
import scala.collection.mutable
import scala.collection.immutable
import scala.util.Failure

/**
 * Created by yoelusa on 31/03/15.
 */
object Manager {

  sealed trait Work[+A]

  final case class Job[+A](document: A, worker: ActorRef) extends Work[A]

  object Work {

    def canProcessJob[A](work: Work[A])(f: Tester[A]): Boolean = work match {
      case job: Job[A] => canProcessJobs(immutable.HashMap((job.document, job)))(f)
      case _ => false
    }
    def canProcessJobs[A](currentJobs: immutable.HashMap[A, Work[A]])(f: Tester[A]): Boolean =
      f(currentJobs)
  }

  final type Tester[A] = immutable.HashMap[A, Work[A]] => Boolean

  final case class Task[A](work: Work[A], requester: ActorRef)

  final case class JobDone[A](document: A, result: Any)

  final case class WorkResult(result: Any, requester: ActorRef)

  implicit final def tasksToJobs[A]: mutable.Map[A, Task[A]] => immutable.HashMap[A, Work[A]] =
    _.foldLeft(immutable.HashMap.empty[A, Work[A]])((ac, m) => ac + ((m._1, m._2.work)))

  abstract class AbstractManager[T](f: Tester[T]) extends Actor with ActorLogging {
    import Work._

    def receive = {
      case work: Job[T] => processWork(work, sender())
      case done: JobDone[T] =>
        ongoingJobs.get(done.document) match {
          case Some(task) =>
            task.requester ! done.result
            ongoingJobs -= done.document
            processQueue()
          case _ =>
        }
    }

    private[queuemanager] final val queuedJobs = mutable.HashMap.empty[T, Task[T]]
    private[queuemanager] final val ongoingJobs = mutable.HashMap.empty[T, Task[T]]

    private[queuemanager] final def processWork(work: Job[T], captSender: ActorRef) =
      if (canProcessJob(work)(f)) {
        canProcessJobs(ongoingJobs + ((work.document, Task(work, captSender))))(f) match {
          case true =>
            val task = Task(work, captSender)
            ongoingJobs += ((work.document, task))
            work.worker ! work.document
          case _ => queuedJobs += ((work.document, Task(work, captSender)))
        }
      }
      else captSender ! Failure(exception = new Throwable("The job is too large! "))

    private[queuemanager] final def processQueue() = queuedJobs.headOption match {
      case Some(kv) =>
        if (canProcessJobs(ongoingJobs + kv)(f)) {
          val task = queuedJobs.remove(kv._1).get
          task.work match {
            case work: Job[T] =>
              ongoingJobs += ((work.document, task))
              work.worker ! work.document
            case _ =>
          }
      }
      case _ =>
    }
  }
}
