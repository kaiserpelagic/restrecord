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

import net.liftweb.util._
import net.liftweb.common._
import Helpers._
import net.liftweb.json.JsonAST._
import net.liftweb.json.{Printer}
import net.liftweb.record.{MetaRecord, Record}

import dispatch._
import com.ning.http.client.{RequestBuilder, Request}


trait RestMetaRecord[BaseRecord <: RestRecord[BaseRecord]] 
  extends JSONMetaRecord[BaseRecord] {

  self: BaseRecord =>
  
  val http = Http 

  def find(query: (String, String)*): Promise[Box[BaseRecord]] =
    findFrom(webservice, buildUri, query: _*)

  def find(id: Any, query: (String, String)*): Promise[Box[BaseRecord]] = 
    findFrom(webservice, buildUri(id), query: _*)

  def findFrom(svc: WebService, path: List[String], 
    query: (String, String)*): Promise[Box[BaseRecord]] = {
   
   withHttp(http, svc(path, query: _*) find, fromJValue)
  }
  
  def create(inst: BaseRecord): Promise[Box[JValue]] = {
    createFrom(inst, inst.webservice)
  }

  def createFrom[T](inst: BaseRecord, svc: WebService): Promise[Box[JValue]] = { 
    foreachCallback(inst, _.beforeCreate)
    try {
      withHttp(http, svc(inst.createEndpoint) create(inst.asJValue), fullIdent)
    } finally {
      foreachCallback(inst, _.afterCreate)
    }
  }
  
  def save(inst: BaseRecord): Promise[Box[JValue]] = 
    saveFrom(inst, inst.webservice)

  def saveFrom(inst: BaseRecord, svc: WebService): Promise[Box[JValue]] = {
    foreachCallback(inst, _.beforeSave)
    try {
      withHttp(http, svc(inst.saveEndpoint) save(inst.asJValue), fullIdent)
    } finally {
      foreachCallback(inst, _.afterSave)
    }
  }
  
  def delete(inst: BaseRecord): Promise[Box[JValue]] = 
    deleteFrom(inst, inst.webservice)

  def deleteFrom(inst: BaseRecord, svc: WebService): Promise[Box[JValue]] = {
    foreachCallback(inst, _.beforeDelete)
    try { 
      withHttp(http, svc(inst.deleteEndpoint) delete, fullIdent)
    } finally {
      foreachCallback(inst, _.afterDelete)
    }
  }

  def fullIdent(jv: JValue) = Full(jv)

  def withHttp[T](h: Http, body: (Request, OkFunctionHandler[JValue]), 
    handle: JValue => Box[T]): Promise[Box[T]] = {
    
    h(body).either map {
      case Right(v) => handle(v)
      case Left(e) => Failure("error", Full(e), Empty)
    }
  }

  /**
   *  Transforms a list of strings into a url path
   *  List("foo", "bar" "baz") becomes "foo/bar/baz"
   */
  implicit def listToUrlPath(path: List[String]): String = { 
    path.tail.foldLeft(path.headOption getOrElse "")(_ + "/" + _)
  }

  /** 
   *  Transforms a JObject into a String 
   */
  implicit def jobjectToString(in: JObject): String = Printer.compact(render(in))
}
