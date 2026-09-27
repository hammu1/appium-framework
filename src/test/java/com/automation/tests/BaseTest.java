package com.automation.tests;

import com.automation.core.AppiumServerManager;
import com.automation.core.ConfigReader;
import com.automation.core.DriverFactory;
import com.automation.utils.ScreenshotUtil;
import io.appium.java_client.android.AndroidDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeMethod;

public abstract class BaseTest {

    protected static final Logger log = LoggerFactory.getLogger(BaseTest.class);
    protected AndroidDriver driver;

    @BeforeMethod(alwaysRun = true)
    public void setUp() {
        driver = DriverFactory.createAndroidDriver();
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown(ITestResult result) {
        if (!result.isSuccess() && ConfigReader.getBool("screenshot.on.failure", true)) {
            ScreenshotUtil.capture(driver, "FAIL_" + result.getName());
        }
        DriverFactory.quitDriver();
    }

    @AfterSuite(alwaysRun = true)
    public void shutdownServer() {
        AppiumServerManager.stop();
    }
}
