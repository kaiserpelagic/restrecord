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

import dispatch.{host, Promise}
import com.ning.http.client.{RequestBuilder}

object RestWebService {
  var url = "localhost"
  
  def req: RequestBuilder = host(url)
  def defaultWebService = new WebService(req)
}

trait RestRecord[MyType <: RestRecord[MyType]] extends JSONRecord[MyType] {

  self: MyType =>
  
  /** Refine meta to require a RestMetaRecord */
  def meta: RestMetaRecord[MyType]

  /** Defines the RESTful endpoint for this resource -- /foo */
  val uri: List[String]
  
  /** Defines the RESTful suffix for endpoint -- /foo/:id/bar */
  val uriSuffix: List[String] = Nil

  /** Defines and uri identifier for this resource -- /foo/:id or /foo/:id/bar */
  def idPK: Box[String] = Empty

  def buildUri: List[String] = uri ::: uriSuffix 
  
  def buildUri(id: String): List[String] = uri ::: List(id) ::: uriSuffix 

  def buildUri(box: Box[String]): List[String] = box.map(buildUri(_)) openOr buildUri 

  def create[T]: Promise[Box[T]] = meta.create(this)

  def save[T]: Promise[Box[T]] = meta.save(this)
  
  def delete[T]: Promise[Box[T]] = meta.delete(this)
  
  def createEndpoint = buildUri

  def saveEndpoint = buildUri(idPK)

  def deleteEndpoint = buildUri(idPK)

  /** override this method to handle api specific POST / PUT / DELETE responses **/
  def handleResponse[T](json: JValue): Box[T] = Empty 
  
  // override this if you want to change this record's specific webservice
  def myWebservice = Empty

  def _discoverWebservice = myWebservice openOr RestWebService.defaultWebService 

  def webservice = _discoverWebservice 
}
