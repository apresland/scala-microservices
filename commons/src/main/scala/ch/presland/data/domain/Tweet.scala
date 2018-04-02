package ch.presland.data.domain

import java.util.Date

case class Tweet(id: String, time: Date, user: String, content: String)
case class Sentiments( length: Int, time: Iterable[String], hostile: Iterable[Double], negative:Iterable[Double], neutral:Iterable[Double], positive:Iterable[Double])

case class Hashtags( length: Int, dimension: Int,
                     zero: Iterable[String], one:Iterable[String], two:Iterable[String], three:Iterable[String], four:Iterable[String],
                     five: Iterable[Double], six:Iterable[Double], seven:Iterable[Double], eight:Iterable[Double], nine:Iterable[Double])

case class Tweets(length: Int, time: Iterable[String], content: Iterable[String])