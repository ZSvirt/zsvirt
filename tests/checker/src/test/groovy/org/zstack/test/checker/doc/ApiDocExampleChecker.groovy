package org.zstack.test.checker.doc

import org.junit.Test
import org.zstack.header.Constants
import org.zstack.header.configuration.PythonClassInventory
import org.zstack.header.exception.CloudRuntimeException
import org.zstack.header.message.APIEvent
import org.zstack.header.message.APIMessage
import org.zstack.header.message.APIReply
import org.zstack.header.message.DocUtils
import org.zstack.header.message.Message
import org.zstack.header.search.Inventory
import org.zstack.utils.BeanUtils
import org.zstack.utils.Utils
import org.zstack.utils.gson.JSONObjectUtil
import org.zstack.utils.logging.CLogger

import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.util.concurrent.TimeUnit

class ApiDocExampleChecker {
    private static final CLogger logger = Utils.getLogger(ApiDocExampleChecker.class)

    private static final int BATCH_SIZE = 1000
    private static final long ROUND_INTERVAL_MILLIS = TimeUnit.SECONDS.toMillis(2)

    @Test
    void testApiDocExample() {
        logger.info("start ApiDocExampleChecker")
        System.setProperty(Constants.UUID_FOR_EXAMPLE, "true")

        List<Class> targets = collectTargetClasses()
        logger.info("ApiDocExampleChecker scanned ${targets.size()} class(es)")

        List<Class> withExample = []
        int missing = 0
        for (Class clz : targets) {
            Method method = findExampleMethod(clz)
            if (method == null) {
                logger.warn("class[${clz.name}] doesn't have static __example__ method")
                missing++
                continue
            }
            withExample.add(clz)
        }

        List<String> mismatches = []
        int batches = withExample.isEmpty() ? 0 : (withExample.size() + BATCH_SIZE - 1).intdiv(BATCH_SIZE)
        for (int batchIndex = 0; batchIndex < batches; batchIndex++) {
            int from = batchIndex * BATCH_SIZE
            int to = Math.min(from + BATCH_SIZE, withExample.size())
            List<Class> batch = withExample.subList(from, to)
            logger.info("ApiDocExampleChecker batch ${batchIndex + 1}/${batches}, size=${batch.size()}")

            Map<Class, String> firstRound = new LinkedHashMap<>()
            for (Class clz : batch) {
                firstRound.put(clz, invokeAndSnapshot(clz))
            }

            Thread.sleep(ROUND_INTERVAL_MILLIS)

            for (Class clz : batch) {
                String second = invokeAndSnapshot(clz)
                String first = firstRound.get(clz)
                if (first != second) {
                    mismatches.add("${clz.name}: first=${first}, second=${second}")
                    logger.error("class[${clz.name}] __example__() is not stable between two invocations")
                }
            }
            firstRound.clear()
        }

        if (!mismatches.isEmpty()) {
            throw new CloudRuntimeException(
                    "ApiDocExampleChecker failed: ${mismatches.size()} __example__() result(s) differ after ${ROUND_INTERVAL_MILLIS}ms. " +
                            "Use DocUtils.timestamp()/uuid() instead of current time or random uuid. Sample: " +
                            mismatches.take(10).join("; ") + (mismatches.size() > 10 ? "; ..." : ""))
        }

        logger.info("ApiDocExampleChecker - PASS, classes=${targets.size()}, withExample=${withExample.size()}, missing=${missing}")
    }

    private static List<Class> collectTargetClasses() {
        Set<Class> classes = new LinkedHashSet<>()
        classes.addAll(APIMessage.apiMessageClasses)
        classes.addAll(BeanUtils.reflections.getSubTypesOf(APIMessage.class))
        classes.addAll(BeanUtils.reflections.getSubTypesOf(APIReply.class))
        classes.addAll(BeanUtils.reflections.getSubTypesOf(APIEvent.class))
        classes.add(APIReply.class)
        classes.add(APIEvent.class)
        classes.addAll(BeanUtils.reflections.getTypesAnnotatedWith(Inventory.class))
        BeanUtils.reflections.getTypesAnnotatedWith(PythonClassInventory.class).each { Class clz ->
            if (clz.simpleName.endsWith("Inventory")) {
                classes.add(clz)
            }
        }

        return classes.findAll { Class clz -> usableClass(clz) }.sort { it.name }
    }

    private static boolean usableClass(Class clz) {
        if (clz == null || clz.isInterface() || clz.isAnonymousClass() || clz.isLocalClass()) {
            return false
        }
        if (Modifier.isAbstract(clz.modifiers)) {
            return false
        }
        String name = clz.name
        if (name.startsWith("org.zstack.sdk.") || name.startsWith("org.zstack.test.")) {
            return false
        }
        return true
    }

    private static Method findExampleMethod(Class clz) {
        Method method
        try {
            method = clz.getDeclaredMethod("__example__")
        } catch (NoSuchMethodException ignored) {
            return null
        }
        if (!Modifier.isPublic(method.modifiers) || !Modifier.isStatic(method.modifiers)) {
            throw new CloudRuntimeException("__example__() of ${clz.name} must be public static")
        }
        return method
    }

    private static String invokeAndSnapshot(Class clz) {
        Method method = findExampleMethod(clz)
        clearApiUuidMap()
        def obj = method.invoke(null)

        if (obj instanceof Message) {
            obj.id = ""
            obj.createdTime = 0L
        }

        return JSONObjectUtil.toJsonString(obj)
    }

    private static void clearApiUuidMap() {
        Field field = DocUtils.class.getDeclaredField("apiUuidMap")
        field.setAccessible(true)
        ((Map) field.get(null)).clear()
    }
}
