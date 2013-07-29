package net.liftmodules
package restrecord

import dispatch._
import net.liftweb.common._

import com.ning.http.client.{Request}
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object HttpHelper {
  def execute[U, T](h: Http, body: (Request, FunctionHandler[U]), 
    handle: U => Box[T]): Future[Box[T]] = {
   
    h(body).either map {
      case Right(v) => handle(v)
      case Left(e) => Failure("error", Full(e), Empty)
    }
  }
}
