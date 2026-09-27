package com.automation.pages;

import com.automation.utils.Gestures;
import com.automation.utils.Waits;
import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/**
 * Base for all page objects. Subclass and add app-specific locators + actions.
 */
public abstract class BasePage {

    protected final AppiumDriver driver;
    protected final Waits waits;
    protected final Gestures gestures;

    protected BasePage(AppiumDriver driver) {
        this.driver = driver;
        this.waits = new Waits(driver);
        this.gestures = new Gestures(driver);
    }

    protected WebElement find(By by) {
        return waits.visible(by);
    }

    protected void click(By by) {
        waits.clickable(by).click();
    }

    protected void type(By by, String text) {
        WebElement el = waits.visible(by);
        el.clear();
        el.sendKeys(text);
    }

    protected boolean isDisplayed(By by, int timeoutSec) {
        return waits.exists(by, timeoutSec);
    }

    protected String text(By by) {
        return waits.visible(by).getText();
    }
}
