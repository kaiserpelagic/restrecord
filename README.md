Rest Record
===========

A Lift Record interface for RESTful apis

Uses <a href="http://dispatch.databinder.net/Dispatch.html">Databinder Dispatch's </a><a href="https://github.com/AsyncHttpClient/async-http-client">async-http-client</a> for NIO transacations, so there's no more blocking while waiting on api calls

## Setup and Configuration 

### Configuration
RestRecord can be configured by setting vars on the RestWebService object.

* host: String = "api.twitter.com"
* context: Box[String] =  Full("1.1")
* ssl: Boolean -> if true uses https
* oauth: Boolean -> if true uses oauth

Configuration for Twitter's api v1.1 using oauth

```scala
object Boot.scala {
  etc ...
   
  RestWebService.host = "api.twitter.com"
  RestWebService.context = Full("1.1")
  RestWebService.oauth = true
}
```
To use oauth you'll need to add these properties into the defalt.props file

* oauthRequestToken = my_twitter_oauth_token
* oauthTokenSecret = my_twitter_oauth_token_secret
* oauthConsumerKey = my_twitter_consumer_key
* oauthConsumerSecret = my_twitter_consumer_secret


## Creating a RestRecord

Below is a Twitter search api exmaple.

api.twitter.com/1.1/search/tweets.json

```scala
import net.liftmodules.restrecord._

class Search extends RestRecord[Search] {
  def meta = Search
  
  override val uri = "search" :: "tweets.json" :: Nil

  object text extends OptionalStringField(this, Empty)
}

object Search extends Search with RestMetaRecord[Search] { 
  // allows for flexible parsing of the json
  override def ignoreExtraJSONFields: Boolean = true
  override def needAllJSONFields: Boolean = false
}
```

### Finding a Record (GET)

```scala
  //api.twitter.com/1.1/search/tweets.json?q=lift_framework
  val search: Promise[Box[Search]] = Search.find(("q", "lift framework")) 

  // assert that a promised value be available at any time with the use of apply; this is blocking
  val result: Box[Search] = search()
```

Twitter's retweet api has an id so lets take a look at modeling it

exmaple: api.twitter.com/1.1/statuses/retweets/21947795900469248.json
```scala
import net.liftmodules.restrecord._

class ReTweet extends RestRecordPk[ReTweet] {
  def meta = ReTweet
  
  // Defines what the id is for this endpoint.
  // Twitter requires ".json" appended to the id even though json is the only option.
  // If the resource has a path after the id override the suffix val.
  def idPk: Any = (id.is.toString) + ".json"
  
  override val uri = "statuses" :: "retweets" :: Nil
  
  object id extends IntField(this, 0)
  object retweet_count extends OptionIntField(this, Empty)
}

object ReTweet extends ReTweet with RestMetaRecord[ReTweet] { 
  // allows for flexible parsing of the json
  override def ignoreExtraJSONFields: Boolean = true
  override def needAllJSONFields: Boolean = false
}
```
### Finding a Record (GET)

```scala
  //api.twitter.com/1.1/statuses/retweets/21947795900469248.json
  val retweet: Promise[Box[ReTweet]] = ReTweet.find(21947795900469248.toString + ".json") 
  
  //api.twitter.com/1.1/statuses/retweets/21947795900469248.json?count=5&trim_user=t
  val retweet2: Promise[Box[ReTweet]] = ReTweet.find(21947795900469248.toString + ".json", ("count", "5"), ("trim_user", "t"))
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

RestRecordPk will include the id in the url on save and delete, but not on create since it technically doesn't exist yet.

```scala
val createRes: Promise[Box[JValue]] = MyRest.create
val saveRes = Promise[Box[JValue]] = MyRest.save
val deleteRes = Promise[Box[JValue]] = MyRest.delete
```

### Example Project

An example project using Twitter Search and RestRecord can be found here:

https://github.com/kaiserpelagic/restrecord-example
