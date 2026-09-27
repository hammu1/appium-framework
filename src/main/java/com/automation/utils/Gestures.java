package com.automation.utils;

import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Pause;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;

import java.time.Duration;
import java.util.Collections;

/**
 * Cross-platform W3C-Actions gestures (Appium 2 / Selenium 4).
 */
public class Gestures {

    private final AppiumDriver driver;

    public Gestures(AppiumDriver driver) {
        this.driver = driver;
    }

    public void tap(int x, int y) {
        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
        Sequence tap = new Sequence(finger, 1)
                .addAction(finger.createPointerMove(Duration.ZERO,
                        PointerInput.Origin.viewport(), x, y))
                .addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()))
                .addAction(new Pause(finger, Duration.ofMillis(100)))
                .addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
        driver.perform(Collections.singletonList(tap));
    }

    public void tap(WebElement el) {
        org.openqa.selenium.Point p = el.getLocation();
        Dimension s = el.getSize();
        tap(p.getX() + s.getWidth() / 2, p.getY() + s.getHeight() / 2);
    }

    public void swipe(int x1, int y1, int x2, int y2, int durationMs) {
        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
        Sequence swipe = new Sequence(finger, 1)
                .addAction(finger.createPointerMove(Duration.ZERO,
                        PointerInput.Origin.viewport(), x1, y1))
                .addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()))
                .addAction(finger.createPointerMove(Duration.ofMillis(durationMs),
                        PointerInput.Origin.viewport(), x2, y2))
                .addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
        driver.perform(Collections.singletonList(swipe));
    }

    public void swipeUp() {
        Dimension size = driver.manage().window().getSize();
        int x = size.getWidth() / 2;
        swipe(x, (int) (size.getHeight() * 0.8), x, (int) (size.getHeight() * 0.2), 400);
    }

    public void swipeDown() {
        Dimension size = driver.manage().window().getSize();
        int x = size.getWidth() / 2;
        swipe(x, (int) (size.getHeight() * 0.2), x, (int) (size.getHeight() * 0.8), 400);
    }

    public void swipeLeft() {
        Dimension size = driver.manage().window().getSize();
        int y = size.getHeight() / 2;
        swipe((int) (size.getWidth() * 0.8), y, (int) (size.getWidth() * 0.2), y, 400);
    }

    public void swipeRight() {
        Dimension size = driver.manage().window().getSize();
        int y = size.getHeight() / 2;
        swipe((int) (size.getWidth() * 0.2), y, (int) (size.getWidth() * 0.8), y, 400);
    }
}
