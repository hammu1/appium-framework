package com.automation.utils;

import com.automation.core.ConfigReader;
import org.apache.commons.io.FileUtils;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ScreenshotUtil {

    private static final Logger log = LoggerFactory.getLogger(ScreenshotUtil.class);
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");

    public static File capture(WebDriver driver, String label) {
        String dir = ConfigReader.get("screenshot.dir", "target/screenshots");
        String safe = label.replaceAll("[^a-zA-Z0-9._-]", "_");
        File dest = new File(dir, safe + "_" + LocalDateTime.now().format(TS) + ".png");
        try {
            File src = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            FileUtils.copyFile(src, dest);
            log.info("Screenshot saved: {}", dest.getAbsolutePath());
            return dest;
        } catch (Exception e) {
            log.warn("Screenshot failed: {}", e.getMessage());
            return null;
        }
    }
}
