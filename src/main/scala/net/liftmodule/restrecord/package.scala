package net.liftmodules

import net.liftweb.util._
import net.liftweb.common._
import Helpers._
import net.liftweb.json.JsonAST._
import net.liftweb.json.{Printer}
import com.ning.http.client.{RequestBuilder}

package object restrecord {
  implicit def jobjectToString(in: JObject): String = Printer.compact(render(in))

  implicit def implyRequestBuilderToWebService(builder: RequestBuilder): WebService =
    new WebServiceImpl(builder)
}
