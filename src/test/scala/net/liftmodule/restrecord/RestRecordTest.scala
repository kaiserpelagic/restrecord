import org.scalatest._
import org.scalatest.matchers.MustMatchers
import org.scalamock.scalatest.MockFactory

import net.liftmodules.restrecord._
import net.liftweb.record.field._
import dispatch._
import net.liftweb.json.JsonAST._
import net.liftweb.common._
import scala.concurrent.ExecutionContext.Implicits.global
import com.ning.http.client.{RequestBuilder, Request, Response}

import scala.xml._
import java.net.{URLEncoder,URLDecoder}

import unfiltered.netty
import unfiltered.response._
import unfiltered.request._

object TestServer {
  val server = {
    netty.Http.anylocal.handler(netty.cycle.Planify {
      case req @ Path(Seg("echo" :: json :: Nil)) => 
        JsonContent ~> ResponseString(URLDecoder.decode(json, "utf-8"))
    }).start()
  }
}

trait Config {
  val configuration = RestRecordConfig("127.0.0.1", Full(TestServer.server.port))
}

class Echo extends RestRecord[Echo] {
  def meta = Echo
  val uri = "echo" :: * :: Nil

  object id extends IntField(this, 0)
}

object Echo extends Echo with RestMetaRecord[Echo] with Config

class RestRecordTest extends FunSuiteWithSession with MustMatchers {

  test("Can create a RestRecord") {
    val foo = Echo.createRecord
    foo must not equal (null)
  }

  test("Can set a field on a RestRecord") {
    val foo = Echo.createRecord.id(1)
    foo.id.valueBox.openOr(0) must equal (1) 
  }

  test("Can get a resource from a json") {
    val id = 2
    val json = """{"id": %s}""".format(id)
    val resp = Echo.find(json)().openOr(Echo.createRecord)
    resp.id.valueBox.openOr(0) must equal(id)
  }

  test("Can get a resource from a json source with extra an field") {
    val id = 2
    val json = """{"id": %s, "extrafield": 1}""".format(id)
    val resp = Echo.find(json)().openOr(Echo.createRecord)
    resp.id.valueBox.openOr(0) must equal(id)
  }

  test("Can get a resource from a json source with a missing field") {
    val id = 2
    val json = """{"extrafield": 1}"""
    val resp = Echo.find(json)().openOr(Echo.createRecord)
    resp.id.valueBox must equal(Full(0))
  }
}
