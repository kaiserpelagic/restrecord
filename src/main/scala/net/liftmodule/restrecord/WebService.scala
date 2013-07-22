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
import com.ning.http.client.{RequestBuilder}
import com.ning.http.client.oauth._

import scala.xml._

class WebService(val request: RequestBuilder) extends Handlers {

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
  def request: RequestBuilder
}

trait Handlers extends WebRequest {
  /** JSON Handlers */
  
  def find = request.GET > as.lift.Json
  
  def create(body: String) = request.POST.setBody(body) > as.lift.Json 
  
  def save(body: String) = request.PUT.setBody(body) > as.lift.Json

  def delete = request.DELETE > as.lift.Json

  /** XML Handlers */

  def findXML = request > as.xml.Elem 
    
  def saveXML(body: NodeSeq) = { 
    request.PUT.setBody(body.toString) > as.xml.Elem  
  }

  def createXML(body: NodeSeq) = {
    request.POST.setBody(body.toString) > as.xml.Elem
  }
  
  def deleteXML = {
    request.DELETE > as.xml.Elem
  }
  
  /** Form Handlers */
  def createFORM(body: String) = {
    request.POST.setBody(body) > as.xml.Elem
  }

  /* Download a file */
  def download = {
    request.GET > as.Bytes
  }
}
