package com.automation.tests;

import com.automation.crawler.SmokeCrawler;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Works against ANY APK in apps/. No app-specific knowledge needed.
 * Run with: mvn test -Dtest=SmokeCrawlTest
 */
public class SmokeCrawlTest extends BaseTest {

    @Test(description = "Auto-crawl the app and report crashes / discovered activities")
    public void crawlAnyApk() {
        String pkg = driver.getCurrentPackage();
        log.info("App launched, package={}", pkg);

        SmokeCrawler crawler = new SmokeCrawler(driver, pkg);
        SmokeCrawler.CrawlReport report = crawler.crawl();

        Assert.assertEquals(report.crashes, 0,
                "App crashed during crawl — check logs and screenshots in target/");
        Assert.assertTrue(report.activities.size() >= 1,
                "Crawler did not discover any activities");
    }
}
