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

import dispatch._
import dispatch.oauth._

import com.ning.http.client.{Request}
import com.ning.http.client.oauth._

import net.liftweb.json.JsonAST.{JValue}

import scala.xml._

abstract class WebService extends RequestMakerInterface with RequestHandlerInterface {
  val http = Http 
}

trait RequestMakerInterface {
  def url(path: List[String]): WebService

  def query(params: (String, String)*): WebService

  def head(params: (String, String)*): WebService

  def oauth(consumer: ConsumerKey, token: RequestToken): WebService 
}

trait RequestHandlerInterface {
  /** JSON Handlers */
  def find: (Request, FunctionHandler[JValue])
  def create(body: String): (Request, FunctionHandler[JValue])
  def save(body: String): (Request, FunctionHandler[JValue])
  def delete: (Request, FunctionHandler[JValue])

  /** XML Handlers */
  def findXML: (Request, FunctionHandler[NodeSeq])
  def createXML(body: NodeSeq): (Request, FunctionHandler[NodeSeq])
  def saveXML(body: NodeSeq): (Request, FunctionHandler[NodeSeq])
  def deleteXML: (Request, FunctionHandler[NodeSeq])
   
  /** Form Handlers */
  def createFORM(body: String): (Request, FunctionHandler[NodeSeq]) 

  /* Download a file */
  def download: (Request, FunctionHandler[Array[Byte]]) 
}

class WebServiceImpl(val request: Req) extends WebService with RequestHandler with RequestMaker

trait RequestMaker extends RequestMakerInterface with WebRequest {
  def url(path: List[String]) = 
    path.foldLeft(request)((request, part) => request / part)

  def query(params: (String, String)*) = 
   request <<? Seq(params: _*)
  
  def head(head: (String, String)*) = 
    request <:< Seq(head: _*)

  def oauth(consumer: ConsumerKey, token: RequestToken) = 
    new SigningVerbs(request) <@(consumer, token)
}

trait WebRequest {
  def request: Req
}

trait RequestHandler extends WebRequest with RequestHandlerInterface {

  def find = request.GET > as.lift.Json
  
  def create(body: String) = request.POST.setBody(body) > as.lift.Json 
  
  def save(body: String) = request.PUT.setBody(body) > as.lift.Json

  def delete = request.DELETE > as.lift.Json


  def findXML = request > as.xml.Elem 
    
  def saveXML(body: NodeSeq) = 
    request.PUT.setBody(body.toString) > as.xml.Elem  

  def createXML(body: NodeSeq) =
    request.POST.setBody(body.toString) > as.xml.Elem
  
  def deleteXML = request.DELETE > as.xml.Elem
  
  def createFORM(body: String) = request.POST.setBody(body) > as.xml.Elem

  def download = request.GET > as.Bytes
}
