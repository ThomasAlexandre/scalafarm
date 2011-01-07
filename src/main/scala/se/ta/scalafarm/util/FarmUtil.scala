/*
 * Copyright (C) 2010-2011 Thomas Alexandre
 */
package se.ta.scalafarm.util

import java.io.{File,FileWriter,StringWriter}

object FarmUtil {
  def writeToFile(buffer:StringWriter,filePath:String): File = {
    val fw = new FileWriter(filePath)
    try {
      fw.write(buffer.toString)
    } finally {
      fw.close()
    }
    new File(filePath)
  }
}
