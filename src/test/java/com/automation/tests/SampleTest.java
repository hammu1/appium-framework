package com.automation.tests;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Template for app-specific tests. Copy this and replace with your own
 * page objects + assertions.
 */
public class SampleTest extends BaseTest {

    @Test(description = "Sanity check — app launches and reports a current activity")
    public void appLaunchesSuccessfully() {
        String pkg = driver.getCurrentPackage();
        String activity = driver.currentActivity();
        log.info("Launched: pkg={}, activity={}", pkg, activity);

        Assert.assertNotNull(pkg, "Package should not be null after launch");
        Assert.assertNotNull(activity, "Activity should not be null after launch");
    }
}
