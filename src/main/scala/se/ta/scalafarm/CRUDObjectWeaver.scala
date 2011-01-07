/*
 * Copyright (C) 2010-2011 Thomas Alexandre
 */
package se.ta.scalafarm

import java.sql.{Connection, DriverManager,ResultSet,ResultSetMetaData}
import java.io.{File,FileOutputStream,FileWriter,PrintWriter,StringWriter}
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

object CRUDObjectWeaver{
  val engine = new TemplateEngine
}
class CRUDObjectWeaver(configuration:Configuration) extends Logged {

  import CRUDObjectWeaver._
  
  def generateSnippetsAndHTML(datasets:List[String]): Unit = {
    
    log("Running COW! (CRUD Object Weaver)")
    
    val outputPackageBase = configuration.getDomainObjectsOutputPackageBase
    val snippetOutputDirectory = configuration.getSnippetsOutputDirectory
    val webPagesOutputDirectory = configuration.getWebPagesOutputDirectory
    val buffer = new StringWriter()
    
    //datasets map{table:String =>camelify(table.toLowerCase)} foreach { className =>
    datasets.foreach { tableName =>
      
      // Create HTML directory for table if needed
      val htmlOutputDirectory = webPagesOutputDirectory+"/"+camelifyMethod(tableName).toLowerCase+"s"
      val success = new File(htmlOutputDirectory).mkdir()
      if (success) {
        log("Directory: " + htmlOutputDirectory + " created")
      } else {
        log("Directory: " + htmlOutputDirectory + " Not created")
      }
      
      val buffer = new StringWriter()
      val context = new DefaultRenderContext(engine, new PrintWriter(buffer))
      context.attributes("outputPackageBase") = outputPackageBase
      
      val retrievedProperties:List[scala.IndexedSeq[Object]] = DBUtil.retrieveProperties(configuration, tableName)
      //log("Retrieved properties from DB metadata for "+tableName+" = "+retrievedProperties)
      
      val primaryKeys = DBUtil.retrievePrimaryKeys(configuration,tableName)
      
      val properties = retrievedProperties.map{row =>
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
         //if (primaryKeys.contains(propertyName)) "true" else "false" // is property primarykey?
         if (primaryKeys.contains(propertyName)) true else false
        )
      }.reverse
      log("Properties used in template for table "+tableName+" = "+properties)
      
      val foreignKeys = DBUtil.retrieveForeignKeys(configuration)
      
      log("foreignKeys = "+foreignKeys)
      
      val manyToOne = foreignKeys.getOrElse(tableName,Nil).map{fk =>
        val propertyName = camelifyMethod(fk(7).toString)
        val propertyLabel = camelifyMethod(fk(2).toString)
        //val primaryKey = DBUtil.retrievePrimaryKeys(configuration,fk(7).toString)
        val pkColumnName = camelifyMethod(fk(3).toString)
        log("Property name: "+propertyName+" , Property Label: "+
            propertyLabel+"PK Column Name"+pkColumnName)
        (propertyName,propertyLabel,pkColumnName)
      }.groupBy(_._1)
      
      log("manyToOne = "+manyToOne)
      
      context.attributes("properties") = properties
      context.attributes("manyToOne") = manyToOne
      context.attributes("tableName") = tableName
      
      val snippetTemplate = engine.load("src/main/resources/templates/snippet.ssp")
      snippetTemplate.render(context)
      val snippetFileName = snippetOutputDirectory+"/"+camelify(tableName.toLowerCase)+"Ops.scala"
      FarmUtil.writeToFile(buffer,snippetFileName)
      println("Generated snippet "+snippetFileName)
      buffer.flush
      
      engine.escapeMarkup_=(false)
      
      val htmlTemplateNames = List("list.scaml","add.scaml","search.scaml")
      htmlTemplateNames.foreach { templateName =>
        val buffer = new StringWriter()
        val context = new DefaultRenderContext(engine, new PrintWriter(buffer))
        val template = engine.load("src/main/resources/templates/"+templateName)
        context.attributes("outputPackageBase") = outputPackageBase
        context.attributes("className") = camelify(tableName.toLowerCase)
        context.attributes("properties") = properties
        log("Properties: "+properties)
        template.render(context)
        val fileName = htmlOutputDirectory+"/"+templateName.substring(0,templateName.length-6)+".html"
        FarmUtil.writeToFile(buffer,fileName)
        println("Generated "+fileName)
      }
    }    
  }
}
