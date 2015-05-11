### Popeye for PDFBox 
Popeye is a queue job manager (in package extra.queuemanager) with Akka.
It offers the implementer an abstract class (Manager) of [T] that takes a function of type 
`HashMap[T, Work[T]] => Boolean`. Where Work is 

```scala
case class Job[T](document: T, worker: ActorRef) extends Work[T]
```

Popeye is designed for managing asynchronous jobs concurrently and in parallel on paged documents. It provides a configuration settings `running.parOps` in resources/application.conf for setting the number of parallel operations supported (typically this is the maximum number of pages that are being processed at any one time across all the documents submitted to the manager).
In this example implementation Popeye for PDFBox parses pdf files.