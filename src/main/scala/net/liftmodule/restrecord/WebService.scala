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
import net.liftweb.json.JsonAST.{JValue, JObject, render}
import net.liftweb.json.{Printer}

import dispatch._
import com.ning.http.client.{RequestBuilder}


object WebService {
  def apply(url: String) = new WebService(host(url))
}

class WebService(request: RequestBuilder) {

  import WebServiceHelpers._
  def apply(path: List[String]) = 
    new WebService(request / buildPath(path))
  
  def apply(path: List[String], params: Map[String, String]) = 
    new WebService(request / buildPath(path) <<? params)

  /** JSON Handlers */

  def find = request.GET OK LiftJson.As
  
  def create(body: JObject) = request.POST.setBody(jobjectToString(body)) OK LiftJson.As 
  
  def save(body: JObject) = request.PUT.setBody(jobjectToString(body)) OK LiftJson.As

  def delete = request.DELETE OK LiftJson.As
  
  /** Convert a JObject into a String */
  private def jobjectToString(in: JObject): String = Printer.compact(render(in))
}


object WebServiceHelpers {
  def buildPath(path: List[String]): String = 
    if (!path.isEmpty)
      path.tail.foldLeft(path.head)(_ + "/" + _)
    else 
      ""
}
