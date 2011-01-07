/*
 * Copyright (C) 2010-2011 Thomas Alexandre
 */
package se.ta.scalafarm

import org.fusesource.scalate._
import scala.util.logging.ConsoleLogger
import se.ta.scalafarm.util._

object Farm {
  /**
   * This is the entry point where all the files are generated
   */
  def main(args: Array[String]): Unit = {
    
    val engine = new TemplateEngine
    val configuration = Configuration("src/main/resources/generator.config")
    
    val dog = new DomainObjectGenerator(configuration,engine) with ConsoleLogger
    val datasets = dog.generateDomainObjects()
    println("datasets: "+datasets)
    
    val pig = new PopulateInformationGenerator(configuration) with ConsoleLogger
    pig.generateTestData()
    
    val cat = new ConfigurationAndTest(configuration,engine) with ConsoleLogger
    cat.generateTestConfigurationFile(datasets)
    cat.generateTestPopulateDBFile(datasets)
    cat.generateOrmXmlFile(datasets)
    cat.generatePersistenceUnitUtilFile(datasets)
    cat.generateBootFile(datasets)
    cat.updatePersistenceXmlFile()
    cat.updateDependenciesInPOMFile()
    cat.transformTestData()
    
    val mouse = new MOdelUsefulSElects(configuration,engine) with ConsoleLogger
    mouse.generateModelMainClass()
    mouse.generateModelClasses(datasets)
    
    val cow = new CRUDObjectWeaver(configuration) with ConsoleLogger
    cow.generateSnippetsAndHTML(datasets)
  }
}  
