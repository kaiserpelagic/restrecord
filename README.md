RestRecord
===========

A Lift Record interface for RESTful apis

Uses <a href="http://dispatch.databinder.net/Dispatch.html">Databinder Dispatch's </a><a href="https://github.com/AsyncHttpClient/async-http-client">async-http-client</a> for NIO transacations, so there's no more blocking while waiting on api calls

## Setup and Configuration 

### Integrating into your project

Add the following to resolvers

```
resolvers ++= Seq(
  "snapshots" at "http://oss.sonatype.org/content/repositories/snapshots",
  "releases" at "http://oss.sonatype.org/content/repositories/releases"
)
```

In build.sbt add this to libraryDependencies

```
liftVersion = 2.5

scalaVersion =  2.10.1

"net.liftmodules" %% "restrecord" % (liftVersion + "-1.5-SNAPSHOT")

for 2.9.2 use liftVersion + "-1.3-SNAPSHOT"
```

### Configuration
RestRecord can be configured through the RestRecordConfig class

Configuration for Twitter's api v1.1 using oauth

```scala
// defaults
case class RestRecordConfig(
  host: String = "localhost", 
  port: Box[Int] = Empty, 
  context: Box[String] = Empty, 
  ssl: Boolean = false,
  oauth: Boolean = false,
  consumer: Box[ConsumerKey] = Empty,
  token: Box[RequestToken] = Empty
)

// import if you're using oauth
import com.ning.http.client.oauth._

// mix this into a RestMetaRecord object to configure the resource
trait TwitterConfig {

  val consumerKey = new ConsumerKey(key, secret)
  val token = new RequestToken(key, secret)

  val configuration = RestRecordConfig(
    "api.twitter.com",
    Empty,
    Full("1.1"),
    true, 
    true,
    Full(consumerKey),
    Full(token)
  )
}
```

## Creating a RestRecord

Below is an example of using Twitter's search api with ResetRecord. 

example GET request api.twitter.com/1.1/search/tweets.json?q=lift_framework

Here is a condensed json response:
```json
{
  "statuses": [
    {
      "coordinates": null,
      "favorited": false,
      "truncated": false,
      "created_at": "Mon Sep 24 03:35:21 +0000 2012",
      "id_str": "250075927172759552",
      "text": "foobarbaz"
    }
  ]
}   
```

The Search api uses the same json as the Status api. I've modeled that one as well becuse the Status api uses an id in the api path.

```scala
class Search extends RestRecord[Search] {
  def meta = Search

  // defines the search resource endpoint
  val uri = "search" :: "tweets.json" :: Nil
  
  object statuses extends JSONSubRecordArrayField(this, Statuses)
}

object Search extends Search with RestMetaRecord[Search] with TwitterConfig

class Statuses extends RestRecord[Statuses] {
  def meta = Statuses

  val uri = "statuses" :: "show" :: * :: Nil

  // Defines the id in the resource path.
  // This will be used on Save and Deletes if the Box is Full
  // Twitter requires ".json" after the id even though they only respond with json !!!
  override def idPk = Full(id_str.is + ".json")

  object id_str extends StringField(this, "")
  object text extends OptionalStringField(this, Empty)
}

object Statuses extends Statuses with RestMetaRecord[Statuses] with TwitterConfig
```
RestRecord uses JSONRecord (which includes JSONSubRecordArrayField used above) from the couchdb lift module. Unfortunately, couchdb imports an older version of Dispatch which conflicts with the newer version used in RestRecord.

My work around for now is to copy JSONRecord into the RestRecord package. Hopefully, in the future JSONRecord will be folded into lift Record.

### Finding a Tweet (GET)

```scala
  // brings implicits into scopt for Future -> EnrichedFuture
  import dispatch._ 

  // api.twitter.com/1.1/search/tweets.json?q=lift_framework
  val search: Future[Box[Search]] = Search.find(("q", "lift framework")) 

  // assert that an EnrichedFuture value be available at any time with the use of apply; this is blocking
  val result: Box[Search] = search()

```
### Finding a Status (GET)

```scala
  //api.twitter.com/1.1/statuses/show/21947795900469248.json
  val status: Future[Box[Status]] = Status.find(21947795900469248.toString + ".json") 
  
  //api.twitter.com/1.1/statuses/show/21947795900469248.json?trim_user=t
  val status2: Future[Box[Status]] = Status.find(21947795900469248.toString + ".json", ("trim_user", "t"))
```

HTTP failures are captured in the Box as a Failure. The caller is responsible for handling them 

#### Twitter Search Snippet Example
```scala
object Twitter {

  def search = Search.find(("q", "lift framework"))

  def render: CssSel = {
    val s: Search = search
    
    // make other api calls (they will be async) or do other expensive things
    
    val result = s() openOr Search.createRecord   // blocking
    "li *" #> result.statuses.is.map(t => "@text *" #> Text(t.text.valueBox openOr ""))
  }
}
```

### POST, PUT, DELETE

Creating, saving and deleting use the matching REST verbs and returns a Future[Box[JValue]].

```scala
val record = MyRecord.createRecord.id(2)
val createRes: Future[Box[JValue]] = record.create  // POST
val saveRes: Future[Box[JValue]] = record.save     // PUT
val deleteRes: Future[Box[JValue]] = record.delete // DELETE
```

### Example Project

An example project using Twitter Search and RestRecord can be found here:

https://github.com/kaiserpelagic/restrecord-example
