/*
* Copyright 2010-2011 WorldWide Conferencing, LLC
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at svc.http://www.apache.org/licenses/LICENSE-2.0
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package net.liftmodules
package restrecord

import net.liftweb.util._
import net.liftweb.common._
import Helpers._
import net.liftweb.json.JsonAST._
import net.liftweb.record.{MetaRecord, Record}

import dispatch._
import com.ning.http.client.{RequestBuilder, Request}
import com.ning.http.client.oauth._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

case class RestRecordConfig(
  host: String = "localhost", 
  port: Box[Int] = Empty, 
  context: Box[String] = Empty, 
  ssl: Boolean = false,
  oauth: Boolean = false,
  consumer: Box[ConsumerKey] = Empty,
  token: Box[RequestToken] = Empty
) {
  def getConsumer = consumer openOr new ConsumerKey("", "")
  def getToken = token openOr new RequestToken("", "")
}

trait RestMetaRecord[BaseRecord <: RestRecord[BaseRecord]] 
  extends JSONMetaRecord[BaseRecord] {

  self: BaseRecord =>
  
  val configuration: RestRecordConfig

  def find(query: (String, String)*): Future[Box[BaseRecord]] = { 
    findFrom(webservice, findEndpoint, query: _*)
  }

  def find(id: Int, query: (String, String)*): Future[Box[BaseRecord]] = {
    find(List(id.toString), query: _*)
  }
  
  def find(id: String, query: (String, String)*): Future[Box[BaseRecord]] = {
    find(List(id), query: _*)
  }
  
  def find(ids: List[String], query: (String, String)*): Future[Box[BaseRecord]] = {
    val rec = findFrom(webservice, findEndpoint(ids: _*), query: _*)
    injectResourceIds(rec, ids)
    rec
  }
  
  def findFrom(svc: WebService, path: List[String], 
    query: (String, String)*): Future[Box[BaseRecord]] = {
    withHttp(svc.http, oauth(svc url(path) query(query: _*)) find, fromJValue)
  }

  def create(inst: BaseRecord, path: List[String], query: (String, String)*) = 
    createFrom(inst, inst.webservice, path, query: _*)

  def createFrom(inst: BaseRecord, svc: WebService, path: List[String],
    query: (String, String)*): Future[Box[JValue]] = {
    withHttp(svc.http, oauth(svc url(path) query(query: _*)) create(inst.asJValue), fullJV)
  }

  def save(inst: BaseRecord, path: List[String], query: (String, String)*) = { 
    saveFrom(inst, inst.webservice, path, query: _*)
  }

  def saveFrom(inst: BaseRecord, svc: WebService, path: List[String],
    query: (String, String)*): Future[Box[JValue]] = {
    withHttp(svc.http, oauth(svc url(path) query(query: _*)) save(inst.asJValue), fullJV)
  }

  def delete(inst: BaseRecord, path: List[String], query: (String, String)*) = {
    deleteFrom(inst, inst.webservice, path, query: _*)
  }

  def deleteFrom(inst: BaseRecord, svc: WebService, path: List[String],
    query: (String, String)*): Future[Box[JValue]] = {
    withHttp(svc.http, oauth(svc url(path) query(query: _*)) delete, fullJV)
  }

  def withHttp[T](h: Http, body: (Request, FunctionHandler[JValue]), 
    handle: JValue => Box[T]): Future[Box[T]] = {
    HttpHelper.execute(h, body, handle) 
  }

  def oauth(svc: WebService): WebService = { 
    if (config.oauth) svc oauth(config.getConsumer, config.getToken) else svc
  }

  private def fullJV(jv: JValue) = Full(jv)

  private def injectResourceIds(rec: Future[Box[BaseRecord]], ids: List[String]) = {
    rec.foreach(_.foreach(_.resourceIds = ids))
  }
}
