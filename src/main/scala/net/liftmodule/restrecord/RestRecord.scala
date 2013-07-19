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

import net.liftweb.common._
import net.liftweb.json.JsonAST._
import net.liftweb.record.{MetaRecord, Record}
import net.liftweb.util.Props 

import dispatch._
import com.ning.http.client.{RequestBuilder}
import com.ning.http.client.oauth._

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

trait RestRecord[MyType <: RestRecord[MyType]] extends JSONRecord[MyType] 
  with RestEndpoint {

  self: MyType =>
  
  def config: RestRecordConfig

  /** 
   *  Refine meta to require a RestMetaRecord 
   */
  def meta: RestMetaRecord[MyType]

  /** 
   *  Defines the RESTful id for this resource
   *  Empty implies this endoint does not use and id
   *  Used on Saves and Deletes 
   */
  def idPk: Box[Any] = Empty
 
  def create: Promise[Box[JValue]] = meta.create(this)

  def save: Promise[Box[JValue]] = meta.save(this)
  
  def delete: Promise[Box[JValue]] = meta.delete(this)

  def findEndpoint(id: Any) = uri(id)
  
  def findEndpoint = uri 

  def createEndpoint = uri // we should never have an id on creation
  
  def saveEndpoint = _discoverEndpoint

  def deleteEndpoint = _discoverEndpoint

  def webservice: WebService = { 
    val reqNoContext = (config.port.map(:/(config.host, _)) openOr :/(config.host)) 
    val req = (config.context.map(reqNoContext / _) openOr reqNoContext)
    new WebService(req) 
  }

  private def _discoverEndpoint = idPk.map(uri(_)) openOr uri
}
