package org.zstack.sdktools.apibinding

import javassist.Modifier
import org.apache.commons.io.FileUtils
import org.apache.commons.lang.StringUtils
import org.reflections.Reflections
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.core.type.filter.AnnotationTypeFilter
import org.springframework.core.type.filter.AssignableTypeFilter
import org.springframework.stereotype.Component
import org.springframework.stereotype.Controller
import org.zstack.configuration.PythonApiActionGenerator
import org.zstack.core.Platform
import org.zstack.core.config.GlobalConfigDefinitionScanner
import org.zstack.core.config.GlobalConfigDescription
import org.zstack.core.log.LogSafeGson
import org.zstack.core.rest.RESTApiJsonTemplateGenerator
import org.zstack.header.configuration.NoPython
import org.zstack.header.exception.CloudRuntimeException
import org.zstack.header.configuration.PythonClass
import org.zstack.header.configuration.PythonClassInventory
import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.log.NoLogging
import org.zstack.header.message.APIDeleteMessage
import org.zstack.header.message.APIEvent
import org.zstack.header.message.APIListMessage
import org.zstack.header.message.APIMessage
import org.zstack.header.message.APIParam
import org.zstack.header.message.APIReply
import org.zstack.header.message.OverriddenApiParam
import org.zstack.header.message.OverriddenApiParams
import org.zstack.header.rest.APINoSee
import org.zstack.header.rest.RestRequest
import org.zstack.header.search.APIGetMessage
import org.zstack.header.search.APISearchMessage
import org.zstack.utils.CollectionUtils
import org.zstack.utils.FieldUtils
import org.zstack.utils.TypeUtils
import org.zstack.utils.Utils
import org.zstack.utils.logging.CLogger
import org.zstack.utils.path.PathUtil
import org.zstack.zql.ZQLPythonWriter

import java.lang.reflect.Field
import java.util.stream.Collectors

/**
 * Offline copy of {@code ConfigurationManagerImpl.generateApiJsonTemplate}
 * (json templates + python/inventory.py + api_messages.py + api_actions.py).
 * <p>
 * Uses Spring's {@link ClassPathScanningCandidateComponentProvider} as a
 * classpath scan library, not the MN Spring container. Do not call
 * {@code Platform.getComponentLoader()}.
 */
class Python2ApiBindingGenerator {
    private static final CLogger logger = Utils.getLogger(Python2ApiBindingGenerator.class)

    private Set<String> generatedPythonClassName

    void generate(String exportPath, List<String> basePkgs) {
        String pythonApisDir = System.getProperty("pythonApisDir")
        if (pythonApisDir == null) {
            pythonApisDir = System.getProperty("user.home")
        }
        generatedPythonClassName = new HashSet<>()
        if (exportPath == null) {
            exportPath = PathUtil.join(pythonApisDir, "zstack-python-template")
        }
        if (basePkgs == null || basePkgs.isEmpty()) {
            basePkgs = new ArrayList<>(1)
            basePkgs.add("org.zstack")
        }

        FileUtils.deleteDirectory(new File(exportPath))
        File folder = new File(exportPath)
        folder.mkdirs()
        File jsonFolder = new File(PathUtil.join(folder.getAbsolutePath(), "json"))
        jsonFolder.mkdirs()
        File pythonFolder = new File(PathUtil.join(folder.getAbsolutePath(), "python"))
        pythonFolder.mkdirs()
        ensureDirectory(folder)
        ensureDirectory(jsonFolder)
        ensureDirectory(pythonFolder)

        // write api_messages.py
        StringBuilder apiNameBuilder = new StringBuilder()
        apiNameBuilder.append("api_names = [\n")
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false)
        scanner.addIncludeFilter(new AssignableTypeFilter(APIEvent.class))
        scanner.addIncludeFilter(new AssignableTypeFilter(APIReply.class))
        scanner.addIncludeFilter(new AssignableTypeFilter(APIMessage.class))
        scanner.addExcludeFilter(new AnnotationTypeFilter(Controller.class))
        scanner.addExcludeFilter(new AnnotationTypeFilter(Component.class))
        for (String pkg : basePkgs) {
            for (BeanDefinition bd : scanner.findCandidateComponents(pkg)) {
                Class<?> clazz = null
                try {
                    clazz = Class.forName(bd.getBeanClassName())
                    logger.debug(String.format("dumping message: %s", bd.getBeanClassName()))
                    String template = RESTApiJsonTemplateGenerator.dump(clazz)
                    FileUtils.write(new File(PathUtil.join(jsonFolder.getAbsolutePath(),
                            clazz.getName() + ".json")), template)
                } catch (Exception e) {
                    logger.warn(String.format("Unable to generate json template for %s", bd.getBeanClassName()), e)
                }

                if (clazz != null && APIMessage.class.isAssignableFrom(clazz)) {
                    if (TypeUtils.isTypeOf(clazz, APISearchMessage.class, APIGetMessage.class, APIListMessage.class)) {
                        continue
                    }
                    apiNameBuilder.append(String.format("%s'%s',\n", whiteSpace(4), clazz.getName()))
                }
            }
        }
        apiNameBuilder.append("]\n")
        FileUtils.write(new File(PathUtil.join(pythonFolder.getAbsolutePath(), "api_messages.py")),
                apiNameBuilder.toString())

        // write inventory.py
        StringBuilder pysb = new StringBuilder()
        pysb.append("#-*-coding: UTF-8 -*-\n")
        pysb.append("from zstacklib.utils import log")
        generateMandatoryFieldClass(pysb)
        generateApiMessagePythonClass(pysb, basePkgs)
        generateInventoryPythonClass(pysb, basePkgs)
        generateConstantPythonClass(pysb, basePkgs)
        generateGlobalConfigPythonConstant(pysb)
        Python2QueryObjectWriter.writePython(pysb)
        pysb.append("\n")
        new ZQLPythonWriter().writePython(pysb)
        String pyStr = pysb.toString()
        FileUtils.write(new File(PathUtil.join(pythonFolder.getAbsolutePath(), "inventory.py")), pyStr)

        // write api_actions.py
        PythonApiActionGenerator.generatePythonApiAction(basePkgs, pythonFolder.getAbsolutePath())

        logger.info(String.format("Generated result in %s", folder.getAbsolutePath()))
        generatedPythonClassName = null
    }

    private static void ensureDirectory(File dir) {
        if (!dir.isDirectory()) {
            throw new CloudRuntimeException(String.format("failed to create directory: %s", dir.getAbsolutePath()))
        }
    }

    private static String whiteSpace(int num) {
        return StringUtils.repeat(" ", num)
    }

    private String classToInventoryPythonClass(Class<?> clazz) {
        StringBuilder sb = new StringBuilder()
        boolean hasParent = (clazz.getSuperclass() != Object.class)

        if (hasParent && !isPythonClassGenerated(clazz.getSuperclass())) {
            sb.append(classToInventoryPythonClass(clazz.getSuperclass()))
        }

        if (hasParent) {
            sb.append(String.format("\nclass %s(%s):", clazz.getSimpleName(), clazz.getSuperclass().getSimpleName()))
            sb.append(String.format("\n%sdef __init__(self):", whiteSpace(4)))
            sb.append(String.format("\n%ssuper(%s, self).__init__()", whiteSpace(8), clazz.getSimpleName()))
        } else {
            sb.append(String.format("\nclass %s(object):", clazz.getSimpleName()))
            sb.append(String.format("\n%sdef __init__(self):", whiteSpace(4)))
        }

        Field[] fs = clazz.getDeclaredFields()
        for (Field f : fs) {
            sb.append(String.format("\n%sself.%s = None", whiteSpace(8), f.getName()))
        }

        sb.append(String.format("\n\n%sdef evaluate(self, inv):", whiteSpace(4)))
        if (hasParent) {
            sb.append(String.format("\n%ssuper(%s, self).evaluate(inv)", whiteSpace(8), clazz.getSimpleName()))
        }
        for (Field f : fs) {
            sb.append(String.format("\n%sif hasattr(inv, '%s'):", whiteSpace(8), f.getName()))
            sb.append(String.format("\n%sself.%s = inv.%s", whiteSpace(12), f.getName(), f.getName()))
            sb.append(String.format("\n%selse:", whiteSpace(8)))
            sb.append(String.format("\n%sself.%s = None\n", whiteSpace(12), f.getName()))
        }

        sb.append("\n\n")
        markPythonClassAsGenerated(clazz)
        return sb.toString()
    }

    private String classToApiMessagePythonClass(Class<?> clazz) {
        return classToApiMessagePythonClass(clazz, null)
    }

    private String classToApiMessagePythonClass(Class<?> clazz, RestRequest request) {
        StringBuilder result = new StringBuilder()
        boolean emptyLine = true

        String signature = String.format("%s_FULL_NAME", clazz.getSimpleName()).toUpperCase()
        result.append(String.format("\n%s = '%s'", signature, clazz.getName()))
        result.append(String.format("\nclass %s(object):", clazz.getSimpleName()))
        result.append(String.format("\n%sFULL_NAME='%s'", whiteSpace(4), clazz.getName()))
        if (request != null) {
            result.append(String.format("\n%sRESPONSE_NAME='%s'", whiteSpace(4), request.responseClass().getSimpleName()))
        }

        StringBuilder sb = new StringBuilder()
        List<Field> fs = FieldUtils.getAllFields(clazz)
        Map<String, NoLogging.Type> sensitiveFields = new HashMap<>()
        for (Field f : fs) {
            APINoSee nosee = f.getAnnotation(APINoSee.class)
            if (nosee != null && f.getName() != "timeout" && f.getName() != "session") {
                continue
            }

            APIParam at = f.getAnnotation(APIParam.class)
            OverriddenApiParams o = clazz.getAnnotation(OverriddenApiParams.class)
            if (o != null) {
                for (OverriddenApiParam op : o.value()) {
                    if (op.field() == f.getName()) {
                        at = op.param()
                        break
                    }
                }
            }

            NoLogging noLogging = f.getAnnotation(NoLogging.class)
            if (noLogging != null) {
                sensitiveFields.put(f.getName(), noLogging.type())
            }

            if (at != null && at.required()) {
                sb.append(String.format("\n%s#mandatory field", whiteSpace(8)))
            }
            if (at != null && at.validValues().length != 0) {
                List<String> values = new ArrayList<>(at.validValues().length)
                Collections.addAll(values, at.validValues())
                sb.append(String.format("\n%s#valid values: %s", whiteSpace(8), values))
            } else if (at != null && at.validEnums().length != 0) {
                List<String> values = CollectionUtils.valuesForEnums(at.validEnums()).collect(Collectors.toList())
                sb.append(String.format("\n%s#valid values: %s", whiteSpace(8), values))
            }
            if (at != null && at.validRegexValues() != null && at.validRegexValues().trim() != "") {
                String regex = at.validRegexValues().trim()
                sb.append(String.format("\n%s#valid regex values: %s", whiteSpace(8), regex))
            }

            if (Collection.class.isAssignableFrom(f.getType())) {
                if (at != null && at.required()) {
                    sb.append(String.format("\n%sself.%s = NotNoneList()", whiteSpace(8), f.getName()))
                } else {
                    sb.append(String.format("\n%sself.%s = OptionalList()", whiteSpace(8), f.getName()))
                }
            } else if (Map.class.isAssignableFrom(f.getType())) {
                if (at != null && at.required()) {
                    sb.append(String.format("\n%sself.%s = NotNoneMap()", whiteSpace(8), f.getName()))
                } else {
                    sb.append(String.format("\n%sself.%s = OptionalMap()", whiteSpace(8), f.getName()))
                }
            } else {
                if (at != null && at.required()) {
                    sb.append(String.format("\n%sself.%s = NotNoneField()", whiteSpace(8), f.getName()))
                } else {
                    sb.append(String.format("\n%sself.%s = None", whiteSpace(8), f.getName()))
                }
            }

            emptyLine = false
        }

        if (emptyLine) {
            sb.append(String.format("\n%spass", whiteSpace(8)))
        }

        if (!sensitiveFields.isEmpty()) {
            result.append(String.format("\n%s@log.sensitive_fields(\"%s\")", whiteSpace(4),
                    String.join("\", \"", sensitiveFields.keySet())))
        }

        result.append(String.format("\n%sdef __init__(self):", whiteSpace(4)))
        result.append(sb.toString())
        result.append("\n\n")
        markPythonClassAsGenerated(clazz)
        return result.toString()
    }

    private String classToApiEventPythonClass(Class<?> clazz) {
        Map<String, NoLogging.Type> sensitiveFields = LogSafeGson.getSensitiveFields(clazz)
        if (sensitiveFields.isEmpty()) {
            return ""
        }

        StringBuilder result = new StringBuilder()
        result.append(String.format("\nclass %s(object):", clazz.getSimpleName()))
        result.append(String.format("\n%sFULL_NAME='%s'", whiteSpace(4), clazz.getName()))
        result.append(String.format("\n%s@log.sensitive_fields(\"%s\")", whiteSpace(4),
                String.join("\", \"", sensitiveFields.keySet())))
        result.append(String.format("\n%sdef __init__(self):", whiteSpace(4)))
        result.append(String.format("\n%spass", whiteSpace(8)))
        result.append("\n\n")
        markPythonClassAsGenerated(clazz)
        return result.toString()
    }

    private static void generateSimplePythonClass(StringBuilder sb, Class<?> clazz) {
        sb.append(String.format("\nclass %s(object):", clazz.getSimpleName()))
        sb.append(String.format("\n%sdef __init__(self):", whiteSpace(4)))
        for (Field f : clazz.getDeclaredFields()) {
            sb.append(String.format("\n%sself.%s = None", whiteSpace(8), f.getName()))
        }
        sb.append("\n\n")
    }

    private static void generateErrorCodePythonClass(StringBuilder sb) {
        generateSimplePythonClass(sb, ErrorCode.class)
    }

    private static void generateSessionPythonClass(StringBuilder sb) {
        sb.append(String.format("\nclass Session(object):"))
        sb.append(String.format("\n%sdef __init__(self):", whiteSpace(4)))
        sb.append(String.format("\n%sself.uuid = None", whiteSpace(8)))
        sb.append("\n\n")
    }

    private static void generateSearchConditionClass(StringBuilder sb) {
        generateSimplePythonClass(sb, APISearchMessage.NOLTriple.class)
        generateSimplePythonClass(sb, APISearchMessage.NOVTriple.class)
    }

    private static void generateMandatoryFieldClass(StringBuilder sb) {
        sb.append(String.format("\n\nclass NotNoneField(object):"))
        sb.append(String.format("\n%spass\n", whiteSpace(4)))

        sb.append(String.format("\n\nclass NotNoneList(object):"))
        sb.append(String.format("\n%spass\n", whiteSpace(4)))

        sb.append(String.format("\n\nclass OptionalList(object):"))
        sb.append(String.format("\n%spass\n", whiteSpace(4)))

        sb.append(String.format("\n\nclass NotNoneMap(object):"))
        sb.append(String.format("\n%spass\n", whiteSpace(4)))

        sb.append(String.format("\n\nclass OptionalMap(object):"))
        sb.append(String.format("\n%spass\n", whiteSpace(4)))
    }

    private void generateBaseApiMessagePythonClass(StringBuilder sb) {
        sb.append(String.format("\nclass %s(object):", APIMessage.class.getSimpleName()))
        sb.append(String.format("\n%sdef __init__(self):", whiteSpace(4)))
        sb.append(String.format("\n%ssuper(%s, self).__init__()", whiteSpace(8), APIMessage.class.getSimpleName()))
        sb.append(String.format("\n%sself.timeout = None", whiteSpace(8)))
        for (Field f : APIMessage.class.getDeclaredFields()) {
            sb.append(String.format("\n%sself.%s = None", whiteSpace(8), f.getName()))
        }
        sb.append("\n\n")
        markPythonClassAsGenerated(APIMessage.class)
        sb.append(classToApiMessagePythonClass(APIDeleteMessage.class))
        markPythonClassAsGenerated(APIDeleteMessage.class)
        generateSearchConditionClass(sb)
    }

    private static void generateApiNameList(StringBuilder sb, List<String> apiNames) {
        Collections.sort(apiNames)
        sb.append(String.format("\napi_names = ["))
        for (String name : apiNames) {
            sb.append(String.format("\n%s'%s',", whiteSpace(4), name))
        }
        sb.append("\n]\n")
    }

    private void generateApiMessagePythonClass(StringBuilder sb, List<String> basePkgs) {
        generateSessionPythonClass(sb)
        generateErrorCodePythonClass(sb)
        generateBaseApiMessagePythonClass(sb)
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(true)
        scanner.addIncludeFilter(new AssignableTypeFilter(APIMessage.class))
        scanner.addIncludeFilter(new AssignableTypeFilter(APIReply.class))
        scanner.addExcludeFilter(new AnnotationTypeFilter(Controller.class))
        scanner.addExcludeFilter(new AnnotationTypeFilter(NoPython.class))
        scanner.addExcludeFilter(new AnnotationTypeFilter(Component.class))
        List<String> apiNames = new ArrayList<>(100)
        for (String pkg : basePkgs) {
            for (BeanDefinition bd : scanner.findCandidateComponents(pkg).stream().sorted({ bd1, bd2 ->
                return bd1.getBeanClassName() <=> bd2.getBeanClassName()
            }).collect(Collectors.toList())) {
                try {
                    Class<?> clazz = Class.forName(bd.getBeanClassName())
                    if (clazz == APIMessage.class || clazz == APIListMessage.class || clazz == APIDeleteMessage.class || clazz == APISearchMessage.class) {
                        continue
                    }
                    if (TypeUtils.isTypeOf(clazz, APISearchMessage.class, APIListMessage.class, APIGetMessage.class)) {
                        continue
                    }
                    if (Modifier.isAbstract(clazz.getModifiers())) {
                        continue
                    }
                    RestRequest request
                    if ((request = clazz.getAnnotation(RestRequest.class)) == null) {
                        continue
                    }
                    if (isPythonClassGenerated(clazz)) {
                        /* This class was generated as other's parent class */
                        continue
                    }
                    sb.append(classToApiMessagePythonClass(clazz, request))
                    sb.append(classToApiEventPythonClass(request.responseClass()))
                    apiNames.add(clazz.getSimpleName())
                } catch (ClassNotFoundException e) {
                    logger.warn(String.format("Unable to generate python class for %s", bd.getBeanClassName()), e)
                }
            }
        }

        apiNames.remove(APIMessage.class.getSimpleName())
        apiNames.remove(APIListMessage.class.getSimpleName())
        apiNames.remove(APIDeleteMessage.class.getSimpleName())
        apiNames.remove(APISearchMessage.class.getSimpleName())
        generateApiNameList(sb, apiNames)
    }

    private static void generateConstantFromClassField(StringBuilder sb, Class<?> clazz) {
        sb.append("\n#").append(clazz.getSimpleName())
        if (clazz.isEnum()) {
            for (Field f : clazz.getDeclaredFields()) {
                if (f.isEnumConstant()) {
                    String name = f.getName().toUpperCase()
                    f.setAccessible(true)
                    String value = f.get(null).toString()
                    sb.append(String.format("\n%s = '%s'", name, value))
                }
            }
        } else {
            for (Field f : clazz.getDeclaredFields()) {
                PythonClass at = f.getAnnotation(PythonClass.class)
                if (at == null) {
                    continue
                }

                String name = f.getName().toUpperCase()
                f.setAccessible(true)
                String value = f.get(null).toString()
                sb.append(String.format("\n%s = '%s'", name, value))
            }
        }
        sb.append("\n")
    }

    private static void generateConstantPythonClass(StringBuilder sb, List<String> basePkgs) {
        Reflections reflections = Platform.getReflections()
        Set<Class<?>> annotated = reflections.getTypesAnnotatedWith(PythonClass.class)
        for (Class<?> clazz : annotated.stream().sorted({ c1, c2 ->
            return c1.getSimpleName() <=> c2.getSimpleName()
        }).collect(Collectors.toList())) {
            try {
                generateConstantFromClassField(sb, clazz)
            } catch (Exception e) {
                logger.warn(String.format("Unable to generate python class for %s", clazz.getName()), e)
            }
        }
    }

    private void generateInventoryPythonClass(StringBuilder sb, List<String> basePkgs) {
        List<String> inventoryPython = new ArrayList<>()
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false)
        scanner.addIncludeFilter(new AnnotationTypeFilter(PythonClassInventory.class))
        scanner.addExcludeFilter(new AnnotationTypeFilter(Component.class))
        for (String pkg : basePkgs) {
            for (BeanDefinition bd : scanner.findCandidateComponents(pkg).stream().sorted({ bd1, bd2 ->
                return bd1.getBeanClassName() <=> bd2.getBeanClassName()
            }).collect(Collectors.toList())) {
                try {
                    Class<?> clazz = Class.forName(bd.getBeanClassName())
                    if (isPythonClassGenerated(clazz)) {
                        /* This class was generated as other's parent class */
                        continue
                    }
                    inventoryPython.add(classToInventoryPythonClass(clazz))
                } catch (Exception e) {
                    logger.warn(String.format("Unable to generate python class for %s", bd.getBeanClassName()), e)
                }
            }
        }

        for (String invstr : inventoryPython) {
            sb.append(invstr)
        }
    }

    private boolean isPythonClassGenerated(Class<?> clazz) {
        return generatedPythonClassName.contains(clazz.getSimpleName())
    }

    private void markPythonClassAsGenerated(Class<?> clazz) {
        generatedPythonClassName.add(clazz.getSimpleName())
    }

    private static void generateGlobalConfigPythonConstant(StringBuilder pysb) {
        pysb.append("\n#GlobalConfigPythonConstant")
        Map<String, List<String>> configs = new HashMap<>()
        for (GlobalConfigDescription d : GlobalConfigDefinitionScanner.scan()) {
            List<String> cnames = configs.get(d.category)
            if (cnames == null) {
                cnames = new ArrayList<>()
                configs.put(d.category, cnames)
            }
            cnames.add(d.name)
        }

        for (Map.Entry<String, List<String>> e : configs.entrySet()
                .stream()
                .sorted({ e1, e2 ->
                    return e1.getKey().toUpperCase() <=> e2.getKey().toUpperCase()
                })
                .collect(Collectors.toList())) {
            pysb.append(String.format("\nclass GlobalConfig_%s(object):", e.getKey().toUpperCase().replaceAll("\\.", "_")))
            for (String cname : e.getValue()) {
                String var = cname.replaceAll("\\.", "_").replaceAll("-", "_")
                pysb.append(String.format("\n%s%s = '%s'", whiteSpace(4), var.toUpperCase(), cname))
            }
            pysb.append("\n")
            pysb.append(String.format("\n%s@staticmethod", whiteSpace(4)))
            pysb.append(String.format("\n%sdef get_category():", whiteSpace(4)))
            pysb.append(String.format("\n%sreturn '%s'\n", whiteSpace(8), e.getKey()))
        }
    }
}
