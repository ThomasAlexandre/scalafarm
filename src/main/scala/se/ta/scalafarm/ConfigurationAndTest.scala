/*
 * Copyright (C) 2010-2011 Thomas Alexandre
 */
package se.ta.scalafarm

import org.fusesource.scalate._
import java.io.{File,PrintWriter,StringWriter,FileWriter}
import scala.util.logging.Logged
import scala.xml._
import scala.xml.transform._
import se.ta.scalafarm.util._

class ConfigurationAndTest(configuration:Configuration,engine:TemplateEngine) extends Logged {
    
  def generateTestConfigurationFile(datasets:List[String]): File = { 
    
    println("Running CAT! (Configuration And Test)")
    val TEST_CONFIG_FILE = "test.config"
    println("Generating "+TEST_CONFIG_FILE+" file...")
    
    // Generate Config File for Test
    val environment = configuration.getEnvironment
    val outputDirectory = configuration.getTestDataOutputDirectory
    val persistenceUnit = configuration.getPersistenceUnit
    val databaseName = configuration.getSourceDatabaseName
    val testConfigDirectory = configuration.getTestConfigDirectory
    
    // Create directory if needed
    val success = (new File(testConfigDirectory)).mkdirs()
    if (success) {
      log("Directory: " + testConfigDirectory + " created")
    } else {
      log("Directory: " + testConfigDirectory + " not created")
    }
    
    val template = engine.load("src/main/resources/templates/test_config.ssp")
    val buffer = new StringWriter()
    val context = new DefaultRenderContext(engine, new PrintWriter(buffer))  
    
    context.attributes("environment") = environment
    context.attributes("database") = databaseName
    context.attributes("datasets") = datasets
    context.attributes("persistenceUnit") = persistenceUnit
    template.render(context)
    
    // Write to output file
    FarmUtil.writeToFile(buffer,testConfigDirectory+"/"+TEST_CONFIG_FILE)
  }
  
  def generateTestPopulateDBFile(datasets:List[String]): Unit = { 
    
    val DEFAULT_TEST_POPULATE_DATA_SCALA_FILE="TestPopulateDB.scala"
    println("Generating "+DEFAULT_TEST_POPULATE_DATA_SCALA_FILE+" file...")
    
    // Generate Config File for Test
    val outputDirectory = configuration.getTestDataOutputDirectory
    val outputPackageBase = configuration.getDomainObjectsOutputPackageBase
    
    val template = engine.load("src/main/resources/templates/test_populate_db.ssp")
    val buffer = new StringWriter()
    val context = new DefaultRenderContext(engine, new PrintWriter(buffer))
    context.attributes("outputPackageBase") = outputPackageBase
    context.attributes("datasets") = datasets
    template.render(context)
    
    // Write to output file
    FarmUtil.writeToFile(buffer,outputDirectory+"/"+DEFAULT_TEST_POPULATE_DATA_SCALA_FILE)
  }
  
  def generatePersistenceUnitUtilFile(datasets:List[String]): Unit = { 
    
    val DEFAULT_PERSISTENCE_UNIT_UTIL_FILE="PersistenceUnit.scala"
    println("Generating "+DEFAULT_PERSISTENCE_UNIT_UTIL_FILE+" file...")
    
    // Generate Config File for Test
    val outputDirectory = configuration.getUtilOutputDirectory
    val outputPackageBase = configuration.getDomainObjectsOutputPackageBase
    
    // Create directory if needed
    val success = (new File(outputDirectory)).mkdirs()
    if (success) {
      log("Directory: " + outputDirectory + " created")
    } else {
      log("Directory: " + outputDirectory + " could not be created")
    }
    
    val template = engine.load("src/main/resources/templates/persistence_unit.ssp")
    val buffer = new StringWriter()
    val context = new DefaultRenderContext(engine, new PrintWriter(buffer))
    context.attributes("outputPackageBase") = outputPackageBase
    //context.attributes("datasets") = datasets
    template.render(context)
    
    // Write to output file
    FarmUtil.writeToFile(buffer,outputDirectory+"/"+DEFAULT_PERSISTENCE_UNIT_UTIL_FILE)
  }
  
  def generateOrmXmlFile(datasets:List[String]): Unit = { 
   
    println("Generating ORM file...")
    
    // Generate Config File for Test
    val outputDirectory = configuration.getSrcMainResourcesDirectory
    val outputFilename = configuration.getOrmXmlFilename
    val outputPackageBase = configuration.getDomainObjectsOutputPackageBase
    
    val template = engine.load("src/main/resources/templates/orm.scaml")
    val buffer = new StringWriter()
    val context = new DefaultRenderContext(engine, new PrintWriter(buffer))
    context.attributes("outputPackageBase") = outputPackageBase
    context.attributes("datasets") = datasets.map{ table =>
      StringUtil.camelify(table.toLowerCase)
    }
    template.render(context)
    
    // Write to output file
    FarmUtil.writeToFile(buffer,outputDirectory+"/"+outputFilename)
  }
  
  def updatePersistenceXmlFile(): Unit = {
    
    println("Updating persistence.xml file ...")
    val outputDirectory = configuration.getSrcMainResourcesDirectory
    val outputFilename = configuration.getOrmXmlFilename
    val persistenceUnitName = configuration.getPersistenceUnit
    val persistenceXML = XML.loadFile(outputDirectory+"/persistence.xml")
    
    def makePersistenceUnitInfo(config:Configuration) =
      (<persistence-unit name={config.getPersistenceUnit} transaction-type="RESOURCE_LOCAL">
          <properties>
            <property name="hibernate.dialect" value={config.getDatabaseDialect}></property>
            <property name="hibernate.connection.driver_class" value={config.getDriver}></property>
            <property name="hibernate.connection.url" value={config.getTargetURL}></property>
            <property name="hibernate.connection.username" value={config.getTargetUsername}></property>
            <property name="hibernate.connection.password" value={config.getTargetPassword}></property>
            <property name="hibernate.max_fetch_depth" value="3"></property>
            <property name="hibernate.show_sql" value="true"></property>
            <property name="hibernate.hbm2ddl.auto" value="update"></property>
          </properties>
          <mapping-file>META-INF/{config.getOrmXmlFilename}</mapping-file>
       </persistence-unit>)
    
    val addPersistenceUnitInfo = new RewriteRule {
      override def transform(n: Node): NodeSeq = n match {
        case e: Elem if e.label == "persistence" =>
          new Elem(e.prefix, e.label,
                   e.attributes,
                   e.scope,
                   transform(e.child) ++ makePersistenceUnitInfo(configuration) :_*)
        case n => n
      }
    }
    
    val persistenceUnitXML = 
      (persistenceXML \\ "persistence-unit").filter(n => (n \ "@name")
                                                    .text
                                                    .compareTo(persistenceUnitName)==0)
    println("persistenceUnitXML :"+persistenceUnitXML)
    
    if (persistenceUnitXML.isEmpty) {
      // generate whole <persistence-unit>
      println("need to generate <persistence-unit> ")
      val transformedNodes = new RuleTransformer(addPersistenceUnitInfo).transform(persistenceXML)
      transformedNodes.foreach{ node =>
        println("saving node :"+node)
        XML.save(outputDirectory+"/persistence.xml", node, "UTF-8", true, null)
      }
    } 
  }
  
  def updateDependenciesInPOMFile():Unit = {
    val targetRootDir = configuration.getTargetRootDirectory
    val pomXML = XML.loadFile(targetRootDir+"/pom.xml")
    
    val dependencies = 
      (<dependency>
       <groupId>org.apache.derby</groupId>
       <artifactId>derbyclient</artifactId>
       <version>10.4.2.0</version>
       <optional>true</optional>
       </dependency>
       <dependency>
          <groupId>org.dbunit</groupId>
          <artifactId>dbunit</artifactId>
          <version>2.4.7</version>
          <type>jar</type>
       </dependency>
       <dependency>
          <groupId>net.lag</groupId>
          <artifactId>configgy</artifactId>
          <version>2.0.0</version>
       </dependency>)
    
    val addPOMDependencies = new RewriteRule {
      override def transform(n: Node): NodeSeq = n match {
        case e: Elem if e.label == "dependencies" =>
          new Elem(e.prefix, e.label,
                   e.attributes,
                   e.scope,
                   transform(e.child) ++ dependencies :_*)
        case n => n
      }
    }
    
    val dbUnitDependencyXML = 
      (pomXML \\ "artifactId").filter(n => n.text.compareTo("dbunit")==0)
    println("dbUnitDependencyXML :"+dbUnitDependencyXML)
    
    if (dbUnitDependencyXML.isEmpty) {
      // add all jar dependencies
      println("Need to generate update POM file with dependencies. ")
      val transformedNodes = new RuleTransformer(addPOMDependencies).transform(pomXML)
      transformedNodes.foreach{ node =>
        log("saving node :"+node)
        XML.save(targetRootDir+"/pom.xml", node, "UTF-8", true, null)
      }
    } 
  }
  
  def transformTestData(): Unit = {
    
    println("Transforming Test Data ...")
    val outputDirectory = configuration.getSrcMainResourcesDirectory
    val outputFilename = configuration.getOrmXmlFilename
    val persistenceUnitName = configuration.getPersistenceUnit
    val testDataOutputDir = configuration.getTestDataOutputDirectory+"/"+configuration.getSourceDatabaseName
    val foreignKeys = DBUtil.retrieveForeignKeys(configuration)
    
    // Rename foreignkeys
    foreignKeys.keys.foreach{ table =>
      val filePath = testDataOutputDir+"/"+table+".xml"
      log("Transforming XML foreignkey names for "+filePath)
      
      val lines = scala.io.Source.fromFile(filePath).getLines.toList
      
      // a transformation is a Tuple with 2 strings ("Find What","Replace With")
      // fk(7) -> FK_COLUMN_NAME, VARCHAR
      // fk(2) -> PK_TABLE_NAME, VARCHAR
      // fk(3) -> PK_COLUMN_NAME, VARCHAR 
      val transformations = foreignKeys(table).map { fk =>
        (fk(7).toString,StringUtil.camelify(fk(2).toString)+"_"+fk(3))
      }
      log("transformations: "+transformations)
      
      
      val transformedLines = lines.map { line =>
        transformations.foldLeft(line){ (replaced:String,transformation:Tuple2[String,String])=>
          replaced.replaceFirst(transformation._1, transformation._2)
        }  
      }
    
      val fileWriter = new FileWriter(filePath)
      transformedLines.foreach { transformedLine =>
        fileWriter.write(transformedLine+"\n")
      }
      fileWriter.close()
    }
  }
  
  def generateBootFile(datasets:List[String]): Unit = { 
   
    println("Generating Boot file...")
    
    // Generate Config File for Test
    val outputDirectory = configuration.getBootDirectory
    val outputPackageBase = configuration.getDomainObjectsOutputPackageBase
    
    val template = engine.load("src/main/resources/templates/boot_class.ssp")
    val buffer = new StringWriter()
    val context = new DefaultRenderContext(engine, new PrintWriter(buffer))
    context.attributes("tableNames") = datasets.map{ table =>
      StringUtil.camelify(table.toLowerCase)
    }
    context.attributes("outputPackageBase") = outputPackageBase
    template.render(context)
    
    // Write to output file
    FarmUtil.writeToFile(buffer,outputDirectory+"/Boot.scala")
  }
}
