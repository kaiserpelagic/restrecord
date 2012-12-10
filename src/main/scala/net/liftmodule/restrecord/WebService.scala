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
import dispatch.as.lift._
import com.ning.http.client.{RequestBuilder}


object WebService {
  def apply(url: String) = new WebService(host(url))
}

class WebService(request: RequestBuilder) {

  def apply(path: String) = 
    new WebService(request / path)
  
  def apply(params: (String, String)*) = 
    new WebService(request <<? Seq(params: _*))
  
  def apply(path: String, params: (String, String)*) = 
    new WebService(request / path <<? Seq(params: _*))

  def header(head: (String, String)*) = 
    new WebService(request <:< Seq(head: _*))

  /** JSON Handlers */

  def find = request.GET OK Json
  
  def create(body: String) = request.POST.setBody(body) OK Json 
  
  def save(body: String) = request.PUT.setBody(body) OK Json

  def delete = request.DELETE OK Json
}
