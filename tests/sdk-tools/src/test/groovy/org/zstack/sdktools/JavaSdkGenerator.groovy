package org.zstack.sdktools

import org.apache.commons.io.FileUtils
import org.zstack.core.Platform
import org.zstack.header.exception.CloudRuntimeException
import org.zstack.header.rest.RestRequest
import org.zstack.utils.path.PathUtil

import java.lang.reflect.Modifier

class JavaSdkGenerator {
    static void generateJavaSdk(String path) {
        def folder = new File(path)
        if (!folder.exists()) {
            folder.mkdirs()
        }

        try {
            Set<Class<?>> apiClasses = Platform.getReflections().getTypesAnnotatedWith(RestRequest.class)
                    .findAll { it.isAnnotationPresent(RestRequest.class) }
            List<SdkFile> allFiles = []
            for (Class apiClz : apiClasses) {
                if (Modifier.isAbstract(apiClz.getModifiers())) {
                    continue
                }

                SdkTemplate tmp = new SdkApiTemplate(apiClz)
                allFiles.addAll(tmp.generate())
            }

            allFiles.addAll(new SdkDataStructureGenerator().generate() as Collection<? extends SdkFile>)

            for (SdkFile f : allFiles) {
                //logger.debug(String.format("\n%s", f.getContent()));
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
