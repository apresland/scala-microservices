# Tweet analysis with reactive realtime streams  
Spark, Akka, Cassandra and Kafka are four of the five members of a stack known by the acronym SMACK. When combined with the fith member, Mesos, they provide a complete opensource Big Data platform. This project demonstates how they can be used to construct reactive streams to construct a realtime tweet analyisis pipeline providing sentiment and hashtag analysis.

## Kafka
Kafka is a distributed horizontally scaleable and fault-tolerant Message Broker used for building realtime data pipelines and streaming applications with large data volumes. Because Kafka partitions data and saves it in an Append-Only-Log it can handle Terabytes of data without impact on performance.

## Akka
Akka is an implementation of the Actor model that allows the construction of highly distributed reactive applications. Combining with Scala as the development language many useful aspects of functional programming can leveraged to proivide concise solutions. Since Akka 2.4 REST services are also supported.

