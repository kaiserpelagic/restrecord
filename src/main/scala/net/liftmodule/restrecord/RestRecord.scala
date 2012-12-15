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

import dispatch.{Promise}
import com.ning.http.client.{RequestBuilder}

object RestWebService {
  var url = "localhost"
  def webservice = WebService(url)
}

trait RestRecordPk[MyType <: RestRecordPk[MyType]] extends RestRecord[MyType] {
  self: MyType =>
  
  def meta: RestMetaRecordPk[MyType]

  def idPK: Any

  /** 
   *  Defines the RESTful suffix after id 
   *  Example: /uri/:id/uriSuffix
   */
  val uriSuffix: List[String] = Nil
  
  def buildUri(id: Any): List[String] = uri ::: List(id.toString) ::: uriSuffix
  
  override def findEndpoint(id: Any) = buildUri(id)
  
  override def saveEndpoint = buildUri(idPK)

  override def deleteEndpoint = buildUri(idPK)
}

trait RestRecord[MyType <: RestRecord[MyType]] extends JSONRecord[MyType] {

  self: MyType =>
  
  /** 
   *  Refine meta to require a RestMetaRecord 
   */
  def meta: RestMetaRecord[MyType]

  /** 
   *  Defines the RESTful endpoint for this resource 
   *  Examples: /foo or /foo/bar 
   */
  val uri: List[String]
  
  def buildUri: List[String] = uri
  
  def create: Promise[Box[JValue]] = meta.create(this)

  def save: Promise[Box[JValue]] = meta.save(this)
  
  def delete: Promise[Box[JValue]] = meta.delete(this)
  
  def findEndpoint = buildUri

  def createEndpoint = buildUri

  def saveEndpoint = buildUri

  def deleteEndpoint = buildUri

  /** override this method to handle api specific POST / PUT / DELETE responses **/
  def handleResponse(jv: JValue): Box[JValue] = Full(jv) 
  
  // override this if you want to change this record's specific webservice
  def myWebservice = Empty

  def _discoverWebservice = myWebservice openOr RestWebService.webservice

  def webservice = _discoverWebservice 
}
