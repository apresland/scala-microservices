# Reactive Tweetstream analysis

A demonstration showing how to use Spark, Akka, Cassandra and Kafka for realtime tweet sentiment analyis and for trending hashtag identification in Scala.

The SMACK stack (Spark, Mesos, Akka, Cassandra and Kafka) are commonly used to build realtime analysis pipelines in production. Since Mesos is responsible for scaling the system and not of interest here. From the others Spark, Akka and Kafka focus on reacting to the data streaming into the application with Cassandra as data store. Although SMACK is the cannonical Big-Data solution interesting alternative technologies are Elasticsearch, Logstash and for this reason alone will also be used.

# Stack Overview

* Docker
Docker Compose was used to containerise and manage the Cassandra, Kafka, Elasticsearch and Logstash services.

* Spark
Spark is an open-source cluster computing framework which supports batch-processing and stream-processing (micro-batch) with a highly performant Scala API.

* Akka
Akka is a framework that allows the construction of highly distributed reactive applications using Actors and Streams. Combined with Scala as the development language many useful aspects of functional programming can leveraged.

* Cassandra
Cassandra is a column-oriented databank that is distributed, linearly scaleable. Cassandras integrates seamlessly with Spark so that distributed operations can be executed local to the data. This data-locality means that IO operations are minimized and the CPUs only process data found locally on disk. 

* Kafka
Kafka is a distributed horizontally scaleable and fault-tolerant Message Broker used for building realtime data pipelines and streaming applications with large data volumes. Because Kafka partitions data and saves it in an Append-Only-Log it can handle Terabytes of data without impact on performance.

* Elasticsearch
Elasticsearch is a distributed search and analytics engine which can be used to perform queries during explorative analysis.

* Logstash
Logstash is an open source, server-side data processing pipeline that ingests data from a multitude of sources simultaneously, transforms it, and then sends it to a “stash” (Elasticsearch).

# Architecture
The platform is composed of the following services bound together with Kafka:

* Ingest - Logstash/Elasticsearch
* Digest - Akka/Spark
* Backend - Akka-Http/REST
* Frontend - Javascript/D3

TODO: Diagram

The Ingest service uses Logstash twitter input plugin to ingest events from the Twitter streaming API and pushes them directly into Elasticsearch. Additionaly Logstash writes the events into Kafka via the Kafka output plugin. The Digest service reads the data from Kafka and processes them with Spark to provide tweet sentiment analyis and time-sliced hashtag aggregations. The results are written into Cassandra tables optimised to serve frontend requests via the Akka-Http REST interface.

## Ingestion
The ingestion of data from social-media is typical of fast-big-data use-case where the continuous stream and large volumes can lead to back-pressure. 

TODO: Diagram

Data ingestion is based on Logstash which ingests data, filters it, and sends it to output “stashes.” Using the Twitter input plugin Logstash ingests a continuous stream of tweet events and filters them for a keyword (e.g."Brexit"). Using the Kafka output plugin Logstash writes the filtered twitter events to Kafka for downstream processing.

TODO: Logstash snippit

Logstash writes Tweet events into Kafka in there entirety and they contain non-relevant information that can be removed before digestion. Here Akka is employed to react to events and to reduce the event-data for the subsequent realtime analysis. Droping this information here involves no data loss since Logstash has also pushed all Tweets into Elasticsearch and these can be used later for explorative analyis. 

## Digestion
During data digestion value is created by extracting information from the data. In this simple example we are interested in extracting sentiment from Tweets that include a keyword (e.g. "Brexit") and in identifying trending hashtags in realtime. The analysis uses Spark streams to perform the analyis and write micro-batch results into Cassandra in near-realtime. For the sentiment analysis the Stanford CoreNLP Natural-Langauge-Processing library is used.

## Backend
The backend uses Akka HTTP which builds a full server-side stack ontop of Akka Actors and streams. The high-level, routing API of Akka HTTP provides a DSL to describe HTTP “routes” and how they should be handled. Each route is composed of one or more level of Directive that narrows down to handling one specific request.

Transforming request/response bodies between JSON format and application objects is done separately from the route declarations, in marshallers, which are pulled in implicitly using the “magnet” pattern which means that requests can completed as long as there is an implicit marshaller available in scope.

TODO: Marsheler snippit

Default marshallers are provided for simple objects like String or ByteString, and you can define your own for example for JSON. An additional module provides JSON serialization using the spray-json library.

The Route created using the Route DSL is then “bound” to a port to start serving HTTP requests:

## Frontend
Visualisation is achieved using the D3 javascript library to create a Streamgraph of trending hashtags. With most of the hard work alread done the frontend simply makes a request for the actual data to the backend REST serivice and updates the streamgraph viewed in the browser.
