package com.automation.core;

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Comparator;
import java.util.Optional;

/**
 * Builds an AndroidDriver from config + APK.
 * Currently Android-only; iOS support can be added by mirroring this class.
 */
public class DriverFactory {

    private static final Logger log = LoggerFactory.getLogger(DriverFactory.class);
    private static final ThreadLocal<AndroidDriver> DRIVER = new ThreadLocal<>();

    public static AndroidDriver createAndroidDriver() {
        URL serverUrl = AppiumServerManager.start();

        String apkPath = resolveApkPath();
        ApkInspector apk = new ApkInspector(apkPath);

        UiAutomator2Options options = new UiAutomator2Options()
                .setPlatformName(ConfigReader.get("platform.name", "Android"))
                .setAutomationName(ConfigReader.get("automation.name", "UiAutomator2"))
                .setDeviceName(ConfigReader.get("device.name", "emulator-5554"))
                .setApp(new File(apk.getApkPath()).getAbsolutePath())
                .setAppPackage(apk.getPackageName())
                .setAppActivity(apk.getLaunchActivity())
                .setNoReset(ConfigReader.getBool("app.noReset", true))
                .setFullReset(ConfigReader.getBool("app.fullReset", false))
                .setNewCommandTimeout(Duration.ofSeconds(
                        ConfigReader.getInt("new.command.timeout", 300)));

        String pv = ConfigReader.get("platform.version");
        if (!pv.isBlank()) options.setPlatformVersion(pv);

        log.info("Creating AndroidDriver — pkg={}, activity={}", apk.getPackageName(), apk.getLaunchActivity());
        AndroidDriver driver = new AndroidDriver(serverUrl, options);

        int implicitWait = ConfigReader.getInt("implicit.wait", 0);
        if (implicitWait > 0) {
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(implicitWait));
        }

        DRIVER.set(driver);
        return driver;
    }

    public static AndroidDriver getDriver() {
        AndroidDriver d = DRIVER.get();
        if (d == null) throw new IllegalStateException("Driver not initialized for this thread");
        return d;
    }

    public static void quitDriver() {
        AndroidDriver d = DRIVER.get();
        if (d != null) {
            try { d.quit(); } catch (Exception e) { log.warn("Error quitting driver", e); }
            DRIVER.remove();
        }
    }

    /**
     * Resolves the APK path:
     * 1. -Dapp.path=... (system property)
     * 2. config.properties app.path
     * 3. First *.apk found in apps/ directory (alphabetical)
     */
    private static String resolveApkPath() {
        String configured = ConfigReader.get("app.path");
        if (!configured.isBlank()) {
            File f = new File(configured);
            if (!f.exists()) f = new File("apps", configured);
            if (!f.exists()) throw new IllegalArgumentException("APK not found: " + configured);
            return f.getAbsolutePath();
        }

        Path appsDir = Paths.get("apps");
        if (!Files.isDirectory(appsDir)) {
            throw new IllegalStateException("No app.path configured and apps/ directory missing");
        }
        try {
            Optional<Path> apk = Files.list(appsDir)
                    .filter(p -> p.toString().toLowerCase().endsWith(".apk"))
                    .min(Comparator.naturalOrder());
            return apk.orElseThrow(() -> new IllegalStateException(
                    "No .apk found in apps/ — drop one in or set app.path"))
                    .toAbsolutePath().toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to scan apps/ directory", e);
        }
    }
}
