package ch.presland.data.domain

import java.util.Date

case class Tweet(id: String, time: Date, user: String, content: String)
case class Sentiments( length: Int, dimension: Int, zero: Iterable[Double], one:Iterable[Double], two:Iterable[Double], three:Iterable[Double], four:Iterable[Double])

case class Hashtags( length: Int, dimension: Int,
                     zero: Iterable[Double], one:Iterable[Double], two:Iterable[Double], three:Iterable[Double], four:Iterable[Double],
                     five: Iterable[Double], six:Iterable[Double], seven:Iterable[Double], eight:Iterable[Double], nine:Iterable[Double]
                   )
