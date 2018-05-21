package ch.presland.data.domain

import java.util.Date

case class User(id: Int, name: String, screen_name: String, lang: String, followers: Int)
case class Tweet(id: String, date: Date, user: String, content: String)
case class Sentiment(id: String, date: Date, score: Int)
case class Tweets(length: Int, date: Iterable[String], content: Iterable[String])
case class Hashtags(hashtags: String)
case class Statistics(id: Iterable[String], date: Iterable[String], user: Iterable[String])
case class Sentiments( length: Int, date: Iterable[String], hostile: Iterable[Double], negative:Iterable[Double], neutral:Iterable[Double], positive:Iterable[Double])