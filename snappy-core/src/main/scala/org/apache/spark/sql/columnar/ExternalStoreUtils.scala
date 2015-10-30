package org.apache.spark.sql.columnar

import java.sql.Connection
import java.util.Properties

//import com.gemstone.gemfire.internal.cache.{PartitionedRegion, AbstractRegion}

import scala.collection.mutable

import org.apache.spark.sql.collection.{UUIDRegionKey, Utils}
import org.apache.spark.sql.execution.ConnectionPool
import org.apache.spark.sql.execution.datasources.jdbc.{DriverRegistry, JdbcUtils}
import org.apache.spark.sql.jdbc.JdbcDialects
import org.apache.spark.sql.row.{GemFireXDClientDialect, GemFireXDDialect}

/**
 * Utility methods used by external storage layers.
 */
private[sql] object ExternalStoreUtils {

  def getAllPoolProperties(url: String, driver: String,
      poolProps: Map[String, String], hikariCP: Boolean) = {
    val urlProp = if (hikariCP) "jdbcUrl" else "url"
    val driverClassProp = "driverClassName"
    if (driver == null || driver.isEmpty) {
      if (poolProps.isEmpty) {
        Map(urlProp -> url)
      } else {
        poolProps + (urlProp -> url)
      }
    } else if (poolProps.isEmpty) {
      Map(urlProp -> url, driverClassProp -> driver)
    } else {
      poolProps + (urlProp -> url) + (driverClassProp -> driver)
    }
  }

  def validateAndGetAllProps(options: Map[String, String], urlProvided : Option[String], driverProvided : Option[String]) = {
    val parameters = new mutable.HashMap[String, String]
    parameters ++= options


    val url = if(!urlProvided.isDefined){
      parameters.remove("url").getOrElse(
        sys.error("Option 'url' not specified"))
    }else{
      urlProvided.get
    }
    val driver = if(!driverProvided.isDefined){
      parameters.remove("driver")
    }else{
      driverProvided
    }
    val poolImpl = parameters.remove("poolimpl")
    val poolProperties = parameters.remove("poolproperties")

    driver.foreach(DriverRegistry.register)

    val hikariCP = poolImpl.map(Utils.normalizeId) match {
      case Some("hikari") => true
      case Some("tomcat") => false
      case Some(p) =>
        throw new IllegalArgumentException("ExternalStoreUtils: " +
            s"unsupported pool implementation '$p' " +
            s"(supported values: tomcat, hikari)")
      case None => false
    }
    val poolProps = poolProperties.map(p => Map(p.split(",").map { s =>
      val eqIndex = s.indexOf('=')
      if (eqIndex >= 0) {
        (s.substring(0, eqIndex).trim, s.substring(eqIndex + 1).trim)
      } else {
        // assume a boolean property to be enabled
        (s.trim, "true")
      }
    }: _*)).getOrElse(Map.empty)

    // remaining parameters are passed as properties to getConnection
    val connProps = new Properties()
    parameters.foreach(kv => connProps.setProperty(kv._1, kv._2))
    val allPoolProps = getAllPoolProperties(url, driver.orNull,
      poolProps, hikariCP)
    (url, driver, allPoolProps, connProps, hikariCP)
  }

  def getPoolConnection(id: String, driver: Option[String],
      poolProps: Map[String, String], connProps: Properties,
      hikariCP: Boolean): Connection = {
    try {
      if (driver.isDefined) DriverRegistry.register(driver.get)
    } catch {
      case cnfe: ClassNotFoundException => throw new IllegalArgumentException(
        s"Couldn't find driver class $driver", cnfe)
    }
    ConnectionPool.getPoolConnection(id, poolProps, connProps, hikariCP)
  }

  def getConnection(url: String, connProperties: Properties) = {
    connProperties.remove("poolProperties")
    JdbcUtils.createConnection(url, connProperties)
    //DriverManager.getConnection(url)
  }

  def getConnectionType(url: String) = {
    JdbcDialects.get(url) match {
      case GemFireXDDialect => ConnectionType.Embedded
      case GemFireXDClientDialect => ConnectionType.Net
      case _ => ConnectionType.Unknown
    }
  }
}

object ConnectionType extends Enumeration {
  type ConnectionType = Value
  val Embedded, Net, Unknown = Value
}
