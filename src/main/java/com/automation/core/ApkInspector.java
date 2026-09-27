package com.automation.core;

import net.dongliu.apk.parser.ApkFile;
import net.dongliu.apk.parser.bean.ApkMeta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

/**
 * Extracts package name and main launchable activity from an APK file.
 * Reads AndroidManifest.xml directly — does not require aapt to be installed.
 */
public class ApkInspector {

    private static final Logger log = LoggerFactory.getLogger(ApkInspector.class);

    private final String apkPath;
    private final String packageName;
    private final String launchActivity;
    private final String versionName;
    private final String label;

    public ApkInspector(String apkPath) {
        this.apkPath = apkPath;
        File apkFile = new File(apkPath);
        if (!apkFile.exists()) {
            throw new IllegalArgumentException("APK not found: " + apkPath);
        }
        try (ApkFile apk = new ApkFile(apkFile)) {
            ApkMeta meta = apk.getApkMeta();
            this.packageName = meta.getPackageName();
            this.versionName = meta.getVersionName();
            this.label = meta.getLabel();

            String manifestXml = apk.getManifestXml();
            this.launchActivity = extractLaunchActivity(manifestXml, this.packageName);

            log.info("APK inspected: package={}, launchActivity={}, version={}, label={}",
                    packageName, launchActivity, versionName, label);
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse APK: " + apkPath, e);
        }
    }

    /**
     * Crude but effective parser: looks for the activity that contains
     * MAIN action + LAUNCHER category in its intent-filter.
     */
    private String extractLaunchActivity(String manifestXml, String pkg) {
        // Split into activity tags and inspect each block
        String[] blocks = manifestXml.split("<activity");
        for (String block : blocks) {
            if (block.contains("android.intent.action.MAIN")
                    && block.contains("android.intent.category.LAUNCHER")) {
                String name = extractAttr(block, "android:name");
                if (name == null) continue;
                if (name.startsWith(".")) name = pkg + name;
                else if (!name.contains(".")) name = pkg + "." + name;
                return name;
            }
        }
        log.warn("Could not detect launcher activity from manifest; defaulting to .MainActivity");
        return pkg + ".MainActivity";
    }

    private String extractAttr(String xml, String attr) {
        int idx = xml.indexOf(attr + "=\"");
        if (idx < 0) return null;
        int start = idx + attr.length() + 2;
        int end = xml.indexOf('"', start);
        if (end < 0) return null;
        return xml.substring(start, end);
    }

    public String getApkPath()       { return apkPath; }
    public String getPackageName()   { return packageName; }
    public String getLaunchActivity(){ return launchActivity; }
    public String getVersionName()   { return versionName; }
    public String getLabel()         { return label; }
}
