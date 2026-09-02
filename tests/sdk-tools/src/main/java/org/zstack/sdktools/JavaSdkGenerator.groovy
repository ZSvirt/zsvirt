package org.zstack.sdktools

import org.apache.commons.io.FileUtils
import org.zstack.core.Platform
import org.zstack.header.exception.CloudRuntimeException
import org.zstack.header.rest.RestRequest
import org.zstack.sdk.SdkClassSourceUtils
import org.zstack.utils.path.PathUtil

import java.lang.reflect.Modifier
import java.util.function.Predicate

class JavaSdkGenerator {
    /**
     * Generate open-source Java SDK, excluding classes from zsvirt-ee.
     */
    static void generateJavaSdk(String path) {
        generateJavaSdk(path, { Class<?> c -> !SdkClassSourceUtils.isFromEnterpriseEdition(c) } as Predicate<Class<?>>, false)
    }

    /**
     * Generate enterprise Java SDK, including only classes from zsvirt-ee.
     */
    static void generateEnterpriseJavaSdk(String path) {
        generateJavaSdk(path, { Class<?> c -> SdkClassSourceUtils.isFromEnterpriseEdition(c) } as Predicate<Class<?>>, true)
    }

    static void generateJavaSdk(String path, Predicate<Class<?>> include, boolean enterpriseEdition) {
        def folder = new File(path)
        if (!folder.exists()) {
            folder.mkdirs()
        }

        try {
            Set<Class<?>> apiClasses = Platform.getReflections().getTypesAnnotatedWith(RestRequest.class)
                    .findAll { it.isAnnotationPresent(RestRequest.class) && include.test(it) }
            if (enterpriseEdition) {
                println("enterprise SDK: matched ${apiClasses.size()} @RestRequest class(es) via SdkClassSourceUtils")
                if (apiClasses.isEmpty()) {
                    println("WARNING: no EE API classes matched. Re-install zsvirt-ee jars after ZStack-Edition Manifest is set on the enterprise parent POM, or ensure EE modules resolve from zsvirt-ee/**/target/classes")
                }
            }
            List<SdkFile> allFiles = []
            for (Class apiClz : apiClasses) {
                if (Modifier.isAbstract(apiClz.getModifiers())) {
                    continue
                }

                SdkTemplate tmp = new SdkApiTemplate(apiClz)
                allFiles.addAll(tmp.generate())
            }

            allFiles.addAll(new SdkDataStructureGenerator(include, enterpriseEdition).generate() as Collection<? extends SdkFile>)

            for (SdkFile f : allFiles) {
                String fpath = PathUtil.join(path, f.getSubPath() == null ? "" : f.getSubPath(), f.getFileName())
                def dir = new File(fpath).getParentFile()
                if (!dir.exists()) {
                    dir.mkdirs()
                }
                FileUtils.writeStringToFile(new File(fpath), f.getContent())
            }
        } catch (Exception e) {
            e.printStackTrace()
            throw new CloudRuntimeException(e)
        }
    }
}
