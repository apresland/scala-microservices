package ch.presland.data.stream

import akka.actor.ActorSystem
import akka.actor.ActorRef
import ch.presland.data.domain.Tweet
import twitter4j._

class TwitterClient(actorRef: ActorRef) {

  private val CONSUMER_KEY = "iQlMVwSIi1jZeIEY19RdVkqzb"
  private val CONSUMER_SECRET = "vtBZ8i3v8zHJgANME8XcMrLlHeI8T4xmKyOAbI7vbxCyawJI7a"
  private val ACCESS_KEY = "225386473-bLWwDXraZrrGrFZA6Xslijzvt0GHayTRdJVUfMMx"
  private val ACCESS_SECRET = "hxJWX0WWnAjvGRPhLE6yINbPm4OgyaEEoLpxMsMmFevFB"

  private val config = new twitter4j.conf.ConfigurationBuilder()
    .setLoggerImpl("twitter4j.NullLoggerFactory")
    .setDebugEnabled(false)
    .setUseSSL(true)
    .setOAuthConsumerKey(CONSUMER_KEY)
    .setOAuthConsumerSecret(CONSUMER_SECRET)
    .setOAuthAccessToken(ACCESS_KEY)
    .setOAuthAccessTokenSecret(ACCESS_SECRET)
    .build

  private val twitterStream = new TwitterStreamFactory(config).getInstance

  private def statusListener = new StatusListener() {
    def onStatus(status: Status) {
      actorRef ! Tweet(status.getUser.getScreenName, status.getId.toString, status.getText)
    }
    def onDeletionNotice(statusDeletionNotice: StatusDeletionNotice) {}
    def onTrackLimitationNotice(numberOfLimitedStatuses: Int) {}
    def onException(ex: Exception) { ex.printStackTrace() }
    def onScrubGeo(arg0: Long, arg1: Long) {}
    def onStallWarning(warning: StallWarning) {}
  }

  def start(): Unit = {
    val query: FilterQuery = new FilterQuery()
    twitterStream.addListener(statusListener)
    query.track(Array("brexit", "Brexit", "BREXIT"))
    twitterStream.filter(query)
  }

  def stop(): Unit = {
    twitterStream.cleanUp()
    twitterStream.shutdown()
  }
}
