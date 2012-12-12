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
  val host = "localhost"
  val context = Empty
  def req = host + (context.map("/" + context) openOr "")
  def webservice = WebService(req)
}

case class URI(uri: List[String], suffix: List[String], id: Box[Any])

trait RestRecord[MyType <: RestRecord[MyType]] extends JSONRecord[MyType] {

  self: MyType =>
  
  /** Refine meta to require a RestMetaRecord */
  def meta: RestMetaRecord[MyType]

  /** 
   *  Defines the RESTful endpoint for this resource 
   *  Examples: /foo or /foo/bar 
   */
  val uri: List[String]
  
  /** 
   *  Defines the RESTful suffix for endpoint. 
   *  Use when the resource is defined by and id.
   *  Example: /uri/:id/suffix
   */
  val uriSuffix: List[String] = Nil

  /** 
   *  Defines the identifier for this resource, if it has one 
   */
  def idPK: Box[Any] = Empty

  def buildUri: List[String] = buildUri(URI(uri, uriSuffix, Empty))

  def buildUri(id: Any): List[String] = buildUri(URI(uri, uriSuffix, Full(id)))

  def buildUri(u: URI): List[String] = u match {
    case URI(uri: List[String], Nil, Empty) => uri 
    case URI(uri: List[String], suffix: List[String], Empty) => uri ::: suffix 
    case URI(uri: List[String], suffix: List[String], Full(id)) => uri ::: List(id.toString) ::: suffix 
    case _ => uri 
  }
  
  def create[T]: Promise[Box[JValue]] = meta.create(this)

  def save[T]: Promise[Box[JValue]] = meta.save(this)
  
  def delete[T]: Promise[Box[JValue]] = meta.delete(this)
  
  def createEndpoint = buildUri(URI(uri, uriSuffix, Empty))

  def saveEndpoint = buildUri(URI(uri, uriSuffix, idPK))

  def deleteEndpoint = buildUri(URI(uri, uriSuffix, idPK))

  // override this if you want to change this record's specific webservice
  def myWebservice = Empty

  def _discoverWebservice = myWebservice openOr RestWebService.webservice

  def webservice = _discoverWebservice 
}
