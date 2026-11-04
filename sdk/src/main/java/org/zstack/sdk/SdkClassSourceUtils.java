package org.zstack.sdk;

import java.io.File;
import java.net.URL;
import java.net.URLDecoder;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * Utilities for classifying SDK-related product classes by classpath origin.
 */
public final class SdkClassSourceUtils {
    private static final String ENTERPRISE_PATH_SEGMENT = "zsvirt-ee";
    public static final String MANIFEST_EDITION_ATTRIBUTE = "ZStack-Edition";
    public static final String MANIFEST_EDITION_ENTERPRISE = "enterprise";

    private SdkClassSourceUtils() {
    }

    /**
     * 判断给定 Class 是否来自企业版仓库 zsvirt-ee（用于开发环境的 SDK 代码生成过滤）。
     * Decide whether the given Class originates from the enterprise repo zsvirt-ee
     * (for SDK codegen filtering in a development environment).
     *
     * <p><b>用途 / Purpose</b><br>
     * 供 {@code sdk-tools} / {@code sdk-tools-ee} 在扫描 {@code @RestRequest} 等类型时区分
     * API/结构类来自 zsvirt-ee 目录内还是外，从而决定写入开源 {@code sdk} 还是 {@code sdk-ee}。
     * Used by sdk-tools / sdk-tools-ee when scanning API types so open-source {@code sdk}
     * and enterprise {@code sdk-ee} generation stay separated.
     *
     * <p><b>原理 / How it works</b><br>
     * 读取 {@code clz.getProtectionDomain().getCodeSource().getLocation()}，按下面顺序判定：
     * Inspects ProtectionDomain → CodeSource → location URL, then:
     * <ol>
     *   <li>规范化 path 是否包含路径段 {@code zsvirt-ee}（兼容 {@code /zsvirt-ee/} 与
     *   {@code \zsvirt-ee\}）— 适合 reactor / IDE 下的 {@code target/classes}。<br>
     *   Path contains {@code zsvirt-ee} — for reactor/IDE {@code target/classes}.</li>
     *   <li>若 location 是 jar：读取 {@code META-INF/MANIFEST.MF} 中
     *   {@code ZStack-Edition: enterprise}（由 zsvirt-ee 父 POM 的 maven-jar-plugin 写入）。
     *   {@code ./runMavenProfile sdk}（存在 zsvirt-ee 时）通常从 {@code ~/.m2} 拉 EE jar，path 不再含
     *   {@code zsvirt-ee}，必须靠 Manifest。<br>
     *   If location is a jar: read Manifest attribute {@code ZStack-Edition=enterprise}
     *   (written by zsvirt-ee parent POM). EE SDK generation typically loads EE jars from
     *   {@code ~/.m2}, so Manifest is required.</li>
     * </ol>
     * 不根据 Java package 名判断。Package names are not used.
     *
     * <p><b>开发 / 生成场景 / Dev codegen usage</b><br>
     * - reactor {@code zsvirt-ee/**\/target/classes}：靠 path。<br>
     * - 已 {@code mvn install} 的 EE jar：靠 Manifest；改父 POM 后需重新 install EE 模块。<br>
     * - reactor {@code target/classes}: path check.<br>
     * - installed EE jars: Manifest; re-install EE modules after parent POM change.
     *
     * <p><b>非目标 / Non-goals</b><br>
     * 不表示业务上的 premium/商业功能；{@code premium/} 等开源树内模块一律视为非 EE。
     * Not a premium/commercial-feature marker; open-source-tree modules such as
     * {@code premium/} are non-EE.
     *
     * <p><b>失败策略 / Failure policy</b><br>
     * 无法读取 CodeSource/location/Manifest 时返回 {@code false}（保守：不当作 EE）。
     * Returns {@code false} when origin cannot be determined (conservative: not EE).
     *
     * @param clz product API or structure class to inspect; null returns false
     * @return true if path contains {@code zsvirt-ee} or jar Manifest marks enterprise
     */
    public static boolean isFromEnterpriseEdition(Class<?> clz) {
        if (clz == null) {
            return false;
        }

        try {
            ProtectionDomain domain = clz.getProtectionDomain();
            if (domain == null) {
                return false;
            }

            CodeSource codeSource = domain.getCodeSource();
            if (codeSource == null) {
                return false;
            }

            URL location = codeSource.getLocation();
            if (location == null) {
                return false;
            }

            String path = location.getPath();
            if (path == null || path.isEmpty()) {
                return false;
            }

            try {
                path = URLDecoder.decode(path, "UTF-8");
            } catch (Exception ignored) {
                // keep raw path
            }

            String normalized = path.replace('\\', '/');
            String lower = normalized.toLowerCase();
            if (lower.contains("/" + ENTERPRISE_PATH_SEGMENT + "/")
                    || lower.endsWith("/" + ENTERPRISE_PATH_SEGMENT)
                    || lower.contains("/" + ENTERPRISE_PATH_SEGMENT + "!")) {
                return true;
            }

            return isEnterpriseEditionJar(location, normalized);
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean isEnterpriseEditionJar(URL location, String normalizedPath) {
        String filePath = normalizedPath;
        if (filePath.endsWith("/") || !filePath.toLowerCase().endsWith(".jar")) {
            return false;
        }

        if (!"file".equalsIgnoreCase(location.getProtocol())) {
            return false;
        }

        try {
            File jarFile = new File(location.toURI());
            if (!jarFile.isFile()) {
                jarFile = new File(filePath);
            }
            return jarFile.isFile() && readEditionFromJar(jarFile);
        } catch (Exception e) {
            File jarFile = new File(filePath);
            return jarFile.isFile() && readEditionFromJar(jarFile);
        }
    }

    private static boolean readEditionFromJar(File jarFile) {
        try (JarFile jar = new JarFile(jarFile)) {
            Manifest manifest = jar.getManifest();
            if (manifest == null) {
                return false;
            }
            String edition = manifest.getMainAttributes().getValue(MANIFEST_EDITION_ATTRIBUTE);
            return MANIFEST_EDITION_ENTERPRISE.equalsIgnoreCase(edition);
        } catch (Exception e) {
            return false;
        }
    }
}
