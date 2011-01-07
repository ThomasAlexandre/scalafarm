/*
 * Copyright (C) 2010-2011 Thomas Alexandre
 */
package se.ta.scalafarm

import org.dbunit.database.{DatabaseConnection,QueryDataSet}
import org.dbunit.dataset.xml.FlatXmlDataSet
import java.io.FileOutputStream
import scala.util.logging.Logged
import java.io.File
import se.ta.scalafarm.util._

class PopulateInformationGenerator(configuration:Configuration) extends Logged {
  
  def generateTestData(): Unit = {   
    
    println("Running PIG! (Populate Information Generator)")
    val testDataOutputAllInOneFile = configuration.isTestDataOutputAllInOneFile
    val testDataOutputDirectory = configuration.getTestDataOutputDirectory
    val databaseName = configuration.getSourceDatabaseName
    println("Database Name: "+databaseName)
    
    val jdbcConnection = DBUtil.getConnection(configuration)
    val connection = new DatabaseConnection(jdbcConnection)
    val fullDataSet = connection.createDataSet
    val tableNames = fullDataSet.getTableNames.toList
    println("Tables that will be exported as .xml testdata: "+tableNames)
    
    // Create directory if needed
    val success = (new File(testDataOutputDirectory+"/"+databaseName)).mkdirs()
    if (success) {
      log("Directory: " + testDataOutputDirectory + " created")
    } else {
      log("Directory: " + testDataOutputDirectory + " could not be created")
    }
    
    // Create XML datasets using DBUnit
    if(testDataOutputAllInOneFile) {
      FlatXmlDataSet.write(fullDataSet, new FileOutputStream(testDataOutputDirectory+"/"+databaseName+".xml"));
    } else {
      // One file per Table
      tableNames.foreach { tableName =>
        val partialDataSet = new QueryDataSet(connection)
        partialDataSet.addTable(tableName)
        FlatXmlDataSet.write(partialDataSet, new FileOutputStream(testDataOutputDirectory+"/"+databaseName+"/"+tableName+".xml"));
      }
    }
  }
}
