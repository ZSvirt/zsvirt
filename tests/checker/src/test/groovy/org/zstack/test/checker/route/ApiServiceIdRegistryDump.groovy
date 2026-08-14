package org.zstack.test.checker.route

import org.junit.Test
import org.zstack.core.Platform
import org.zstack.header.description.route.ApiRouteUtils
import org.zstack.header.exception.CloudRuntimeException
import org.zstack.header.message.APIMessage
import org.zstack.utils.BeanUtils
import org.zstack.utils.Utils
import org.zstack.utils.logging.CLogger

import java.lang.reflect.Modifier

/**
 * Assert PackageDescriptionRegistry serviceIds against the frozen baseline resource.
 * <p>
 * Baseline (source): {@code tests/checker/src/test/resources/org/zstack/test/checker/route/api-service-id-registry-baseline.properties}
 * <p>
 * Only baseline APIs are checked. API classes present at runtime but absent from the baseline
 * are ignored so newly added messages do not fail this test until someone updates the file.
 * <p>
 * Optional: {@code -Dapi.service.routes.dump.file=...} still dumps the full runtime map for ad-hoc diff.
 */
class ApiServiceIdRegistryDump {
    private static final CLogger logger = Utils.getLogger(ApiServiceIdRegistryDump.class)

    private static final String BASELINE_RESOURCE =
            "/org/zstack/test/checker/route/api-service-id-registry-baseline.properties"

    private static final String BASELINE_SOURCE_PATH =
            "tests/checker/src/test/resources/org/zstack/test/checker/route/api-service-id-registry-baseline.properties"

    @Test
    void dumpApiServiceIdFromRegistry() {
        System.setProperty("exitJVMOnBootFailure", "false")
        Platform.getManagementServerId()

        Map<String, String> actual = collectActualRoutes()
        maybeDump(actual)

        assert !actual.isEmpty(): "no API service routes resolved from PackageDescriptionRegistry"

        Map<String, String> baseline = loadBaseline()
        assert !baseline.isEmpty(): "baseline is empty: ${BASELINE_SOURCE_PATH}"

        List<String> mismatches = []
        baseline.each { String fqn, String expectedServiceId ->
            String actualServiceId = actual.get(fqn)
            if (actualServiceId == null) {
                mismatches.add("${fqn}: baseline=${expectedServiceId}, actual=<missing>")
            } else if (expectedServiceId != actualServiceId) {
                mismatches.add("${fqn}: baseline=${expectedServiceId}, actual=${actualServiceId}")
            }
        }

        int extras = (actual.keySet() - baseline.keySet()).size()

        if (!mismatches.isEmpty()) {
            mismatches.each { String line ->
                logger.error("ApiServiceIdRegistryDump mismatch: ${line}")
            }
            logger.error("ApiServiceIdRegistryDump: ${mismatches.size()} baseline API(s) do not match registry " +
                    "(baseline size=${baseline.size()}, actual size=${actual.size()}, " +
                    "extras ignored=${extras}).")
            logger.error("Update the baseline if the new serviceIds are intentional: ${BASELINE_SOURCE_PATH}")
            logger.error("Classpath resource: ${BASELINE_RESOURCE}")
            throw new CloudRuntimeException(
                    "ApiServiceIdRegistryDump failed: ${mismatches.size()} baseline API serviceId mismatch(es). " +
                            "See logger for full list. Edit ${BASELINE_SOURCE_PATH} when the change is intentional " +
                            "(resource ${BASELINE_RESOURCE}). " +
                            "Sample: ${mismatches.take(10).join('; ')}" +
                            (mismatches.size() > 10 ? "; ..." : "")
            )
        }

        logger.info("ApiServiceIdRegistryDump - PASS, " +
                "checked ${baseline.size()} baseline APIs against registry " +
                "(actual routes=${actual.size()}, ignored extras not in baseline=${extras})")
    }

    private static Map<String, String> collectActualRoutes() {
        Map<String, String> routes = new TreeMap<>()
        BeanUtils.reflections.getSubTypesOf(APIMessage.class).findAll { Class clz ->
            !clz.isInterface() && !Modifier.isAbstract(clz.getModifiers())
        }.each { Class clz ->
            String serviceId = ApiRouteUtils.resolveServiceIdFromRegistry(clz)
            if (serviceId != null) {
                routes.put(clz.getName(), serviceId)
            }
        }
        return routes
    }

    private static Map<String, String> loadBaseline() {
        InputStream stream = ApiServiceIdRegistryDump.class.getResourceAsStream(BASELINE_RESOURCE)
        if (stream == null) {
            throw new CloudRuntimeException(
                    "baseline resource not found on classpath: ${BASELINE_RESOURCE}. " +
                            "Source file is ${BASELINE_SOURCE_PATH}")
        }
        Map<String, String> baseline = new TreeMap<>()
        stream.withReader("UTF-8") { Reader reader ->
            reader.eachLine { String line ->
                String s = line.trim()
                if (!s.isEmpty() && !s.startsWith("#")) {
                    int eq = s.indexOf('=')
                    if (eq > 0) {
                        baseline.put(s.substring(0, eq).trim(), s.substring(eq + 1).trim())
                    }
                }
                return
            }
        }
        return baseline
    }

    private static void maybeDump(Map<String, String> routes) {
        StringBuilder sb = new StringBuilder()
        routes.each { String fqn, String serviceId ->
            sb.append(fqn).append("=").append(serviceId).append("\n")
        }
        String props = sb.toString()

        String out = System.getProperty("api.service.routes.dump.file")
        if (out != null && !out.isEmpty()) {
            File f = new File(out)
            File parent = f.getParentFile()
            if (parent != null && !parent.exists()) {
                parent.mkdirs()
            }
            f.setText(props, "UTF-8")
            logger.info("ApiServiceIdRegistryDump wrote ${routes.size()} routes to ${f.getAbsolutePath()}")
        }
    }
}
