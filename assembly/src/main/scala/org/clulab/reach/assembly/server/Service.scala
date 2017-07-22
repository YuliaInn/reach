package org.clulab.reach.assembly.server

import akka.actor.ActorSystem
import akka.event.LoggingAdapter
import akka.http.scaladsl.server.Directives._
import akka.stream.Materializer
import org.json4s.DefaultFormats
import org.json4s.jackson.Serialization
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.ExecutionContextExecutor
import com.typesafe.config.Config


trait Service {

  implicit val serialization = Serialization
  implicit val formats = DefaultFormats

  implicit val system: ActorSystem

  implicit def executionContext: ExecutionContextExecutor

  implicit val materializer: Materializer

  def config: Config

  val logger: LoggingAdapter

  def in[U](duration: FiniteDuration)(body: => U): Unit =
    system.scheduler.scheduleOnce(duration)(body)

  //  def apiRequest(request: HttpRequest): Future[HttpResponse] = Source.single(request).via(apiConnectionFlow).runWith(Sink.head)

  val static = "org/clulab/reach/assembly/server/static"
  val routes = {
    // log results
    logRequestResult("reach-assembly") {
      // index page
      path("") {
        getFromResource(s"$static/index.html")
      } ~
      // annotation UI
      path("annotate") {
        getFromResource(s"$static/annotate.html")
      } ~
      // model UI
      path("model") {
        getFromResource(s"$static/model.html")
      }
    }
  }
}
