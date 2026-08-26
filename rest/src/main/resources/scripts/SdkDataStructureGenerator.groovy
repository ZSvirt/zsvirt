package scripts

import org.apache.commons.lang.StringUtils
import org.reflections.Reflections
import org.zstack.core.Platform
import org.zstack.header.exception.CloudRuntimeException
import org.zstack.header.message.Message
import org.zstack.header.query.APIQueryReply
import org.zstack.header.rest.APINoSee
import org.zstack.header.rest.NoSDK
import org.zstack.header.rest.RestResponse
import org.zstack.header.rest.SDK
import org.zstack.rest.sdk.SdkFile
import org.zstack.rest.sdk.SdkTemplate
import org.zstack.utils.FieldUtils
import org.zstack.utils.Utils
import org.zstack.utils.logging.CLogger

import java.lang.reflect.Field
import java.lang.reflect.Modifier

/**
 * Created by xing5 on 2016/12/11.
 */
class SdkDataStructureGenerator implements SdkTemplate {
    CLogger logger = Utils.getLogger(SdkDataStructureGenerator.class)

    Set<Class> responseClasses
    Map<Class, SdkFile> sdkFileMap = [:]
    Set<Class> laterResolvedClasses = []

    Map<String, String> sourceClassMap = [:]

    Reflections reflections = Platform.reflections

    SdkDataStructureGenerator() {
        Reflections reflections = Platform.getReflections()
        responseClasses = reflections.getTypesAnnotatedWith(RestResponse.class)
        laterResolvedClasses.addAll(reflections.getTypesAnnotatedWith(SDK.class)
                .findAll() { !Message.class.isAssignableFrom(it) })
    }

    @Override
    List<SdkFile> generate() {
        responseClasses.each { c ->
            try {
                generateResponseClass(c)
            } catch (Throwable t) {
                throw new CloudRuntimeException("failed to generate SDK for the class[${c.name}]", t)
            }
        }

        resolveAllClasses()

        def ret = sdkFileMap.values() as List
        ret.add(generateSourceDestClassMap())

        return ret
    }

    def generateSourceDestClassMap() {
        def srcToDst = []
        def dstToSrc = []

        sourceClassMap.each { k, v ->
            srcToDst.add("""\t\t\tput("${k}", "${v}");""")
            dstToSrc.add("""\t\t\tput("${v}", "${k}");""")
        }

        srcToDst.sort()
        dstToSrc.sort()

        SdkFile f = new SdkFile()
        f.subPath = "org/zstack/sdk"
        f.fileName = "SourceClassMap.java"
        f.content = """package org.zstack.sdk;

import java.util.HashMap;

public class SourceClassMap {
    public final static HashMap<String, String> srcToDstMapping = new HashMap() {
        {
${srcToDst.join("\n")}
        }
    };

    public final static HashMap<String, String> dstToSrcMapping = new HashMap() {
        {
${dstToSrc.join("\n")}
        }
    };
}
"""
        return f
    }

    def resolveAllClasses() {
        if (laterResolvedClasses.isEmpty()) {
            return
        }

        Set<Class> toResolve = []
        toResolve.addAll(laterResolvedClasses)

        toResolve.each { Class clz ->
            try {
                resolveClass(clz)
                laterResolvedClasses.remove(clz)
            } catch (Throwable t) {
                throw new CloudRuntimeException("failed to generate SDK for the class[${clz.getName()}]", t)
            }
        }

        resolveAllClasses()
    }

    def getTargetClassName(Class clz) {
        SDK at = clz.getAnnotation(SDK.class)
        if (at == null || at.sdkClassName().isEmpty()) {
            return clz.getSimpleName()
        }

        return at.sdkClassName()
    }

    def resolveClass(Class clz) {
        if (clz.getName().contains("\$") && !Modifier.isStatic(clz.modifiers)) {
            // ignore anonymous class
            return
        }

        if (clz.isAnnotationPresent(NoSDK.class)) {
            return
        }

        if (sdkFileMap.containsKey(clz)) {
            return
        }

        if (isZStackClass(clz.superclass)) {
            addToLaterResolvedClassesIfNeed(clz.superclass)
        }

        def output = []
        def imports = []

        if (!Enum.class.isAssignableFrom(clz)) {
            for (Field f : clz.getDeclaredFields()) {
                if (f.isAnnotationPresent(APINoSee.class)) {
                    continue
                }

                if (isZStackClass(f.type)) {
                    SDK at = f.type.getAnnotation(SDK.class)
                    String simpleName = at != null && !at.sdkClassName().isEmpty() ? at.sdkClassName() : f.type.getSimpleName()
                    imports.add("${SdkApiTemplate.getPackageName(f.type)}.${simpleName}")
                }

                def text = makeFieldText(f.name, f)
                if (text != null) {
                    output.add(text)
                }
            }
        } else {
            for (Enum e : clz.getEnumConstants()) {
                output.add("\t${e.name()},")
            }
        }

        String packageName = SdkApiTemplate.getPackageName(clz)

        SdkFile file = new SdkFile()
        file.subPath = packageName.replaceAll("\\.", "/")
        file.fileName = "${getTargetClassName(clz)}.java"
        if (!Enum.class.isAssignableFrom(clz)) {
            file.content = """package ${packageName};

${imports.collect { "import ${it};" }.join("\n")}

public class ${getTargetClassName(clz)} ${Object.class == clz.superclass ? "" : "extends " + SdkApiTemplate.getPackageName(clz.superclass) + "." + clz.superclass.simpleName} {

${output.join("\n")}
}
"""
        } else {
            file.content = """package ${packageName};

public enum ${getTargetClassName(clz)} {
${output.join("\n")}
}
"""
        }

        sourceClassMap[clz.name] = "${packageName}.${getTargetClassName(clz)}"
        sdkFileMap.put(clz, file)
    }

    def isZStackClass(Class clz) {
        if (clz.isArray() && clz.getComponentType() == byte.class) {
            return false
        }
        if (clz.getName().startsWith("java.") || int.class == clz || long.class == clz
                || short.class == clz || char.class == clz || boolean.class == clz || float.class == clz
                || double.class == clz) {
            return false
        } else if (clz.getCanonicalName().startsWith("org.zstack")) {
            return true
        } else {
            throw new CloudRuntimeException("${clz.getName()} is neither JRE class nor ZStack class")
        }
    }

    def addToLaterResolvedClassesIfNeed(Class clz) {
        if (clz.isAnnotationPresent(NoSDK.class)) {
            return
        }

        if (!sdkFileMap.containsKey(clz)) {
            laterResolvedClasses.add(clz)
        }

        Platform.reflections.getSubTypesOf(clz).forEach({ i ->
            if (!sdkFileMap.containsKey(i) && !i.isAnnotationPresent(NoSDK.class)) {
                laterResolvedClasses.add(i)
            }
        })
    }

    def generateResponseClass(Class responseClass) {
        logger.debug("generating class: ${responseClass.name}")

        RestResponse at = responseClass.getAnnotation(RestResponse.class)

        def fields = [:]

        def addToFields = { String fname, Field f ->
            if (isZStackClass(f.type)) {
                addToLaterResolvedClassesIfNeed(f.type)
                fields[fname] = f
            } else {
                fields[fname] = f
            }
        }

        if (!at.allTo().isEmpty()) {
            Field f = getFieldRecursively(responseClass, at.allTo())
            addToFields(at.allTo(), f)
        } else {
            if (at.fieldsTo().length == 1 && at.fieldsTo()[0] == "all") {
                for (Field f : responseClass.getDeclaredFields()) {
                    addToFields(f.name, f)
                }
            } else {
                at.fieldsTo().each { s ->
                    def ss = s.split("=")

                    def dst, src
                    if (ss.length == 2) {
                        dst = ss[0].trim()
                        src = ss[1].trim()
                    } else {
                        dst = src = ss[0]
                    }

                    Field f = responseClass.getDeclaredField(src)
                    addToFields(dst, f)
                }
            }
        }

        // hack
        if (APIQueryReply.class.isAssignableFrom(responseClass)) {
            addToFields("total", responseClass.superclass.getDeclaredField("total"))
        }

        def imports = []
        def output = []
        fields.each { String name, Field f ->
            if (isZStackClass(f.type)) {
                SDK sdkat = f.type.getAnnotation(SDK.class)
                String simpleName = sdkat != null && !sdkat.sdkClassName().isEmpty() ? sdkat.sdkClassName() : f.type.getSimpleName()
                imports.add("${SdkApiTemplate.getPackageName(f.type)}.${simpleName}")
            }

            def text = makeFieldText(name, f)
            if (text != null) {
                output.add(text)
            }
        }

        def className = responseClass.simpleName
        className = StringUtils.removeStart(className, "API")
        className = StringUtils.removeEnd(className, "Event")
        className = StringUtils.removeEnd(className, "Reply")
        className = StringUtils.capitalize(className)
        className = "${className}Result"

        String packageName = SdkApiTemplate.getPackageName(responseClass)
        SdkFile file = new SdkFile()
        file.subPath = packageName.replaceAll("\\.", "/")
        file.fileName = "${className}.java"
        file.content = """package ${packageName};

${imports.collect { "import ${it};" }.join("\n")}

public class ${className} {
${output.join("\n")}
}
"""
        sdkFileMap[responseClass] = file
    }

    static Field getFieldRecursively(Class<?> clazz, String fieldName) {
        try {
            return clazz.getDeclaredField(fieldName)
        } catch (NoSuchFieldException e) {
            Class<?> superClass = clazz.getSuperclass()

            if (superClass != null && superClass != Object.class) {
                return getFieldRecursively(superClass, fieldName)
            } else {
                throw e
            }
        }
    }

    def makeFieldText(String fname, Field field) {
        boolean isDeprecated = field.getDeclaredAnnotation(Deprecated.class) != null
        String annotationPrefix = isDeprecated ? "    @Deprecated\n" : ""

        // zstack type
        if (isZStackClass(field.type) || Enum.class.isAssignableFrom(field.type)) {
            addToLaterResolvedClassesIfNeed(field.type)

            return """${annotationPrefix}\
    public ${getTargetClassName(field.type)} ${fname};
    public void set${StringUtils.capitalize(fname)}(${getTargetClassName(field.type)} ${fname}) {
        this.${fname} = ${fname};
    }
    public ${getTargetClassName(field.type)} get${StringUtils.capitalize(fname)}() {
        return this.${fname};
    }
"""
        }

        // skip static fields
        if (Modifier.isStatic(field.modifiers)) {
            return null
        }

        // handle byte[] type
        if (field.type == byte[].class) {
            return """${annotationPrefix}\
    public byte[] ${fname};
    public void set${StringUtils.capitalize(fname)}(byte[] ${fname}) {
        this.${fname} = ${fname};
    }
    public byte[] get${StringUtils.capitalize(fname)}() {
        return this.${fname};
    }
"""
        }

        // java type
        if (Collection.class.isAssignableFrom(field.type)) {
            Class genericType = FieldUtils.getGenericType(field)
            if (genericType != null) {
                if (isZStackClass(genericType)) {
                    addToLaterResolvedClassesIfNeed(genericType)
                }
            }

            return """${annotationPrefix}\
    public ${field.type.name} ${fname};
    public void set${StringUtils.capitalize(fname)}(${field.type.name} ${fname}) {
        this.${fname} = ${fname};
    }
    public ${field.type.name} get${StringUtils.capitalize(fname)}() {
        return this.${fname};
    }
"""
        } else if (Map.class.isAssignableFrom(field.type)) {
            Class genericType = FieldUtils.getGenericType(field)
            if (genericType != null) {
                if (isZStackClass(genericType)) {
                    addToLaterResolvedClassesIfNeed(genericType)
                }
            }

            return """${annotationPrefix}\
    public ${field.type.name} ${fname};
    public void set${StringUtils.capitalize(fname)}(${field.type.name} ${fname}) {
        this.${fname} = ${fname};
    }
    public ${field.type.name} get${StringUtils.capitalize(fname)}() {
        return this.${fname};
    }
"""
        } else {
            return """${annotationPrefix}\
    public ${field.type.name} ${fname};
    public void set${StringUtils.capitalize(fname)}(${field.type.name} ${fname}) {
        this.${fname} = ${fname};
    }
    public ${field.type.name} get${StringUtils.capitalize(fname)}() {
        return this.${fname};
    }
"""
        }
    }
}
