RestRecord
===========

A Lift Record interface for RESTful apis

Uses <a href="http://dispatch.databinder.net/Dispatch.html">Databinder Dispatch's </a><a href="https://github.com/AsyncHttpClient/async-http-client">async-http-client</a> for NIO transacations, so there's no more blocking while waiting on api calls

## Setup and Configuration 

### Getting RestRecord

git clone https://github.com/kaiserpelagic/restrecord.git

### Configuration
RestRecord can be configured by setting vars on the RestRecordConfig object in Boot.scala.

* host: String = "api.twitter.com"
* context: Box[String] =  Full("1.1")
* ssl: Boolean -> if true uses https
* oauth: Boolean -> if true uses oauth

Configuration for Twitter's api v1.1 using oauth

```scala
import net.liftmodules.RestRecord
import net.liftmodules.restrecord.{RestRecordConfig}

object Boot {
  etc ...
   
  RestRecordConfig.host = "api.twitter.com"
  RestRecordConfig.context = Full("1.1")
  RestRecordConfig.oauth = true
  RestRecord.init()
}
```
To use oauth you'll need to add these properties into the props file

* twitter.oauthRequestToken = my_twitter_oauth_token
* twitter.oauthTokenSecret = my_twitter_oauth_token_secret
* twitter.oauthConsumerKey = my_twitter_consumer_key
* twitter.oauthConsumerSecret = my_twitter_consumer_secret


## Creating a RestRecord

Below is an example of using Twitter search api (api.twitter.com/1.1/search/tweets.json?q=lift_framework) with ResetRecord. 

Here is a condensed json response from the search api:
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
  override val uri = "search" :: "tweets.json" :: Nil
  
  object statuses extends JSONSubRecordArrayField(this, Statuses)
}

object Search extends Search with RestMetaRecord[Search] { }

class Statuses extends RestRecord[Statuses] {
  def meta = Statuses

  override val uri = "statuses" :: "show" :: Nil

  // Defines the id in the resource path.
  // This will be used on Save and Deletes if the Box is Full
  // Twitter requires ".json" after the id even though they only respond with json !!!
  override def idPk = Full(id_str.is + ".json")

  object id_str extends StringField(this, "")
  object text extends OptionalStringField(this, Empty)
}

object Statuses extends Statuses with RestMetaRecord[Statuses] {
  // allows for flexible parsing of the json
  override def ignoreExtraJSONFields: Boolean = true
  override def needAllJSONFields: Boolean = false 
}
```
RestRecord uses JSONRecord (which includes JSONSubRecordArrayField used above) from the couchdb lift module. Unfortunately, couchdb imports an older version of Dispatch which conflicts with the newer version used in RestRecord.

My work around for now is to copy JSONRecord into the RestRecord package. Hopefully, in the future JSONRecord will be folded into lift Record.

### Finding a Tweet (GET)

```scala
  // api.twitter.com/1.1/search/tweets.json?q=lift_framework
  val search: Promise[Box[Search]] = Search.find(("q", "lift framework")) 

  // assert that a promised value be available at any time with the use of apply; this is blocking
  val result: Box[Search] = search()

  //api.twitter.com/1.1/statuses/show/21947795900469248.json
  val status: Promise[Box[ReTweet]] = Status.find(21947795900469248.toString + ".json") 
  
  //api.twitter.com/1.1/statuses/show/21947795900469248.json?trim_user=t
  val status2: Promise[Box[ReTweet]] = Status.find(21947795900469248.toString + ".json", ("trim_user", "t"))
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

Creating, saving and deleting use the matching REST verbs and returns a Promise[Box[JValue]].

```scala
val createRes: Promise[Box[JValue]] = MyRest.create  // POST
val saveRes = Promise[Box[JValue]] = MyRest.save     // PUT
val deleteRes = Promise[Box[JValue]] = MyRest.delete // DELETE
```

### Example Project

An example project using Twitter Search and RestRecord can be found here:

https://github.com/kaiserpelagic/restrecord-example
