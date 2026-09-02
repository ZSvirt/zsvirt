package org.zstack.sdk;

import java.util.HashMap;
import java.util.Map;

/**
 * Extra source-to-SDK class mappings contributed by enterprise SDK (sdk-ee).
 * Open-source {@link SourceClassMap} stays free of EE entries.
 */
public final class SourceClassMapExtension {
    public static final HashMap<String, String> srcToDstMapping = new HashMap<String, String>();
    public static final HashMap<String, String> dstToSrcMapping = new HashMap<String, String>();

    private static volatile boolean eeMapLoadAttempted;

    private SourceClassMapExtension() {
    }

    public static synchronized void registerAdditional(Map<String, String> srcToDst, Map<String, String> dstToSrc) {
        if (srcToDst != null) {
            srcToDstMapping.putAll(srcToDst);
        }
        if (dstToSrc != null) {
            dstToSrcMapping.putAll(dstToSrc);
        }
    }

    /**
     * Look up SDK destination class name, loading sdk-ee SourceClassMapEe when present.
     */
    public static String getSrcToDst(String src) {
        ensureEnterpriseMapLoaded();
        return srcToDstMapping.get(src);
    }

    private static void ensureEnterpriseMapLoaded() {
        if (eeMapLoadAttempted) {
            return;
        }
        synchronized (SourceClassMapExtension.class) {
            if (eeMapLoadAttempted) {
                return;
            }
            eeMapLoadAttempted = true;
            try {
                Class.forName("org.zstack.sdk.SourceClassMapEe");
            } catch (ClassNotFoundException ignored) {
                // sdk-ee not on classpath
            }
        }
    }
}
