/*
 * Copyright (C) 2010-2011 Thomas Alexandre
 */
package se.ta.scalafarm.util

import java.sql.{ResultSet,ResultSetMetaData}
import java.sql.DriverManager
import scala.util.logging.Logged
import StringUtil._

object DBUtil extends Logged {
  
  def getConnection(configuration:Configuration) = {
    Class.forName(configuration.getDriver).newInstance
    DriverManager.getConnection(configuration.getSourceURL, configuration.getSourceUsername, configuration.getSourcePassword)
  }
  
  def eachRow(resultSet:ResultSet)(closure: ResultSet => Unit) {
    eachRow(resultSet, null, closure)
  }
  
  def eachRow(resultSet:ResultSet, metaClosure: ResultSetMetaData => Unit, closure: ResultSet => Unit) {
    if (metaClosure != null) metaClosure(resultSet.getMetaData)
    while (resultSet.next) closure(resultSet)
  }
  
  def getMetadata(configuration:Configuration):java.sql.DatabaseMetaData = {
    getConnection(configuration).getMetaData
  }
  
  def retrieveAllProperties(configuration:Configuration):Map[String,List[IndexedSeq[Object]]] = {
    val metadata = getMetadata(configuration)
    val columnRows=processResultSet(metadata.getColumns(configuration.getCatalog,configuration.getSchema,null,null))
    columnRows.groupBy(_(2).toString)
  }
  
  def retrieveProperties(configuration:Configuration,tableName:String):List[IndexedSeq[Object]] = {
    val metadata = getMetadata(configuration)
    processResultSet(metadata.getColumns(configuration.getCatalog,configuration.getSchema,tableName,null))
  }
  
  def retrieveTableNames(configuration:Configuration):List[String] = {
    retrieveAllProperties(configuration).keys.toList
  }
  
  def retrieveForeignKeys(configuration:Configuration):Map[String,List[IndexedSeq[Object]]] = {
    val metadata = getMetadata(configuration)
    val foreignKeys = retrieveTableNames(configuration).map { tableName =>
      val foreignKeysResultSet = 
        metadata.getImportedKeys(
          configuration.getCatalog,
          configuration.getSchema,tableName)
      processResultSet(foreignKeysResultSet)
    }.flatten 
    log("foreignKeys: "+foreignKeys)
    foreignKeys.toList.groupBy(_(6).toString)
  }
  
  def retrievePrimaryKeys(configuration:Configuration,tableName:String):List[String] = {
    val metadata = getMetadata(configuration)
    val primaryKeyRS = metadata.getPrimaryKeys(configuration.getCatalog,configuration.getSchema,tableName)
      
    val primaryKeyRows = processResultSet(primaryKeyRS)
    log("PrimaryKey Rows for Table "+tableName+" : "+primaryKeyRows.toString)
      
    primaryKeyRows.map{ primaryKey => 
      camelifyMethod(primaryKey(3).toString)
    } 
  }
  
  private def processResultSet(resultSet:ResultSet):List[IndexedSeq[Object]] = {
    val meta = resultSet.getMetaData
    val columnCount = meta.getColumnCount()
    val columnNames = (1 to columnCount).map{ it => ( meta.getColumnLabel(it),meta.getColumnTypeName(it))}
    log("Number of columns in the result set: "+columnCount.toString)
    log("Columns are: "+columnNames) 
    var columnRows = List[IndexedSeq[Object]]()
    while (resultSet.next) { 
      columnRows ::= (1 to columnCount).map(resultSet.getObject(_))
    }
    columnRows
  }
}
