/*
 * Copyright (C) 2010-2011 Thomas Alexandre
 */
package se.ta.scalafarm

import java.sql.ResultSet
import java.io.{FileOutputStream,FileWriter,PrintWriter,StringWriter}
import scala.collection.immutable.IndexedSeq
import scala.collection._
import scala.util.logging.Logged
import org.dbunit.database.{DatabaseConnection,QueryDataSet}
import org.dbunit.dataset.xml.FlatXmlDataSet
import org.fusesource.scalate._
import net.lag.configgy.Configgy
import net.lag.configgy.Config
import mutable.HashMap
import se.ta.scalafarm.util._
import DBUtil.{getConnection,eachRow}
import StringUtil.{camelify,camelifyMethod}

class DomainObjectGenerator(configuration:Configuration,engine:TemplateEngine) extends Logged {
  
  val jdbcConnection = DBUtil.getConnection(configuration)
  val connection = new DatabaseConnection(jdbcConnection)
  
  def generateDomainObjects(): List[String] = {
    
    log("Running DOG! (Domain Objects Generator)")
    
    val template = engine.load("src/main/resources/templates/domain_object.ssp")
    
    val metadata = jdbcConnection.getMetaData
    val catalog = null
 
    val schema = configuration.getSchema
    val outputPackageBase = configuration.getDomainObjectsOutputPackageBase
    val outputDirectory = configuration.getDomainObjectsOutputDirectory
    
    val columnsMap= DBUtil.retrieveAllProperties(configuration)
    
    //log("Columns Map: "+columnsMap.toString)
    val tableNames = columnsMap.keys
    
    // Process Foreign Keys
    val foreignKeys = tableNames.map { tableName =>
      val foreignKeysResultSet = metadata.getImportedKeys(catalog,schema,tableName)
      processResultSet(foreignKeysResultSet)
    }.flatten 
    log("foreignKeys ("+foreignKeys.size+"): "+foreignKeys)
    val manyToOneRelationships = foreignKeys.toList.groupBy(_(6))
    log("manyToOneRelationships: "+manyToOneRelationships.toString)
    val oneToManyRelationships = foreignKeys.toList.groupBy(_(2))
    log("oneToManyRelationships: "+oneToManyRelationships.toString)
    
    // Loop over table names to create an annotated scala JPA file for each table
    tableNames.foreach { tableName =>
      
      val buffer = new StringWriter()
      val context = new DefaultRenderContext(engine, new PrintWriter(buffer))
      
      val primaryKeys = DBUtil.retrievePrimaryKeys(configuration, tableName)
      
      val properties = columnsMap(tableName).map{row =>
        val propertyName = camelifyMethod(row(3).toString)
        (propertyName,
         row(5).toString match {
            case "BIT" => "Boolean" // find right type
            case "BIGINT" => "Long"  
            case "CHAR" | "LONGVARCHAR" | "VARCHAR" => "String"
            case "DECIMAL" => "Int" // find right type (BigDecimal)
            case "NUMERIC" => "BigDecimal" // find right type
            case "SMALLINT" => "Int" // find right type (Short)
            case "INTEGER"  => "Int"
            case "DOUBLE" => "Int"  // find right type (Double)
            case "DATE"  => "Date"
            case "FLOAT" => "Double" // find right type (Float or Double)  
            case "REAL" => "Double" // find right type
            case "TIME"  => "Time" // find right type (java.sql.Time)
            case "TIMESTAMP"  => "Timestamp"  // find right type (java.sql.Timestamp)
            case "TINYINT" => "Byte" // find right type
          },
         row(17).toString match {
            case "NO" => "false"
            case "YES" => "true"
          }
         ,
         if (primaryKeys.contains(propertyName)) "true" else "false",
         row(6), // length
         row(3)  // DB Column Name
        )
      }.reverse
      
      val manyToOne = manyToOneRelationships.getOrElse(tableName,Nil).map{fk =>
        val propertyName = camelifyMethod(fk(7).toString)
        val propertyLabel = camelifyMethod(fk(2).toString)
        log("Property name: "+propertyName+" , Property Label: "+propertyLabel)
        (propertyName,propertyLabel)
      }.groupBy(_._1)
      
      val oneToMany = oneToManyRelationships.getOrElse(tableName,Nil).map{fk => 
        (camelifyMethod(fk(6).toString),camelifyMethod(fk(2).toString),camelify(fk(6).toString.toLowerCase))
      }
      
      context.attributes("tableName") = tableName
      context.attributes("properties") = properties
      context.attributes("primaryKeys") = primaryKeys
      context.attributes("manyToOne") = manyToOne
      context.attributes("oneToMany") = oneToMany
      context.attributes("outputPackageBase") = outputPackageBase
      log("PrimaryKeys for Table "+tableName+" : "+primaryKeys.toString)
      log("Properties in Table "+tableName+" : "+properties.toString)
      log("ManyToOne relationships for table "+tableName+" : "+manyToOne.toString)
      log("OneToMany relationships for table "+tableName+" : "+oneToMany.toString)
      
      template.render(context)
      
      // Write to output file
      FarmUtil.writeToFile(buffer,outputDirectory+"/"+camelify(tableName.toLowerCase)+".scala")
    }
    
    // Sort the table names so that tables containing foreign key relationships (@ManyToOne)
    // are located after their parent to avoid Database Integrity Violations when populating
    // testdata.
    val unsortedTableNames = tableNames.toList
    log("Unsorted Table Names: "+unsortedTableNames)
    
    var tableDependencyMap = new HashMap[String, List[String]]()
    manyToOneRelationships.keys.foreach { key=>
      tableDependencyMap += key.toString -> manyToOneRelationships(key).map(_(2).toString)
    }
    log("tableDependencyMap : "+tableDependencyMap)
       
    //TODO: Can we do the dependency sorting more elegantly ?
    val tablesWithOrWithoutDependencies = unsortedTableNames.partition(tableDependencyMap.contains(_))
    log("tablesWithOrWithoutDependencies: "+tablesWithOrWithoutDependencies)
    
    var sortedTableNames = mutable.ListBuffer.empty[String]
    
    tableDependencyMap.keys.foreach { key=>
      sortedTableNames++= tableDependencyMap(key):+key
    }
    
    log("sortedTableNames: "+sortedTableNames)
    val reversedTwiceList = sortedTableNames.reverse.distinct.reverse
    log("reversedTwiceList: "+reversedTwiceList)
    val leftOvers = unsortedTableNames.filter { tablename => !reversedTwiceList.contains(tablename)}
    log("leftOvers: "+leftOvers)
    val finalSortedList = reversedTwiceList++leftOvers
    finalSortedList.toList
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
