package com.automation.crawler;

import com.automation.core.ConfigReader;
import com.automation.utils.ScreenshotUtil;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Generic auto-explorer for ANY APK.
 * Walks the UI, taps clickable elements, scrolls, and reports:
 *  - activities visited
 *  - screens captured
 *  - crashes / unexpected app exits
 *
 * Strategy: BFS-style — at each step, list visible clickable elements,
 * pick one we have not tapped before in the current activity, tap it,
 * snapshot, repeat. If stuck, swipe up. Exits when duration expires
 * or the app process is no longer foreground.
 */
public class SmokeCrawler {

    private static final Logger log = LoggerFactory.getLogger(SmokeCrawler.class);

    private final AndroidDriver driver;
    private final String packageUnderTest;
    private final long durationMs;
    private final int maxDepth;
    private final boolean screenshotEachStep;
    private final Set<String> visitedActivities = new LinkedHashSet<>();
    private final Set<String> tappedSignatures = new HashSet<>();
    private int stepsTaken = 0;
    private int crashesDetected = 0;

    public SmokeCrawler(AndroidDriver driver, String packageUnderTest) {
        this.driver = driver;
        this.packageUnderTest = packageUnderTest;
        this.durationMs = ConfigReader.getInt("crawler.duration.seconds", 60) * 1000L;
        this.maxDepth = ConfigReader.getInt("crawler.max.depth", 10);
        this.screenshotEachStep = ConfigReader.getBool("crawler.screenshot.each.step", true);
    }

    public CrawlReport crawl() {
        Instant deadline = Instant.now().plus(Duration.ofMillis(durationMs));
        log.info("=== SmokeCrawler started: pkg={}, duration={}s ===",
                packageUnderTest, durationMs / 1000);

        snapshot("crawl_start");
        recordActivity();

        while (Instant.now().isBefore(deadline) && stepsTaken < maxDepth * 50) {
            if (!isAppForeground()) {
                log.warn("App {} no longer in foreground — possible crash or exit", packageUnderTest);
                crashesDetected++;
                break;
            }

            List<WebElement> tappable = findTappable();
            if (tappable.isEmpty()) {
                log.info("No tappable elements visible — swiping up to scroll");
                swipeUp();
                stepsTaken++;
                continue;
            }

            WebElement chosen = pickUntapped(tappable);
            if (chosen == null) {
                log.info("All visible elements already tapped — swiping up to look for more");
                swipeUp();
                stepsTaken++;
                continue;
            }

            String sig = signature(chosen);
            log.info("Step {}: tapping [{}]", ++stepsTaken, sig);
            tappedSignatures.add(activityKey() + "::" + sig);
            try {
                chosen.click();
                Thread.sleep(800);
                recordActivity();
                if (screenshotEachStep) snapshot("step_" + stepsTaken);
            } catch (Exception e) {
                log.warn("Tap failed at step {}: {}", stepsTaken, e.getMessage());
            }
        }

        snapshot("crawl_end");
        CrawlReport report = new CrawlReport(
                packageUnderTest, stepsTaken, crashesDetected, visitedActivities);
        log.info("=== SmokeCrawler finished ===\n{}", report);
        return report;
    }

    private boolean isAppForeground() {
        try {
            String current = driver.getCurrentPackage();
            return packageUnderTest.equals(current);
        } catch (Exception e) {
            return false;
        }
    }

    private void recordActivity() {
        try {
            String act = driver.currentActivity();
            if (act != null && visitedActivities.add(act)) {
                log.info("→ New activity discovered: {}", act);
            }
        } catch (Exception ignored) { }
    }

    private String activityKey() {
        try {
            String a = driver.currentActivity();
            return a == null ? "?" : a;
        } catch (Exception e) {
            return "?";
        }
    }

    private List<WebElement> findTappable() {
        List<WebElement> out = new ArrayList<>();
        try {
            out.addAll(driver.findElements(By.xpath("//*[@clickable='true' and @displayed='true']")));
        } catch (NoSuchElementException ignored) { }
        return out;
    }

    private WebElement pickUntapped(List<WebElement> elements) {
        String act = activityKey();
        for (WebElement el : elements) {
            String sig = signature(el);
            if (!tappedSignatures.contains(act + "::" + sig)) {
                return el;
            }
        }
        return null;
    }

    private String signature(WebElement el) {
        try {
            String id = el.getAttribute("resource-id");
            String text = el.getText();
            String cls = el.getAttribute("class");
            return (id == null ? "" : id) + "|" + (text == null ? "" : text) + "|" + cls;
        } catch (Exception e) {
            return UUID.randomUUID().toString();
        }
    }

    private void swipeUp() {
        try {
            org.openqa.selenium.Dimension size = driver.manage().window().getSize();
            new com.automation.utils.Gestures(driver).swipe(
                    size.getWidth() / 2,
                    (int) (size.getHeight() * 0.8),
                    size.getWidth() / 2,
                    (int) (size.getHeight() * 0.2),
                    400);
        } catch (Exception e) {
            log.warn("Swipe up failed: {}", e.getMessage());
        }
    }

    private void snapshot(String label) {
        ScreenshotUtil.capture(driver, "crawl_" + label);
    }

    public static class CrawlReport {
        public final String pkg;
        public final int steps;
        public final int crashes;
        public final Set<String> activities;

        public CrawlReport(String pkg, int steps, int crashes, Set<String> activities) {
            this.pkg = pkg;
            this.steps = steps;
            this.crashes = crashes;
            this.activities = activities;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Package:    ").append(pkg).append('\n');
            sb.append("Steps:      ").append(steps).append('\n');
            sb.append("Crashes:    ").append(crashes).append('\n');
            sb.append("Activities: ").append(activities.size()).append('\n');
            for (String a : activities) sb.append("  - ").append(a).append('\n');
            return sb.toString();
        }
    }
}
