/*
 * Copyright (C) 2010-2011 Thomas Alexandre
 */
package se.ta.scalafarm

import org.fusesource.scalate._
import java.io.{PrintWriter,StringWriter}
import se.ta.scalafarm.util._
import StringUtil.camelify

// Mouse - MOdel Used for SElection
class MOdelUsefulSElects(configuration:Configuration,engine:TemplateEngine) {
  
  def generateModelClasses(datasets:List[String]): Unit = { 
    
    datasets map{table:String =>camelify(table.toLowerCase)} foreach { className =>
      
      println("Generating "+className+"Model.scala file...")
    
      val outputDirectory = configuration.getModelOutputDirectory
      val outputPackageBase = configuration.getDomainObjectsOutputPackageBase
    
      val template = engine.load("src/main/resources/templates/model_class.ssp")
      val buffer = new StringWriter()
      val context = new DefaultRenderContext(engine, new PrintWriter(buffer))
      context.attributes("outputPackageBase") = outputPackageBase
      context.attributes("className") = className
      template.render(context)
    
      // Write to output file
      FarmUtil.writeToFile(buffer,outputDirectory+"/"+className+"Model.scala")
    }
  }
  
  def generateModelMainClass(): Unit = { 

      println("Generating Model.scala file...")
    
      val outputDirectory = configuration.getModelOutputDirectory   
      val template = engine.load("src/main/resources/templates/model_main_class.ssp")
      val buffer = new StringWriter()
      val context = new DefaultRenderContext(engine, new PrintWriter(buffer))
      context.attributes("outputPackageBase") = configuration.getDomainObjectsOutputPackageBase
      context.attributes("persistenceUnit") = configuration.getPersistenceUnit
      template.render(context)
    
      // Write to output file
      FarmUtil.writeToFile(buffer,outputDirectory+"/Model.scala")
  }
}
