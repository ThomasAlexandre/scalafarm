/*
 * Copyright (C) 2010-2011 Thomas Alexandre
 */
import sbt._
//import de.element34.sbteclipsify._

//class ScalaFarmProject(info: ProjectInfo) extends DefaultProject(info) with Eclipsify {
class ScalaFarmProject(info: ProjectInfo) extends DefaultProject(info) {

  val scalatools_snapshots = ScalaToolsSnapshots
  val scalatools_releases = "Scalatools Releases Repository" at "http://scala-tools.org/repo-releases"
  val fusesource_releases = "FuseSource Public Repository" at "http://repo.fusesource.com/nexus/content/repositories/public"
  val configgy_releases = "Configgy releases" at "http://www.lag.net/repo/"
  val eclipse_releases = "EclipseLink Repository" at "http://ftp.ing.umu.se/mirror/eclipse/rt/eclipselink/maven.repo"
  val akka_releases = "Akka Repository" at "http://www.scalablesolutions.se/akka/repository"
  
  val configgy = "net.lag" % "configgy" % "2.0.0" % "compile->default"
  val derby_client = "org.apache.derby" % "derbyclient" % "10.5.3.0_1"
  val jpa2 = "org.eclipse.persistence" % "javax.persistence" % "2.0.0"
  val scalate = "org.fusesource.scalate" % "scalate-core" % "1.2"
  val dbunit = "org.dbunit" % "dbunit" % "2.4.7"
  val junit = "junit" % "junit" % "4.8.2"
  val scalatest = "org.scalatest" % "scalatest" % "1.2-for-scala-2.8.0.final-SNAPSHOT" 
  val sl4jsimple = "org.slf4j" % "slf4j-simple" % "1.6.0"
  val sl4japi = "org.slf4j" % "slf4j-api" % "1.6.0"
}