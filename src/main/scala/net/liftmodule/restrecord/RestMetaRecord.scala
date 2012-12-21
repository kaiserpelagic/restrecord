/*
* Copyright 2010-2011 WorldWide Conferencing, LLC
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
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
import net.liftweb.json.{Printer}
import net.liftweb.record.{MetaRecord, Record}

import dispatch._
import com.ning.http.client.{RequestBuilder, Request}
import com.ning.http.client.oauth._

trait RestMetaRecord[BaseRecord <: RestRecord[BaseRecord]] 
  extends JSONMetaRecord[BaseRecord] with Oauth {

  self: BaseRecord =>
  
  val http = Http 

  def find(query: (String, String)*): Promise[Box[BaseRecord]] = 
    findFrom(webservice, findEndpoint, query: _*)
  
  def find(id: String, query: (String, String)*): Promise[Box[BaseRecord]] =
    findFrom(webservice, findEndpoint(id), query: _*)
  
  def find(id: Int, query: (String, String)*): Promise[Box[BaseRecord]] =
    findFrom(webservice, findEndpoint(id), query: _*)
  
  def findFrom(svc: WebService, path: List[String], 
    query: (String, String)*): Promise[Box[BaseRecord]] = {

    withHttp(http, oauth(svc url(path) query(query: _*)) find, fromJValue)
  }

  def oauth(svc: WebService): WebService = 
    if (oauth_?) svc oauth(consumer, token) else svc

  def create(inst: BaseRecord): Promise[Box[JValue]] = 
    createFrom(inst, inst.webservice)

  def createFrom(inst: BaseRecord, svc: WebService): Promise[Box[JValue]] = { 
    foreachCallback(inst, _.beforeCreate)
    try {
      withHttp(http, oauth(svc url(inst.createEndpoint)) create(inst.asJValue), fullIdent)
    } finally {
      foreachCallback(inst, _.afterCreate)
    }
  }

  def save(inst: BaseRecord): Promise[Box[JValue]] = 
    saveFrom(inst, inst.webservice)

  def saveFrom(inst: BaseRecord, svc: WebService): Promise[Box[JValue]] = {
    foreachCallback(inst, _.beforeSave)
    try {
      withHttp(http, oauth(svc url(inst.saveEndpoint)) save(inst.asJValue), fullIdent)
    } finally {
      foreachCallback(inst, _.afterSave)
    }
  }

  def delete(inst: BaseRecord): Promise[Box[JValue]] = 
    deleteFrom(inst, inst.webservice)

  def deleteFrom(inst: BaseRecord, svc: WebService): Promise[Box[JValue]] = {
    foreachCallback(inst, _.beforeDelete)
    try { 
      withHttp(http, oauth(svc url(inst.deleteEndpoint)) delete, fullIdent)
    } finally {
      foreachCallback(inst, _.afterDelete)
    }
  }

  def fullIdent(jv: JValue) = Full(jv)

  def withHttp[T](h: Http, body: (Request, OkFunctionHandler[JValue]), 
    handle: JValue => Box[T]): Promise[Box[T]] = {
   
    h(body).either map {
      case Right(v) => handle(v)
      case Left(e) => Failure("error", Full(e), Empty)
    }
  }

  /** 
   *  Transforms a JObject into a String 
   */
  implicit def jobjectToString(in: JObject): String = Printer.compact(render(in))

  implicit def implyRequestBuilderToWebService(builder: RequestBuilder): WebService =
    new WebService(builder)
}

trait Oauth {
  import RestRecordConfig._
  
  val oauth_? = oauth
  val consumer = new ConsumerKey(consumerKey.getOrElse(""), consumerSecret.getOrElse(""))
  val token = new RequestToken(requestToken.getOrElse(""), tokenSecret.getOrElse(""))
}
