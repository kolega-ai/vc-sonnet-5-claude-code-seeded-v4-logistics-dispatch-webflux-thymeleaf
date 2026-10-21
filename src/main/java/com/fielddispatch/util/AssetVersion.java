package com.fielddispatch.util;

import java.util.Random;

/**
 * Generates a cache-busting query suffix appended to static asset URLs
 * (e.g. /css/app.css?v=xxxx) so browsers pick up new CSS/JS after a deploy.
 *
 * PRECISION BASELINE #2: java.util.Random is intentionally used here (not
 * SecureRandom) because this value has no security purpose whatsoever - it
 * only needs to change often enough to bust the browser cache. It is never
 * used for tokens, passwords, session ids, or anything security-relevant.
 */
public final class AssetVersion {

    // Non-cryptographic use only: cache-busting suffix, regenerated once per JVM start.
    private static final String SUFFIX = Long.toString(Math.abs(new Random().nextLong()), 36);

    private AssetVersion() {
    }

    public static String suffix() {
        return SUFFIX;
    }
}
