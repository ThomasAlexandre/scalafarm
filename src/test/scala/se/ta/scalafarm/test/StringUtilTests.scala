/*
 * Copyright (C) 2010-2011 Thomas Alexandre
 */
package se.ta.scalafarm.test

import org.specs._

class StringUtilTests extends Specification { 
  
  "camelify" should {
    "return SampleInput for sample_input" in {
      val result = se.ta.scalafarm.util.StringUtil.camelify("sample_input")   
      result must be equalTo("SampleInput")
    }
    "return SAMPLEINPUT for SAMPLE_INPUT" in {
      val result = se.ta.scalafarm.util.StringUtil.camelify("SAMPLE_INPUT")   
      result must be equalTo("SAMPLEINPUT")
    }
  }
  "camelifyMethod" should {
    "return sampleInput for sample_input" in {
      val result = se.ta.scalafarm.util.StringUtil.camelifyMethod("sample_input")   
      result must be equalTo("sampleInput")
    }
    "return sampleInput for SAMPLE_INPUT" in {
      val result = se.ta.scalafarm.util.StringUtil.camelifyMethod("SAMPLE_INPUT")   
      result must be equalTo("sampleInput")
    }
  }
  "getAsCommaSeparatedStrings" should {
    "return a string of comma separated strings" in {
      val strings = List("CUSTOMER","PRODUCT","MANUFACTURER")
      val result = se.ta.scalafarm.util.StringUtil.getAsCommaSeparatedStrings(strings)  
      val resultString = "\"CUSTOMER\",\"PRODUCT\",\"MANUFACTURER\""
      result must be equalTo(resultString)
    }
  }
}
