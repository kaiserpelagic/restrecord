Rest Record
===========

A Lift Record interface for RESTful apis

Uses <a href="http://dispatch.databinder.net/Dispatch.html">Databinder Dispatch's </a><a href="https://github.com/AsyncHttpClient/async-http-client">async-http-client</a> for NIO transacations, so there's no more blocking while waiting on api calls

## Setup and Configuration 

### Configuration
By default the api host is "localhost" and context is Empty. 
You can change this in Boot.scala by setting setting the host and context vars in RestWebSerice. 

```scala
object Boot.scala {
  etc ...
   
  RestWebService.host = "api.foursquare.com"
  RestWebService.context = Full("v2")
}
```
In this example I'm using Foursquare's venue api. The host can be overriden later if you need it to be different for a specific Record.

## Creating a RestRecord

```scala
import net.liftmodules.restrecord._

class Venue extends RestRecord[Venue] {
  def meta = Venue

  // api.foursquare.com/v2/venues
  override val uri = "venues" :: Nil

  // this is used on saves and deletes to contstruct the url
  // if the record needs an id
  // api.foursquare.com/v2/venues/:id
  override def idPK = id.valueBox 

  // handleResponse is called with the json response from a PUT / POST / DELETE action
  // by default it returns an Empty box
  // let's return whenever the api response is
  override handleResponse[T](json: JValue) = Full(json)

  object id extends OptionalStringField(this, Empty)
  object name extends OptionalStringField(this, Empty)
}

object Venue extends Venue with RestMetaRecord[Venue] {
  // we aren't going to serialize the entire Foursquare response 
  // so we want to be flexible about parsing
  override def ignoreExtraJSONFields: Boolean = true
  override def needAllJSONFields: Boolean = false 
}
```

### Finding a Record (GET)

```scala
  val ven1: Promise[Box[Venue]] = Venue.find(3) //api.foursquare.com/v2/venues/3
  val ven2: Promise[Box[Venue]] = Venue.find(3, ("foo", "bar")) //api.foursquare.com/v2/venues/3?foo=bar 
  val ven3: Promise[Box[Venue]] = Venue.find(("foo", "bar"), ("baz, laraz")) //api.foursquare.com/v2/venues?foo=bar&baz=laraz

  // assert that a promised value be available at any time with the use of apply
  // this is blocking
  val ven: Box[Venue] = ven1()
```
HTTP failures are captured in the Box as a Failure. The caller is responsible for handling them 

#### Venue Example
```scala
class Foursquare {
  val venue: Promise[Box[Venue]] = Venue.find(3)
  ... some time consuming stuff
  
  def render = {
   val ven: Venue = venue()
   "@name" #> Text(ven.map(_.name.valueBox openOr "") openOr "")
  }
}
```

### POST, PUT, DELETE

<a href="http://en.wikipedia.org/wiki/Dive_bar">Let's add the Merrimaker to Venues</a>

*I don't think Foursquare will let us but we can pretend.*

The response from each action contains either the json response or a 
Failure, i.e. networking exception, json parsing expcetion, ect.

```scala
val merrimaker: MyRecord = Venue.createRecord.name("Merrimaker")
val createRes: Promise[Box[JValue]] = record.create
val saveRes: Promise[Box[JValue]] = merrimaker.save
val deleteRes: Promise[Box[JValue]] = merrimaker.delete
```