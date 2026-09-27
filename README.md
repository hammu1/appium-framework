# Appium Framework — Java + TestNG

<p>
  <img alt="Java" src="https://img.shields.io/badge/Java-17-ED8B00?style=flat-square&logo=openjdk&logoColor=white">
  <img alt="Appium" src="https://img.shields.io/badge/Appium-2.x-662D91?style=flat-square">
  <img alt="TestNG" src="https://img.shields.io/badge/TestNG-suite-DE3423?style=flat-square">
  <img alt="Maven" src="https://img.shields.io/badge/Maven-build-C71A36?style=flat-square&logo=apachemaven&logoColor=white">
  <img alt="License" src="https://img.shields.io/badge/License-All%20Rights%20Reserved-lightgrey?style=flat-square">
</p>

A reusable Android automation framework. Drop any APK in `apps/` and the framework handles driver setup, server management, page-object base, gestures, screenshots, and an **auto-explorer** that smoke-tests any APK with zero app-specific code.

```bash
cp ~/Downloads/yourapp.apk apps/
mvn test -Dtest=SmokeCrawlTest
```

That's a full smoke run on an app the framework has never seen before.

---

## The interesting part: the smoke crawler

Most mobile frameworks need you to write page objects before they can do anything. This one ships with `SmokeCrawler`, which works on **any** APK:

- **Reads the APK binary** to extract the package name and launch activity — no `aapt`, no manifest hunting, no hardcoding
- Launches the app and auto-explores for a configurable duration, tapping clickable elements and scrolling
- Captures a screenshot at every step
- Reports **all activities visited**, **crashes** (detected when the app unexpectedly leaves the foreground), and total step count

It's the fastest way to answer "does this build even survive being used?" before anyone writes a real test.

---

## What you get

- Auto-detects package name + launch activity from any APK
- **Programmatic Appium server** start/stop — no second terminal running `appium`
- TestNG suite with two classes: `SmokeCrawlTest` (generic crawler) and `SampleTest` (template for app-specific tests)
- Page Object base (`BasePage`) with safe waits, click, type, gestures
- W3C gestures — tap, swipe in four directions
- Screenshot on failure + per-step screenshots during the crawl
- Logback logging to `target/logs/`

---

## Project layout

```
appium-framework/
├── apps/                                    # ← drop your APK here
├── pom.xml
├── src/
│   ├── main/java/com/automation/
│   │   ├── core/
│   │   │   ├── ConfigReader.java            # config.properties + -D overrides
│   │   │   ├── ApkInspector.java            # extracts package + launch activity
│   │   │   ├── AppiumServerManager.java     # programmatic Appium server
│   │   │   └── DriverFactory.java           # builds AndroidDriver
│   │   ├── crawler/
│   │   │   └── SmokeCrawler.java            # auto-explorer for any APK
│   │   ├── pages/
│   │   │   └── BasePage.java                # base for page objects
│   │   └── utils/
│   │       ├── Waits.java
│   │       ├── Gestures.java
│   │       └── ScreenshotUtil.java
│   ├── main/resources/
│   │   ├── config.properties
│   │   └── logback.xml
│   └── test/
│       ├── java/com/automation/tests/
│       │   ├── BaseTest.java                # @BeforeMethod / @AfterMethod
│       │   ├── SmokeCrawlTest.java
│       │   └── SampleTest.java
│       └── resources/testng.xml
```

---

## Setup

### 1. Java 17 + Maven

```bash
java -version    # need 17+
mvn -version
```

Set `JAVA_HOME` and `MAVEN_HOME` in your shell profile if they aren't already.

### 2. Android SDK + a device

Install [Android Studio](https://developer.android.com/studio), then:

- **SDK Manager** → install *Android SDK Platform-Tools* and *Android SDK Build-Tools*
- **Virtual Device Manager** → create a Pixel emulator with API 33 or 34 (or just plug in a real device with USB debugging on)
- Add to your shell profile:
  ```bash
  export ANDROID_HOME=$HOME/Android/Sdk
  export PATH=$ANDROID_HOME/platform-tools:$ANDROID_HOME/cmdline-tools/latest/bin:$PATH
  ```
- Verify: `adb --version` prints a version, `adb devices` lists your device

### 3. Appium 2 + UiAutomator2 driver

```bash
npm install -g appium
appium driver install uiautomator2
appium --version    # 2.x
```

### 4. Verify

```bash
adb devices      # must be non-empty
mvn clean compile
```

---

## Running tests

**Drop an APK** — the framework auto-picks the first `.apk` in `apps/`:

```bash
cp /path/to/yourapp.apk apps/
mvn test -Dapp.path=apps/yourapp.apk   # or target a specific one
```

**Start an emulator** (skip if using a real device):

```bash
emulator -avd Pixel_API_34 &
adb wait-for-device
```

**Run the crawler:**

```bash
mvn test -Dtest=SmokeCrawlTest
```

Output lands in `target/logs/automation.log` and `target/screenshots/`.

---

## Writing app-specific tests

Add a page object under `src/main/java/com/automation/pages/` extending `BasePage`:

```java
public class LoginPage extends BasePage {
    private final By usernameField = By.id("com.example:id/username");
    private final By passwordField = By.id("com.example:id/password");
    private final By loginBtn      = AppiumBy.accessibilityId("loginButton");

    public LoginPage(AppiumDriver driver) { super(driver); }

    public void login(String user, String pass) {
        type(usernameField, user);
        type(passwordField, pass);
        click(loginBtn);
    }
}
```

Then a test under `src/test/java/com/automation/tests/` extending `BaseTest`:

```java
public class LoginTest extends BaseTest {
    @Test
    public void validLoginSucceeds() {
        new LoginPage(driver).login("alice", "pw");
        Assert.assertTrue(driver.findElement(By.id("home")).isDisplayed());
    }
}
```

Locator strategy preference, best to worst: `accessibility id` → `id` → `-android uiautomator` → XPath.

---

## Configuration

`src/main/resources/config.properties` controls everything. Override any value at runtime with `-Dkey=value`:

| Key | Purpose |
|-----|---------|
| `app.path` | Path to APK (blank = auto-pick from `apps/`) |
| `device.name` | `emulator-5554` or real device serial from `adb devices` |
| `platform.version` | `13`, `14`, etc. (blank = any) |
| `app.noReset` | `true` keeps app state between tests |
| `appium.server.autoStart` | `true` = framework runs the server; `false` = use external |
| `crawler.duration.seconds` | How long the smoke crawl runs |
| `crawler.max.depth` | Max steps the crawler will take |
| `screenshot.on.failure` | Capture screenshot when a test fails |

---

## Common commands

```bash
mvn clean compile                       # verify deps
mvn test                                # all tests in testng.xml
mvn test -Dtest=SmokeCrawlTest          # crawler only

# specific APK, longer crawl
mvn test -Dtest=SmokeCrawlTest -Dapp.path=apps/myapp.apk -Dcrawler.duration.seconds=180

# real device (serial from `adb devices`)
mvn test -Ddevice.name=R3CN1234ABC
```

---

## Troubleshooting

| Symptom | Fix |
|---------|-----|
| `adb: command not found` | Install Android Studio + add `$ANDROID_HOME/platform-tools` to PATH |
| `appium: command not found` | `npm install -g appium && appium driver install uiautomator2` |
| Driver creation hangs | No emulator/device. `adb devices` must be non-empty |
| `Could not detect launcher activity` | APK has an unusual manifest — set `appium:appActivity` manually in `DriverFactory` |
| Tests can't find elements | Use [Appium Inspector](https://github.com/appium/appium-inspector) to inspect the live UI tree |

---

## Related

- [`claude-qa-skills`](https://github.com/hammu1/claude-qa-skills) — AI agent workflows for QA across web, mobile, extensions, desktop
- [`ai-qa-agent`](https://github.com/hammu1/ai-qa-agent) — multi-platform framework with declarative test plans and PDF reporting

---

## License

**All Rights Reserved** — see [LICENSE](LICENSE).

This code is published for portfolio and evaluation purposes. You're welcome to read it;
reuse in your own projects requires written permission.

Built by [Syed Hammad Ali](https://github.com/hammu1) · AI QA Engineer
