package ch.presland.data.server

import com.datastax.driver.core.{Cluster, Host, Metadata, Session}
import scala.collection.JavaConversions._

object CassandraConnector {

  def connect(): Session = {

    val cluster = Cluster.builder()
      .addContactPoint("localhost")
      .withPort(Integer.valueOf("9042"))
      .build()

    val metadata:Metadata = cluster.getMetadata
    println(s"Connected to cluster: ${metadata.getClusterName}")

    metadata.getAllHosts foreach {
      case host: Host =>
        println(s"Datacenter: ${host.getDatacenter}; Host: ${host.getAddress}; Rack: ${host.getRack}")
    }

    cluster.newSession()
  }

  def close(session: Session): Unit = {
    val cluster = session.getCluster
    session.close()
    cluster.close()
  }

}
