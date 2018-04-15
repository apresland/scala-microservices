package ch.presland.data.stream

import java.util.Properties

import ch.presland.data.domain.{Sentiment, Tweet}
import edu.stanford.nlp.ling.CoreAnnotations
import edu.stanford.nlp.neural.rnn.RNNCoreAnnotations
import edu.stanford.nlp.pipeline.StanfordCoreNLP
import edu.stanford.nlp.sentiment.SentimentCoreAnnotations
import scala.collection.convert.wrapAll._

object TweetAnalyser {

  val props = new Properties()
  props.setProperty("annotators", "tokenize, ssplit, pos, lemma, parse, sentiment")

  val pipeline = new StanfordCoreNLP(props)

  def sentiment(tweet: Tweet): Sentiment = {

    val sentiments = pipeline
      .process(tweet.content)
      .get(classOf[CoreAnnotations.SentencesAnnotation])
      .map(sentence => (sentence, sentence.get(classOf[SentimentCoreAnnotations.SentimentAnnotatedTree])))
      .map{case(sentence, tree) => (sentence.toString, RNNCoreAnnotations.getPredictedClass(tree))}
      .toList

    val sentiment = sentiments
      .maxBy{case (sentence,_) => sentence.toString.length}
      ._2

    Sentiment(tweet.id, tweet.time, sentiment)
  }
}
