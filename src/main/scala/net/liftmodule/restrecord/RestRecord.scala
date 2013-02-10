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
//import dispatch.oauth._
import com.ning.http.client.{RequestBuilder}
import com.ning.http.client.oauth._

object RestRecordConfig {
  var host: String = "localhost"
  var port: Box[Int] = Empty
  var context: Box[String] = Empty
  
  var ssl = false
  var oauth = false
  val requestToken = Props.get("restrecord.oauthRequestToken")
  val tokenSecret =	Props.get("restrecord.oauthTokenSecret")
  val consumerKey = Props.get("restrecord.oauthConsumerKey")
  val consumerSecret = Props.get("restrecord.oauthConsumerSecret") 

  def req = {
    val ctx = host + (context.map("/" + _) openOr "")
    val _req = port.map(:/(ctx, _)) openOr :/(ctx) 
    if (ssl) _req.secure else _req
  }

  def webservice = new WebService(req)
}

trait RestRecord[MyType <: RestRecord[MyType]] extends JSONRecord[MyType] 
  with RestEndpoint with Oauth {

  self: MyType =>
  
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

  // override this if you want to change this record's specific webservice from the default in config
  def webservice: WebService = _discoverWebservice 
  
  def _webservice = Empty

  def _discoverWebservice = _webservice openOr RestRecordConfig.webservice 

  private def _discoverEndpoint = idPk.map(uri(_)) openOr uri
  
  // override this is you something other than the default in config 
  val oauth_? = RestRecordConfig.oauth
}

trait Oauth {
  val consumer = new ConsumerKey(RestRecordConfig.consumerKey.getOrElse(""), RestRecordConfig.consumerSecret.getOrElse(""))
  val token = new RequestToken(RestRecordConfig.requestToken.getOrElse(""), RestRecordConfig.tokenSecret.getOrElse(""))
}

import scala.collection.mutable.ListBuffer

trait RestEndpoint {
  
  final val * = "*"

  /** Defines the RESTful endpoint for this resource -- /foo */
  val uri: List[String]
  
  def uri(id: Any): List[String] = uri(List(id.toString))
  
  def uri(ids: List[Any]): List[String] = {
    val strs = ids.map(_.toString)
    val _ids: ListBuffer[String] = ListBuffer(strs: _*)

    val _uri = uri.foldLeft(ListBuffer[String]())((xs, x) => { 
      val append = if(x.equals(*) && !_ids.isEmpty) _ids.remove(0) else x 
      xs.append(append)
      xs
    })

    _uri.toList
  }
}
