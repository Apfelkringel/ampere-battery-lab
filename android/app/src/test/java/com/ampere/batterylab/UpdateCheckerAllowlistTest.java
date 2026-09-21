package com.ampere.batterylab;

import java.net.URL;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Regression net for UpdateChecker's URL allowlist. Any drift in the
 * GitHub endpoints we trust (raw.githubusercontent.com vs.
 * api.github.com contents path), or in the protocol/port constraints,
 * falls through to a user-visible "Update-Adresse aus
 * Sicherheitsgründen abgelehnt" message; these tests guard against
 * silent loosening of the rules.
 */
public class UpdateCheckerAllowlistTest {

    private static URL url(String s) throws Exception {
        return UpdateChecker.allowlistTestParse(s);
    }

    @Test public void manifestRawUrlAllowed() throws Exception {
        assertTrue(UpdateChecker.isAllowedManifestUrl(url(
                "https://raw.githubusercontent.com/Apfelkringel/ampere-battery-lab-updates/main/latest.json")));
    }

    @Test public void manifestContentsApiUrlAllowed() throws Exception {
        assertTrue(UpdateChecker.isAllowedManifestUrl(url(
                "https://api.github.com/repos/Apfelkringel/ampere-battery-lab-updates/contents/latest.json")));
    }

    @Test public void manifestRawPathOnWrongHostRejected() throws Exception {
        // Same path, spoofed host: must not match.
        assertFalse(UpdateChecker.isAllowedManifestUrl(url(
                "https://evil.example.com/Apfelkringel/ampere-battery-lab-updates/main/latest.json")));
    }

    @Test public void manifestContentsHostWithWrongPathRejected() throws Exception {
        // Right host, wrong path: must not match.
        assertFalse(UpdateChecker.isAllowedManifestUrl(url(
                "https://api.github.com/repos/other-user/other-repo/contents/latest.json")));
    }

    @Test public void manifestNonHttpsRejected() throws Exception {
        assertFalse(UpdateChecker.isAllowedManifestUrl(url(
                "http://raw.githubusercontent.com/Apfelkringel/ampere-battery-lab-updates/main/latest.json")));
        assertFalse(UpdateChecker.isAllowedManifestUrl(url(
                "ftp://api.github.com/repos/Apfelkringel/ampere-battery-lab-updates/contents/latest.json")));
    }

    @Test public void manifestWithPortRejected() throws Exception {
        assertFalse(UpdateChecker.isAllowedManifestUrl(url(
                "https://raw.githubusercontent.com:443/Apfelkringel/ampere-battery-lab-updates/main/latest.json")));
    }

    @Test public void apkRawUrlAllowed() throws Exception {
        assertTrue(UpdateChecker.isAllowedApkUrl(url(
                "https://raw.githubusercontent.com/Apfelkringel/ampere-battery-lab-updates/main/Ampere-Battery-Lab-release.apk")));
    }

    @Test public void apkContentsApiUrlWithRefAllowed() throws Exception {
        assertTrue(UpdateChecker.isAllowedApkUrl(url(
                "https://api.github.com/repos/Apfelkringel/ampere-battery-lab-updates/contents/Ampere-Battery-Lab-release.apk?ref=main")));
    }

    @Test public void apkContentsApiUrlWithoutRefRejected() throws Exception {
        // The contents API requires an explicit ref= query parameter so a
        // tampered file at HEAD never silently serves an untagged artifact.
        assertFalse(UpdateChecker.isAllowedApkUrl(url(
                "https://api.github.com/repos/Apfelkringel/ampere-battery-lab-updates/contents/Ampere-Battery-Lab-release.apk")));
    }

    @Test public void apkNonHttpsRejected() throws Exception {
        assertFalse(UpdateChecker.isAllowedApkUrl(url(
                "http://raw.githubusercontent.com/Apfelkringel/ampere-battery-lab-updates/main/Ampere-Battery-Lab-release.apk")));
    }

    @Test public void apkRawUrlWrongUserRejected() throws Exception {
        assertFalse(UpdateChecker.isAllowedApkUrl(url(
                "https://raw.githubusercontent.com/someone-else/ampere-battery-lab-updates/main/Ampere-Battery-Lab-release.apk")));
    }
}
