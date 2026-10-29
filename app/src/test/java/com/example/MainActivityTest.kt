package com.example

import androidx.test.core.app.ActivityScenario
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MainActivityTest {

    @Test
    fun testMainActivityLaunchesWithoutCrashing() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        assertNotNull(scenario)
        scenario.close()
    }
}
