/*
 * Copyright (C) 2010-2011 Thomas Alexandre
 */
package se.ta.scalafarm.util

import net.lag.configgy.Configgy

case class Configuration(filepath:String) {

  val DEFAULT_OUTPUT_DIR = "./output"
  
  val DEFAULT_GENERATOR_CONFIG_FILE_LOCATION = "src/main/resources/generator.config"
  val DEFAULT_ENVIRONMENT = "demo"
  val DEFAULT_DRIVER = "org.apache.derby.jdbc.ClientDriver"
  val DEFAULT_SOURCE_URL = "jdbc:derby://localhost:1527/sample"
  val DEFAULT_SCHEMA = "APP"
  val DEFAULT_USERNAME = "app"
  val DEFAULT_PASSWORD = "app"
  val DEFAULT_TARGET_URL = "jdbc:derby://localhost:1527/lift_sample"
  val DEFAULT_DOG_OUTPUT_PACKAGE = "com.myproject.spa"
  val DEFAULT_DOG_OUTPUT_DIRECTORY = DEFAULT_OUTPUT_DIR
  val DEFAULT_PIG_OUTPUT_DIRECTORY = DEFAULT_OUTPUT_DIR
  val DEFAULT_TEST_CONFIG_DIRECTORY = DEFAULT_OUTPUT_DIR
  val DEFAULT_MAIN_RESOURCES_DIRECTORY = DEFAULT_OUTPUT_DIR
  val DEFAULT_ORM_FILENAME = "orm.xml"
  val DEFAULT_PERSISTENCE_UNIT = "jpaweb-network"
  val DEFAULT_SNIPPETS_OUTPUT_DIRECTORY = DEFAULT_OUTPUT_DIR
  val DEFAULT_CAT_UTIL_OUTPUT_DIRECTORY = DEFAULT_OUTPUT_DIR
  val DEFAULT_MODEL_OUTPUT_DIRECTORY = DEFAULT_OUTPUT_DIR
  val DEFAULT_TARGET_ROOT_DIR = DEFAULT_OUTPUT_DIR
  val DEFAULT_WEB_PAGES_OUTPUT_DIR = DEFAULT_OUTPUT_DIR
  val DEFAULT_BOOT_DIR = DEFAULT_OUTPUT_DIR
  val DEFAULT_PROJECT_TARGET_ROOT = "."
  
  Configgy.configure(filepath)
  val config = Configgy.config
  
  def getEnvironment() = config.getString("environment").getOrElse(DEFAULT_ENVIRONMENT)
  
  def getCatalog() = config.getString("environments."+getEnvironment+".catalog").getOrElse(null)
  
  def getDriver() = config.getString("environments."+getEnvironment+".driver").getOrElse(DEFAULT_DRIVER)
  
  def getSchema() = config.getString("environments."+getEnvironment+".schema").getOrElse(DEFAULT_SCHEMA)
  
  def getSourceURL() = config.getString("environments."+getEnvironment+".source_url").getOrElse(DEFAULT_SOURCE_URL)
  
  def getSourceDatabaseName() = getSourceURL.split("/").reverse.head
  
  def getDatabaseDialect() = getDriver match {
    case "org.apache.derby.jdbc.ClientDriver" => "org.hibernate.dialect.DerbyDialect"
    // add other drivers here...
    case _ => "org.hibernate.dialect.DerbyDialect"
  }
  
  def getSourceUsername() = config.getString("environments."+getEnvironment+".source_username").getOrElse(DEFAULT_USERNAME)
  
  def getSourcePassword() = config.getString("environments."+getEnvironment+".source_password").getOrElse(DEFAULT_PASSWORD)
  
  def getTargetUsername() = config.getString("environments."+getEnvironment+".target_username").getOrElse(DEFAULT_USERNAME)
  
  def getTargetPassword() = config.getString("environments."+getEnvironment+".target_password").getOrElse(DEFAULT_PASSWORD)
  
  def getTargetProjectRoot() = config.getString("environments."+getEnvironment+".target_root_dir").getOrElse(DEFAULT_PROJECT_TARGET_ROOT)

  def getDomainObjectsOutputPackageBase() = config.getString("environments."+getEnvironment+".output_package_base").getOrElse(DEFAULT_DOG_OUTPUT_PACKAGE)
    
  def getDomainObjectsOutputDirectory() = getTargetProjectRoot()+"/"+config.getString("environments."+getEnvironment+".dog_output_dir").getOrElse(DEFAULT_DOG_OUTPUT_DIRECTORY)
  
  def getOrmXmlFilename() = config.getString("environments."+getEnvironment+".cat_orm_filename").getOrElse(getSourceDatabaseName()+"_"+DEFAULT_ORM_FILENAME)
  
  def getPersistenceUnit() = config.getString("environments."+getEnvironment+".persistence_unit").getOrElse(DEFAULT_PERSISTENCE_UNIT)
  
  def getUtilOutputDirectory() = getTargetProjectRoot()+"/"+config.getString("environments."+getEnvironment+".cat_util_output_dir").getOrElse(DEFAULT_CAT_UTIL_OUTPUT_DIRECTORY)
  
  def getSrcMainResourcesDirectory() = getTargetProjectRoot()+"/"+config.getString("environments."+getEnvironment+".src_main_resources_dir").getOrElse(DEFAULT_MAIN_RESOURCES_DIRECTORY)
  
  def getSnippetsOutputDirectory() = getTargetProjectRoot()+"/"+config.getString("environments."+getEnvironment+".snippets_output_dir").getOrElse(DEFAULT_SNIPPETS_OUTPUT_DIRECTORY)
  
  def getTestDataOutputDirectory() = getTargetProjectRoot()+"/"+config.getString("environments."+getEnvironment+".pig_output_dir").getOrElse(DEFAULT_PIG_OUTPUT_DIRECTORY)
  
  def getTestConfigDirectory() = getTargetProjectRoot()+"/"+config.getString("environments."+getEnvironment+".test_config_dir").getOrElse(DEFAULT_TEST_CONFIG_DIRECTORY)
  
  def isTestDataOutputAllInOneFile() = config.getBool("environments."+getEnvironment+".pig_output_all_in_one_file").getOrElse(true) 
  
  def getTargetURL() = config.getString("environments."+getEnvironment+".target_url").getOrElse(DEFAULT_TARGET_URL)
  
  def getModelOutputDirectory() = getTargetProjectRoot()+"/"+config.getString("environments."+getEnvironment+".mouse_output_dir").getOrElse(DEFAULT_MODEL_OUTPUT_DIRECTORY)
  
  def getTargetRootDirectory() = config.getString("environments."+getEnvironment+".target_root_dir").getOrElse(DEFAULT_TARGET_ROOT_DIR)

  def getWebPagesOutputDirectory() = getTargetProjectRoot()+"/"+config.getString("environments."+getEnvironment+".web_pages_output_dir").getOrElse(DEFAULT_WEB_PAGES_OUTPUT_DIR)

  def getBootDirectory() = getTargetProjectRoot()+"/"+config.getString("environments."+getEnvironment+".boot_dir").getOrElse(DEFAULT_BOOT_DIR)
}
