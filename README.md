# Reactive realtime Tweetstream analysis

The current goto technologies for Fast-Data are Spark, Mesos, Akka, Cassandra and Kafka which together form the so called SMACK stack. Out of these Mesos (with Marathon and DC/OS) is responsible for scaling the system. From the others Spark/Akka/Kafka focus on reacting to the data streaming into the application with Cassandra as data store.

TODO: ELK (Logstash + Elasticsearch)

This demonstration is concerns only the SACK components using Tweet streams as a source of real-time data.

# Technology stack

## Spark
Spark is an open-source cluster computing framework. Spark supports batch-processing and stream-processing (micro-batch) and allows Lamba architectures to be efficiently implemented since the Stream and Batch APIs are the same and single data-logic implementation can be used for both. Sparks Scala API is highly performant so that applications built on Akka and Spark can be develoope with the same tools and philisophy. It supports all relevant NoSQL and SQL solutions.

## Akka
Akka is an implementation of the Actor model that allows the construction of highly distributed reactive applications. Combining with Scala as the development language many useful aspects of functional programming can leveraged to proivide concise solutions. Since Akka 2.4 REST services are also supported.

## Cassandra
Cassandra is a column-oriented databank that is distributed, linearly scaleable to the number of machines in the computing cluster. Cassandras integrates seamlessly with Spark so that distributed operations can be executed local to the data. This data-locality means that IO operations are minimized and the CPUs only process data found locally on disk. 

## Kafka
Kafka is a distributed horizontally scaleable and fault-tolerant Message Broker used for building realtime data pipelines and streaming applications with large data volumes. Because Kafka partitions data and saves it in an Append-Only-Log it can handle Terabytes of data without impact on performance.

## Logstash

## Elasticsearch

# Architecture Overview
The platform is composed of the following services bound together with Kafka:

* Ingest - Logstash/Elasticsearch
* Digest - Akka/Spark
* Backend - Akka-Http/REST
* Frontend - Javascript/D3

TODO: Diagram

The Ingest service uses Logstash twitter input plugin to ingest events from the Twitter streaming API and pushes them directly into Elasticsearch for optional batch processing (not implemented) and additionaly writes the events into Kafka via the logstash Kafka output plugin. The Digest service reads the data from Kafka and processes them with Spark to provide per-tweet sentiment analyis and time-sliced top ranking hashtag aggregations. The results are written into Cassandra optimised for request from the frontend via the REST interface implemented using Akka-Http.

# Data Ingestion
The ingestion of data from social-media is typical of fast-big-data use-case where the continuous stream and large volumes can lead to back-pressure. 

TODO: Diagram

The data ingestion is based on Logstash which is an open source, server-side data processing pipeline that ingests data, optionaly transforming/filtering it, before sending it to an output “stash.” With the Twitter input plugin Logstash supports ingestion of tweet events as a continuous stream and can filter them for keywords (e.g."Brexit"). With the Kafka output plugin Logstash can write the filtered events to a Kafka topic.

TODO: Logstash snippit
