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

  def find(query: (String, String)*): Future[Box[BaseRecord]] = 
    findFrom(webservice, findEndpoint, query: _*)
  
  def find(id: String, query: (String, String)*): Future[Box[BaseRecord]] =
    findFrom(webservice, findEndpoint(id), query: _*)
  
  def find(id: Int, query: (String, String)*): Future[Box[BaseRecord]] =
    findFrom(webservice, findEndpoint(id), query: _*)
  
  def findFrom(svc: WebService, path: List[String], 
    query: (String, String)*): Future[Box[BaseRecord]] = {
    withHttp(svc.http, oauth(svc url(path) query(query: _*)) find, fromJValue)
  }

  def create(inst: BaseRecord): Future[Box[JValue]] = 
    createFrom(inst, inst.webservice)

  def createFrom(inst: BaseRecord, svc: WebService): Future[Box[JValue]] = { 
    foreachCallback(inst, _.beforeCreate)
    try {
      withHttp(svc.http, oauth(svc url(inst.createEndpoint)) create(inst.asJValue), fullIdent)
    } finally {
      foreachCallback(inst, _.afterCreate)
    }
  }

  def save(inst: BaseRecord, path: List[String]) = 
    saveFrom(inst, inst.webservice, path)

  def saveFrom(inst: BaseRecord, svc: WebService, path: List[String]): Future[Box[JValue]] = {
    foreachCallback(inst, _.beforeSave)
    try {
      withHttp(svc.http, oauth(svc url(path)) save(inst.asJValue), fullIdent)
    } finally {
      foreachCallback(inst, _.afterSave)
    }
  }

  def delete(inst: BaseRecord, path: List[String]) =
    deleteFrom(inst, inst.webservice, path)

  def deleteFrom(inst: BaseRecord, svc: WebService, path: List[String]): Future[Box[JValue]] = {
    foreachCallback(inst, _.beforeDelete)
    try { 
      withHttp(svc.http, oauth(svc url(path)) delete, fullIdent)
    } finally {
      foreachCallback(inst, _.afterDelete)
    }
  }

  def fullIdent(jv: JValue) = Full(jv)

  def withHttp[T](h: Http, body: (Request, FunctionHandler[JValue]), 
    handle: JValue => Box[T]): Future[Box[T]] = {
    HttpHelper.execute(h, body, handle) 
  }

  def oauth(svc: WebService): WebService = 
    if (config.oauth) svc oauth(config.getConsumer, config.getToken) else svc
}
