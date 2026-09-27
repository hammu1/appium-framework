package com.automation.core;

import io.appium.java_client.service.local.AppiumDriverLocalService;
import io.appium.java_client.service.local.AppiumServiceBuilder;
import io.appium.java_client.service.local.flags.GeneralServerFlag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;

/**
 * Optional programmatic control of the Appium server.
 * If config.appium.server.autoStart=false, expects user to run `appium` manually.
 */
public class AppiumServerManager {

    private static final Logger log = LoggerFactory.getLogger(AppiumServerManager.class);
    private static AppiumDriverLocalService service;

    public static synchronized URL start() {
        if (!ConfigReader.getBool("appium.server.autoStart", true)) {
            String url = ConfigReader.get("appium.server.url");
            log.info("Auto-start disabled. Using existing server at {}", url);
            try { return new URL(url); } catch (Exception e) { throw new RuntimeException(e); }
        }

        if (service != null && service.isRunning()) {
            return service.getUrl();
        }

        String host = ConfigReader.get("appium.server.host", "127.0.0.1");
        int port = ConfigReader.getInt("appium.server.port", 4723);

        AppiumServiceBuilder builder = new AppiumServiceBuilder()
                .withIPAddress(host)
                .usingPort(port)
                .withArgument(GeneralServerFlag.SESSION_OVERRIDE)
                .withArgument(GeneralServerFlag.LOG_LEVEL, "info")
                .withLogFile(new File("target/logs/appium-server.log"));

        service = AppiumDriverLocalService.buildService(builder);
        service.start();
        log.info("Appium server started at {}", service.getUrl());
        return service.getUrl();
    }

    public static synchronized void stop() {
        if (service != null && service.isRunning()) {
            service.stop();
            log.info("Appium server stopped");
        }
    }
}
