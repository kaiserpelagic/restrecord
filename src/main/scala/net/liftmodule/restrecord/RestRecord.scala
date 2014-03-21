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
import com.ning.http.client.{RequestBuilder, Request}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global


trait RestRecord[MyType <: RestRecord[MyType]] extends JSONRecord[MyType] 
  with RestEndpoint {

  self: MyType =>

  /** 
   *  Refine meta to require a RestMetaRecord 
   */
  def meta: RestMetaRecord[MyType]

  def webservice: WebService = new WebServiceImpl(req)

  def req = {
    val reqNoContext = (config.port.map(:/(config.host, _)) openOr :/(config.host)) 
    (config.context.map(reqNoContext / _) openOr reqNoContext)
  }

  lazy val config: RestRecordConfig = meta.configuration

  def create: Future[Box[JValue]] = meta.create(this)

  def save: Future[Box[JValue]] = meta.save(this, saveEndpoint)

  def save(ids: List[String]): Future[Box[JValue]] = meta.save(this, saveEndpoint(ids: _*))

  def save(ids: List[String], query: (String, String)*): Future[Box[JValue]] = meta.save(this, saveEndpoint(ids: _*), query: _*)
  
  def delete: Future[Box[JValue]] = meta.delete(this, deleteEndpoint)

  def delete(ids: List[String]): Future[Box[JValue]] = meta.delete(this, deleteEndpoint(ids: _*))

  def delete(ids: List[String], query: (String, String)*): Future[Box[JValue]] = meta.delete(this, deleteEndpoint(ids: _*), query: _*)

  def findEndpoint = uri 

  def createEndpoint = uri
  
  def saveEndpoint = uri 

  def deleteEndpoint = uri

  def findEndpoint(ids: String*) = uri(ids: _*)

  def saveEndpoint(ids: String*) = uri(ids: _*)

  def deleteEndpoint(ids: String*) = uri(ids: _*)
}

