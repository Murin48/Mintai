package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.analysis.OcpbRuleEngine
import com.example.data.model.RiskLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("LeaseCheck", appName)
  }

  @Test
  fun `verify OCPB red sample evaluates red clauses`() {
    val result = OcpbRuleEngine.evaluateContract(OcpbRuleEngine.SAMPLE_CONTRACT_RED)
    assertTrue("Red contract must have red count > 0", result.redCount > 0)
    assertTrue("Red contract safety score must be lower than 80", result.safetyScore < 80)
  }

  @Test
  fun `verify OCPB green sample evaluates green clauses`() {
    val result = OcpbRuleEngine.evaluateContract(OcpbRuleEngine.SAMPLE_CONTRACT_GREEN)
    assertEquals("Green contract must have 0 red violations", 0, result.redCount)
    assertTrue("Green contract safety score must be high", result.safetyScore >= 80)
  }
}
