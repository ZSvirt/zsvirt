package scripts


import com.google.common.io.Resources
import groovy.json.JsonBuilder
import org.apache.commons.io.Charsets
import org.apache.commons.io.FileUtils
import org.apache.commons.lang.StringUtils
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.select.Elements
import org.markdown4j.Markdown4jProcessor
import org.springframework.http.HttpMethod
import org.zstack.core.Platform
import org.zstack.core.config.*
import org.zstack.core.errorcode.ErrorFacadeImpl
import org.zstack.header.core.NoDoc
import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.description.route.ApiRouteUtils
import org.zstack.header.exception.CloudRuntimeException
import org.zstack.header.identity.SuppressCredentialCheck
import org.zstack.header.message.APIMessage
import org.zstack.header.message.APIMessage.InvalidApiMessageException
import org.zstack.header.message.APIParam
import org.zstack.header.message.DocUtils
import org.zstack.header.query.APIQueryMessage
import org.zstack.header.rest.APINoSee
import org.zstack.header.rest.RestRequest
import org.zstack.header.rest.RestResponse
import org.zstack.resourceconfig.BindResourceConfig
import org.zstack.rest.RestConstants
import org.zstack.rest.sdk.DocumentGenerator
import org.zstack.utils.*
import org.zstack.utils.data.StringTemplate
import org.zstack.utils.gson.JSONObjectUtil
import org.zstack.utils.logging.CLogger
import org.zstack.utils.path.PathUtil

import javax.xml.bind.JAXBContext
import javax.xml.bind.JAXBException
import javax.xml.bind.Unmarshaller
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.nio.file.Paths
import java.util.regex.Matcher
import java.util.regex.Pattern
import java.util.stream.Collectors

/**
 * Created by xing5 on 2016/12/21.
 */
class RestDocumentationGenerator implements DocumentGenerator {
    static CLogger logger = Utils.getLogger(DocumentGenerator.class)

    final String PLACEHOLDER = "##GLOBALCONFIGCONTENT"
    final String DEPRECATED = "#Deprecated"
    GlobalConfigInitializer initializer = new GlobalConfigInitializer()

    class GlobalConfigInitializer {
        JAXBContext context
        Map<String, String> validatorMap = new HashMap<>()
        Map<String, Class[]> bindResources = new HashMap<>()
        Map<String, GlobalConfig> configs = new HashMap<>()
        List<Field> globalConfigFields = new ArrayList<>()
        Map<String, String> propertiesMap = new HashMap<>()

        GlobalConfigInitializer() {
            init()
        }

        void init() {
            try {
                loadSystemProperties()
                loadConfigFromXml()
                parseGlobalConfigFields()
                loadConfigFromJava()
                link()
            } catch (IllegalArgumentException ie) {
                throw ie
            } catch (Exception e) {
                throw new CloudRuntimeException(e)
            }
        }

        private void loadSystemProperties() {
            boolean noTrim = System.getProperty("DoNotTrimPropertyFile") != null
            for (final String name : System.getProperties().stringPropertyNames()) {
                String value = System.getProperty(name)
                if (!noTrim) {
                    value = value.trim()
                }
                propertiesMap.put(name, value)
            }
        }

        private void parseGlobalConfigFields() {
            Set<Class<?>> definitionClasses = BeanUtils.reflections.getTypesAnnotatedWith(GlobalConfigDefinition.class)
            definitionClasses.each {
                for (Field field : it.getDeclaredFields()) {
                    if (Modifier.isStatic(field.getModifiers()) && GlobalConfig.class.isAssignableFrom(field.getType())) {
                        field.setAccessible(true)
                        try {
                            def bind = field.getAnnotation(BindResourceConfig.class)
                            def validator = field.getAnnotation(GlobalConfigValidation.class)
                            def configBef = field.getAnnotation(GlobalConfigDef.class)
                            GlobalConfig config = (GlobalConfig) field.get(null)
                            if (config == null) {
                                throw new CloudRuntimeException(String.format("GlobalConfigDefinition[%s] " +
                                        "defines a null GlobalConfig[%s]." +
                                        "You must assign a value to it using new GlobalConfig(category, name)",
                                        it.getName(), field.getName()))
                            }
                            if (bind != null) {
                                bindResources.put(config.getIdentity(), bind.value())
                            }

                            globalConfigFields.add(field)
                            if (validator == null) {
                                continue
                            }
                            if (!validator.notNull()) {
                                continue
                            }
                            if (!validator.notEmpty()) {
                                validatorMap.put(config.getIdentity(), "notNull")
                                continue
                            }
                            if (validator.validValues().length != 0) {
                                validatorMap.put(config.getIdentity(), "{"
                                        + validator.validValues().join(", ") + "}")
                                continue
                            }

                            GlobalConfig conf = configs.get(config.getIdentity())
                            boolean isInteger = (conf != null && conf.type == Integer.class.getName()) ||
                                    (configBef != null && configBef.type() == Integer.class)

                            if (validator.min() == Long.MIN_VALUE
                                    && validator.max() == Long.MAX_VALUE) {
                                if (validator.inNumberRange().length != 0) {
                                    validatorMap.put(config.getIdentity(), "["
                                            + validator.inNumberRange().join(" ,") + "]")
                                } else {
                                    validatorMap.put(config.getIdentity(),
                                            generateRangeString(isInteger, validator.min(),
                                                    validator.max()))
                                }
                                continue
                            }
                            if (validator.min() != Long.MIN_VALUE
                                    && validator.max() != Long.MAX_VALUE) {
                                if (validator.min() > validator.max()) {
                                    throw new CloudRuntimeException(
                                            String.format("The globalConfigValidation of " +
                                                    "GlobalConfig[%s] is illegal." +
                                                    "Please check the value range.",
                                                    config.getName()))
                                }
                                validatorMap.put(config.getIdentity(), "["
                                        + validator.min().toString() + ", "
                                        + validator.max().toString() + "]")
                            } else {
                                validatorMap.put(config.getIdentity(),
                                        generateRangeString(isInteger, validator.min(),
                                                validator.max()))
                            }

                        } catch (IllegalAccessException e) {
                            throw new CloudRuntimeException(e)
                        }
                    }
                }
            }
        }

        String generateRangeString(boolean isInteger, long greaterThan, long lessThan) {
            String typeRange = isInteger ? "${Integer.MIN_VALUE},${Integer.MAX_VALUE}" : "${Long.MIN_VALUE},${Long.MAX_VALUE}"
            return "[" + (greaterThan != Long.MIN_VALUE ? greaterThan : typeRange.split(",")[0]) + ", " +
                    (lessThan != Long.MAX_VALUE ? lessThan : typeRange.split(",")[1]) + "]"
        }

        private void loadConfigFromJava() {
            for (Field field : globalConfigFields) {
                try {
                    GlobalConfig config = (GlobalConfig) field.get(null)
                    if (config == null) {
                        throw new CloudRuntimeException(String.format("GlobalConfigDefinition[%s] " +
                                "defines a null GlobalConfig[%s]." +
                                "You must assign a value to it using new GlobalConfig(category, name)",
                                field.getDeclaringClass().getName(), field.getName()));
                    }

                    GlobalConfigDef d = field.getAnnotation(GlobalConfigDef.class)
                    if (d == null) {
                        continue
                    }
                    if (d.defaultValue() == null) {
                        throw new CloudRuntimeException("The default value of ${config.name} is null")
                    }
                    String defaultValue = StringTemplate.substitute(d.defaultValue(), propertiesMap)

                    Class clz = Class.forName("org.zstack.core.config.GlobalConfig")
                    GlobalConfig c = clz.newInstance() as GlobalConfig
                    c.setCategory(config.getCategory())
                    c.setName(config.getName())
                    c.setDescription(d.description())
                    c.setDefaultValue(defaultValue)
                    c.setValue(defaultValue)
                    c.setType(d.type().getName())
                    if ("" != d.validatorRegularExpression()) {
                        c.setValidatorRegularExpression(d.validatorRegularExpression())
                    }

                    if (configs.containsKey(c.getIdentity())) {
                        throw new CloudRuntimeException(String.format("duplicate global configuration. %s defines a" +
                                " global config[category: %s, name: %s] that has been defined by a XML configure or" +
                                " another java class", field.getDeclaringClass().getName(), c.getCategory(), c.getName()))
                    }

                    configs.put(c.getIdentity(), c)
                } catch (IllegalAccessException e) {
                    throw new CloudRuntimeException(e)
                }
            }
        }


        private void loadConfigFromXml() throws JAXBException {
            context = JAXBContext.newInstance("org.zstack.core.config.schema")
            List<String> filePaths = PathUtil.scanFolderOnClassPath("globalConfig")
            for (String path : filePaths) {
                File f = new File(path)
                parseConfig(f)
            }
        }

        private void parseConfig(File file) throws JAXBException {
            if (!file.getName().endsWith("xml")) {
                logger.warn(String.format("file[%s] in global config folder is not end with .xml, skip it",
                        file.getAbsolutePath()))
                return
            }

            Unmarshaller unmarshaller = context.createUnmarshaller()
            org.zstack.core.config.schema.GlobalConfig gb =
                    (org.zstack.core.config.schema.GlobalConfig) unmarshaller.unmarshal(file)
            for (org.zstack.core.config.schema.GlobalConfig.Config c : gb.getConfig()) {
                String category = c.getCategory()
                category = category == null ? "Others" : category;
                c.setCategory(category);
                if (c.getDefaultValue() == null) {
                    throw new IllegalArgumentException(
                            String.format("GlobalConfig[category:%s, name:%s] must have a default value",
                                    c.getCategory(), c.getName()));
                } else {
                    c.setDefaultValue(StringTemplate.substitute(c.getDefaultValue(), propertiesMap))
                }
                if (c.getValue() == null) {
                    c.setValue(c.getDefaultValue())
                } else {
                    c.setValue(StringTemplate.substitute(c.getValue(), propertiesMap))
                }
                GlobalConfig config = GlobalConfig.valueOf(c)
                if (configs.containsKey(config.getIdentity())) {
                    throw new IllegalArgumentException(
                            String.format("duplicate GlobalConfig[category: %s, name: %s]",
                                    config.getCategory(), config.getName()))
                }
                configs.put(config.getIdentity(), config)
            }
        }

        private void link() {
            for (Field field : globalConfigFields) {
                field.setAccessible(true)
                try {
                    GlobalConfig config = (GlobalConfig) field.get(null)
                    if (config == null) {
                        throw new CloudRuntimeException(String.format("GlobalConfigDefinition[%s] " +
                                "defines a null GlobalConfig[%s]." +
                                "You must assign a value to it using new GlobalConfig(category, name)",
                                field.getDeclaringClass().getName(), field.getName()))
                    }

                    link(field, config)
                } catch (IllegalAccessException e) {
                    throw new CloudRuntimeException(e)
                }
            }

            for (GlobalConfig c : configs.values()) {
                if (!c.isLinked()) {
                    logger.warn(String.format("GlobalConfig[category: %s, name: %s] is not linked to any definition",
                            c.getCategory(), c.getName()));
                }
            }
        }

        private void link(Field field, final GlobalConfig old) throws IllegalAccessException {
            GlobalConfig xmlConfig = configs.get(old.getIdentity())
            DebugUtils.Assert(xmlConfig != null,
                    String.format("unable to find GlobalConfig[category:%s, name:%s] for linking to %s.%s",
                            old.getCategory(), old.getName(), field.getDeclaringClass().getName(), field.getName()))
            final GlobalConfig config = old.copy(xmlConfig)
            field.set(null, config)
            configs.put(old.getIdentity(), config)

            final GlobalConfigValidation at = field.getAnnotation(GlobalConfigValidation.class)
            if (at != null) {
                config.installValidateExtension(new GlobalConfigValidatorExtensionPoint() {
                    @Override
                    public void validateGlobalConfig(String category, String name, String oldValue, String value) throws GlobalConfigException {
                        if (at.notNull() && value == null) {
                            throw new GlobalConfigException(String.format("%s cannot be null", config.getCanonicalName()))
                        }
                    }
                });

                config.installValidateExtension(new GlobalConfigValidatorExtensionPoint() {
                    @Override
                    public void validateGlobalConfig(String category, String name, String oldValue, String newValue) throws GlobalConfigException {
                        if (at.notEmpty() && newValue.trim() == "") {
                            throw new GlobalConfigException(String.format("%s cannot be empty string", config.getCanonicalName()))
                        }
                    }
                });

                if (at.inNumberRange().length > 0
                        || at.min() != Long.MIN_VALUE
                        || at.max() != Long.MAX_VALUE) {
                    if (config.getType() != null && TypeUtils.isTypeOf(config.getType(), Long.class, Integer.class)) {
                        throw new CloudRuntimeException(String.format("%s has @GlobalConfigValidation " +
                                "defined on field[%s.%s] which indicates its numeric type, " +
                                "but its type is neither Long nor Integer, it's %s",
                                config.getCanonicalName(), field.getDeclaringClass(), field.getName(), config.getType()))
                    }
                    if (config.getType() == null) {
                        logger.warn(String.format("%s has @GlobalConfigValidation " +
                                "defined on field[%s.%s] which indicates it's numeric type, " +
                                "but its is null, assume it's Long type",
                                config.getCanonicalName(), field.getDeclaringClass(), field.getName()))
                        config.setType(Long.class.getName())
                    }
                }

                if (at.max() != Long.MAX_VALUE) {
                    config.installValidateExtension(new GlobalConfigValidatorExtensionPoint() {
                        @Override
                        public void validateGlobalConfig(String category, String name, String oldValue, String value) throws GlobalConfigException {
                            try {
                                long num = Long.parseLong(value)
                                if (num > at.max()) {
                                    throw new GlobalConfigException(
                                            String.format("%s.max should be %s, but got %s",
                                                    config.getCanonicalName(), at.max(), num))
                                }
                            } catch (NumberFormatException e) {
                                throw new GlobalConfigException(
                                        String.format("%s is not a number or out of range of a Long type", value), e)
                            }
                        }
                    })
                }

                if (at.min() != Long.MIN_VALUE) {
                    config.installValidateExtension(new GlobalConfigValidatorExtensionPoint() {
                        @Override
                        public void validateGlobalConfig(String category, String name, String oldValue, String value) throws GlobalConfigException {
                            try {
                                long num = Long.parseLong(value)
                                if (num < at.min()) {
                                    throw new GlobalConfigException(
                                            String.format("%s.min should be %s, but got %s",
                                                    config.getCanonicalName(), at.min(), num))
                                }
                            } catch (NumberFormatException e) {
                                throw new GlobalConfigException(
                                        String.format("%s is not a number or out of range of a Long type", value), e)
                            }
                        }
                    })
                }

                if (at.inNumberRange().length > 0) {
                    DebugUtils.Assert(at.inNumberRange().length == 2,
                            String.format("@GlobalConfigValidation.inNumberRange " +
                                    "defined on field[%s.%s] must have two elements, " +
                                    "where the first one is lower bound and the second one is upper bound",
                                    field.getDeclaringClass(), field.getName()))

                    config.installValidateExtension(new GlobalConfigValidatorExtensionPoint() {
                        @Override
                        public void validateGlobalConfig(String category, String name, String oldValue, String value) throws GlobalConfigException {
                            try {
                                long num = Long.parseLong(value)
                                long lowBound = at.inNumberRange()[0]
                                long upBound = at.inNumberRange()[1]
                                if (!(num >= lowBound && num <= upBound)) {
                                    throw new GlobalConfigException(String.format("%s must in range of [%s, %s]",
                                            config.getCanonicalName(), lowBound, upBound))
                                }
                            } catch (NumberFormatException e) {
                                throw new GlobalConfigException(
                                        String.format("%s is not a number or out of range of a Long type", value), e)
                            }
                        }
                    })
                }

                if (at.validValues().length > 0) {
                    final List<String> validValues = new ArrayList<String>()
                    Collections.addAll(validValues, at.validValues())
                    config.installValidateExtension(new GlobalConfigValidatorExtensionPoint() {
                        @Override
                        public void validateGlobalConfig(String category, String name, String oldValue, String newValue) throws GlobalConfigException {
                            if (!validValues.contains(newValue)) {
                                throw new GlobalConfigException(
                                        String.format("%s is not a valid value. Valid values are %s", newValue, validValues));
                            }
                        }
                    })
                }
            }

            config.setConfigDef(field.getAnnotation(GlobalConfigDef.class));
            config.setLinked(true)
            if (logger.isTraceEnabled()) {
                logger.trace(String.format("linked GlobalConfig[category:%s, name:%s, value:%s] to %s.%s",
                        config.getCategory(), config.getName(), config.getDefaultValue(),
                        field.getDeclaringClass().getName(), field.getName()))
            }
        }
    }


    String rootPath

    String projectVersion

    Map<String, File> sourceFiles = [:]

    def MUTUAL_FIELDS = [
            "lastOpDate": "最后一次修改时间",
            "createDate": "创建时间",
            "uuid": "资源的UUID，唯一标示该资源",
            "name": "资源名称",
            "description": "资源的详细描述",
            "primaryStorageUuid": "主存储UUID",
            "vmInstanceUuid": "云主机UUID",
            "imageUuid": "镜像UUID",
            "backupStorageUuid": "镜像存储UUID",
            "volumeUuid": "云盘UUID",
            "zoneUuid": "区域UUID",
            "clusterUuid": "集群UUID",
            "hostUuid": "物理机UUID",
            "l2NetworkUuid": "二层网络UUID",
            "l3NetworkUuid": "三层网络UUID",
            "accountUuid": "账户UUID",
            "policyUuid": "权限策略UUID",
            "userUuid": "用户UUID",
            "diskOfferingUuid": "云盘规格UUID",
            "volumeSnapshotUuid": "云盘快照UUID",
            "ipRangeUuid": "IP段UUID",
            "instanceOfferingUuid": "计算规格UUID",
            "vipUuid": "VIP UUID",
            "vmNicUuid": "云主机网卡UUID",
            "networkServiceProviderUuid": "网络服务提供模块UUID",
            "virtualRouterUuid": "云路由UUID",
            "securityGroupUuid": "安全组UUID",
            "eipUuid": "弹性IP UUID",
            "loadBalancerUuid": "负载均衡器UUID",
            "rootVolumeUuid": "根云盘UUID",
            "userTags": "用户标签",
            "systemTags": "系统标签",
            "tagUuids": "标签UUID列表",
            "deleteMode": "删除模式(Permissive / Enforcing，Permissive)",
            "resourceUuid": "资源UUID",
            "buildSystemUuid": "build存储系统UUID"
    ]

    String CHINESE_CN = "zh_cn"
    String ENGLISH_CN = "en_us"

    String LOCATION_URL = "url"
    String LOCATION_BODY = "body"
    String LOCATION_QUERY = "query"

    String CURL_URL_BASE = "http://localhost:8080/zstack"

    String makeFileNameForChinese(Class clz) {
        boolean inner = clz.getEnclosingClass() != null
        String fname
        if (inner) {
            fname = clz.getEnclosingClass().simpleName - ".java" + "_${clz.simpleName}" + "Doc_${CHINESE_CN}.groovy"
        } else {
            fname = clz.simpleName - ".java" + "Doc_${CHINESE_CN}.groovy"
        }

        return fname
    }

    String getSimpleClassNameFromDocTemplateName(String templateName) {
        return templateName - CHINESE_CN - ENGLISH_CN
    }

    def installClosure(ExpandoMetaClass emc, Closure c) {
        c(emc)
    }

    void tryInitVersionOfCurrentRepo() {
        def f = new File("${rootPath}/VERSION")
        if (!f.exists()) {
            println("missing version file[path: ${rootPath}/VERSION]")
            return
        }

        def versionNumber = []
        FileUtils.readLines(f).each { String line ->
            versionNumber.add(line.split("=")[1].trim())
        }

        projectVersion = versionNumber.join(".")
    }

    @Override
    void generateDocTemplates(String scanPath, DocMode mode) {
        rootPath = scanPath
        tryInitVersionOfCurrentRepo()
        scanJavaSourceFiles()

        Set<Class> apiClasses = getRequestRequestApiSet()
        apiClasses.each {
            println("generating doc template for class ${it}")
            def tmp = new ApiRequestDocTemplate(it)
            tmp.generateDocFile(mode)
        }

        apiClasses = apiClasses.collect {
            RestRequest at = it.getAnnotation(RestRequest.class)
            return at.responseClass()
        }

        generateResponseDocTemplate(apiClasses as List, mode)
    }

    Set<Class> getRequestRequestApiSet() {
        Set<Class> apiClasses = Platform.getReflections().getTypesAnnotatedWith(RestRequest.class).findAll { it.isAnnotationPresent(RestRequest.class) }
        Set<String> noDocClasses = Platform.getReflections().getTypesAnnotatedWith(NoDoc.class)
                .stream().map{ c -> c.getSimpleName() }.collect(Collectors.toSet())
        logger.info("no doc classes: ${noDocClasses}".toString())

        String specifiedClasses = System.getProperty("classes")

        if (specifiedClasses != null) {
            def classes = specifiedClasses.split(",") as List
            apiClasses = apiClasses.findAll {
                return classes.contains(it.simpleName)
            }
        }

        if (noDocClasses != null) {
            apiClasses = apiClasses.findAll {
                return !noDocClasses.contains(it.getSimpleName())
            }
        }

        return apiClasses
    }

    @Override
    void generateMarkDown(String scanPath, String resultDir) {
        rootPath = scanPath

        scanJavaSourceFiles()

        File dir = new File(resultDir)
        if (!dir.exists()) {
            dir.mkdirs()
        }

        def errInfo = []
        getRequestRequestApiSet().each {
            if (it.isInterface() || Modifier.isAbstract(it.getModifiers())) {
                return
            }

            def docPath = getDocTemplatePathFromClass(it)
            logger.info("processing ${docPath}")
            try {
                def md = APIQueryMessage.class.isAssignableFrom(it) ? new QueryMarkDown(docPath) : new MarkDown(docPath)
                File f = new File(PathUtil.join(resultDir, md.doc._category, "${it.canonicalName.replaceAll("\\.", "_")}.md"))
                if (!f.parentFile.exists()) {
                    f.parentFile.mkdirs()
                }

                f.write(md.generate())
                logger.info("written ${f.absolutePath}")
            } catch (Exception e) {
                if (ignoreError()) {
                    logger.info("failed to process ${docPath}, ${e.message}")
                    logger.warn(e.message, e)
                } else if (e instanceof FileNotFoundException) {
                    errInfo.add(e.message)
                } else {
                    throw e
                }
            }
        }

        if (!errInfo.isEmpty()) {
            logger.warn(String.format("there are %d errors found:", errInfo.size()))
            throw new FileNotFoundException(errInfo.stream().collect(Collectors.joining("\n")))
        }

        URL url = Resources.getResource("doc/RestIntroduction_zh_cn.md")
        String context = Resources.toString(url, Charsets.UTF_8)
        new File(PathUtil.join(dir.absolutePath, "RestIntroduction_zh_cn.md")).write(context)
    }

    @Override
    void generateErrorCodeDoc(String scanPath, String resultFile) {
        ErrorFacadeImpl errf = new ErrorFacadeImpl()
        errf.init()
        List<ErrorCode> codes = errf.getAllErrorCodes()
        if (codes == null || codes.isEmpty()) {
            throw new RuntimeException("no error codes loaded from classpath errorCodes/")
        }

        File out = new File(resultFile)
        if (out.exists()) {
            if (out.isDirectory()) {
                if (!out.deleteDir()) {
                    throw new RuntimeException(String.format("path %s is a directory and cannot be deleted, please delete it first", out.path))
                }
            } else if (!out.delete()) {
                throw new RuntimeException(String.format("file %s cannot be deleted, please delete it first", out.path))
            }
        }
        if (out.parentFile != null && !out.parentFile.exists()) {
            out.parentFile.mkdirs()
        }

        Map<String, List<ErrorCode>> byPrefix = new TreeMap<>()
        codes.each { ErrorCode code ->
            String full = code.getCode()
            int idx = full == null ? -1 : full.lastIndexOf('.')
            String prefix = idx > 0 ? full.substring(0, idx) : (full ?: "UNKNOWN")
            byPrefix.computeIfAbsent(prefix, { k -> new ArrayList<>() }).add(code)
        }

        StringBuilder sb = new StringBuilder()
        sb.append("# Error Codes\n\n")
        byPrefix.each { String prefix, List<ErrorCode> list ->
            sb.append("## ").append(prefix).append("\n\n")
            sb.append("| Code | Description |\n")
            sb.append("|------|-------------|\n")
            list.sort { a, b -> a.getCode() <=> b.getCode() }
            list.each { ErrorCode code ->
                String desc = code.getDescription() == null ? "" : code.getDescription().trim().replaceAll("\\|", "*")
                sb.append("| ").append(code.getCode()).append(" | ").append(desc).append(" |\n")
            }
            sb.append("\n")
        }

        out.write(sb.toString())
        logger.info(String.format("written error code doc to %s (%s codes)", out.absolutePath, codes.size()))
    }

    @Override
    void generateGlobalConfigMarkDown(String resultDir, DocMode mode) {
        Map<String, GlobalConfig> configMap = initializer.configs
        configMap.each {
            def newPath = PathUtil.join(resultDir, it.value.category, it.value.name) + DEPRECATED + ".md"
            if (new File(newPath).exists()) {
                return
            }
            def path = PathUtil.join(resultDir, it.value.category, it.value.name) + ".md"
            File f = new File(path)
            List<String> names = new ArrayList<>()
            initializer.bindResources.get(it.value.getIdentity()).each { names.add(it.getName()) }
            GlobalConfigMarkDown configMd = new GlobalConfigMarkDown(it.value, names,
                    initializer.validatorMap.get(it.value.getIdentity()))
            if (!f.parentFile.exists()) {
                f.parentFile.mkdirs()
            }
            if (mode == DocMode.CREATE_MISSING) {
                if (f.exists()) {
                    logger.info("${path} exists, skip it")
                    return
                }
                logger.info("generating global config markdown for config[${it.key}]")
                f.write(configMd.generate())
                logger.info("written a global config markdown ${path}")
            } else {
                GlobalConfigMarkDown g = configMd
                if (!f.exists()) {
                    logger.info("${path} does not exist, create it")
                } else {
                    logger.info("start repair a global config markdown ${path}")
                    g = getExistGlobalConfigMarkDown(path)
                    g.merge(configMd)
                    logger.info("re-written a global config markdown ${path}")
                }
                f.write(g.generate())
            }
        }
    }

    GlobalConfigMarkDown getExistGlobalConfigMarkDown(String path) {
        def file = new File(path)
        if (!file.exists()) {
            throw new CloudRuntimeException("${path} does not exist, please create it first.")
        }
        String process = new Markdown4jProcessor().process(file)
        Document document = Jsoup.parse(process)
        GlobalConfigMarkDown globalConfigMarkDown = new GlobalConfigMarkDown()
        Elements allElements = document.getAllElements()
        allElements.forEach({ titleElement ->
            Element contentElement = titleElement.nextElementSibling()
            if (titleElement.childNodeSize() == 0) {
                return
            }
            String title = titleElement.childNode(0).toString()
            if (contentElement == null || contentElement.childNodeSize() == 0) {
                return
            }
            if (contentElement != null) {
                if ("支持的资源级配置" == title && contentElement.childNodeSize() > 1) {
                    String s = contentElement.text()//example: || |—| |org.zstack.header.cluster.ClusterVO
                    String[] split = s.split(" \\|")
                    List<String> list = new ArrayList<>(Arrays.asList(split))
                    list.remove(0)
                    list.remove(0)
                    globalConfigMarkDown.globalConfig.resources = list
                }
                String text = ""
                if (contentElement.childNode(0).childNodeSize() != 0) {
                    Node childNode = contentElement.childNode(0).childNode(0)
                    text = childNode.toString().substring(0, childNode.toString().length() - 1).trim()
                }
                if ("h2" == titleElement.tagName()) {
                    if ("Name" == title) {
                        String name = text.toString()
                        String[] split = name.split("\\(")
                        globalConfigMarkDown.globalConfig.name = split[0]
                        globalConfigMarkDown.name_CN = split[1].substring(0, split[1].length() - 1)
                    }
                    if ("注意事项" == title) {
                        globalConfigMarkDown.additionalRemark = text
                    }
                }
                if ("h3" == titleElement.tagName()) {
                    if ("Description" == title) {
                        globalConfigMarkDown.globalConfig.description = text
                    }
                    if ("含义" == title) {
                        globalConfigMarkDown.desc_CN = text
                    }
                    if ("Type" == title) {
                        globalConfigMarkDown.globalConfig.type = text
                    }
                    if ("Category" == title) {
                        globalConfigMarkDown.globalConfig.category = text
                    }
                    if ("取值范围" == title) {
                        globalConfigMarkDown.globalConfig.valueRange = text == "" ? null : text
                    }
                    if ("DefaultValue" == title) {
                        globalConfigMarkDown.globalConfig.defaultValue = text
                    }
                    if ("默认值补充说明" == title) {
                        globalConfigMarkDown.defaultValueRemark = text
                    }
                    if ("取值范围补充说明" == title) {
                        globalConfigMarkDown.valueRangeRemark = text
                    }
                    if ("资源粒度说明" == title) {
                        globalConfigMarkDown.resourcesGranularitiesRemark = text
                    }
                    if ("背景信息" == title) {
                        globalConfigMarkDown.backgroundInformation = text
                    }
                    if ("UI暴露" == title) {
                        globalConfigMarkDown.isUIExposed = text
                    }
                    if ("CLI手册暴露" == title) {
                        globalConfigMarkDown.isCLIExposed = text
                    }
                }
            }
        })
        return globalConfigMarkDown
    }

    Boolean isConsistent(GlobalConfigMarkDown md, GlobalConfig globalConfig) {
        if (md == null || globalConfig == null) {
            return false
        }
        String mdPath =
                PathUtil.join(PathUtil.join(Paths.get("../doc").toAbsolutePath().normalize().toString(),
                        "globalconfig"), md.globalConfig.category, md.globalConfig.name) + ".md"
        List<String> classes = new ArrayList<>()
        initializer.bindResources.get(globalConfig.getIdentity()).each { classes.add(it.getName()) }
        List<String> newClasses = classes.sort()
        String validatorString = initializer.validatorMap.get(globalConfig.getIdentity())
        Boolean flag = true
        if (md.globalConfig.name != globalConfig.name) {
            logger.info("${mdPath}.name: expect ${globalConfig.name}, actual ${md.globalConfig.name}")
            flag = false
        }
        if (md.globalConfig.defaultValue != globalConfig.defaultValue) {
            logger.info("${mdPath}.defaultValue: expect ${globalConfig.defaultValue}, actual ${md.globalConfig.defaultValue}")
            flag = false
        }
        if (StringUtils.trimToEmpty(md.globalConfig.description) != StringUtils.trimToEmpty(globalConfig.description)) {
            logger.info("${mdPath}.description:\n    expect ${globalConfig.description}\n    actual ${md.globalConfig.description}")
                flag = false
        }
        if (md.globalConfig.type != globalConfig.type) {
            if (globalConfig.type != null) {
                logger.info("${mdPath}.type: expect ${globalConfig.type}, actual ${md.globalConfig.type}")
                flag = false
            }
        }
        if (md.globalConfig.category != globalConfig.category) {
            logger.info("${mdPath}.category: expect ${globalConfig.category}, actual ${md.globalConfig.category}")
            flag = false
        }
            List<String> oldClasses = md.globalConfig.resources.sort()
            if (oldClasses != newClasses) {
                logger.info("${mdPath}.classes: expect ${newClasses}, actual ${oldClasses}")
                flag = false
            }

        if (md.globalConfig.valueRange != (validatorString)) {
            boolean useBooleanValidator = (globalConfig.type == "java.lang.Boolean"
                    && md.globalConfig.valueRange == "{true, false}")
            if (validatorString != null || !useBooleanValidator) {
                logger.info("${mdPath}.valueRange:\n    expect ${validatorString}\n    actual ${md.globalConfig.valueRange}")
                flag = false
            }
        }
        return flag
    }

    void checkMD(String mdPath, GlobalConfig globalConfig) {
        String result = ShellUtils.runAndReturn(
                "grep '${PLACEHOLDER}' ${mdPath}").stdout.replaceAll("\n", "")
        if (!result.empty) {
            throw new CloudRuntimeException("Placeholders are detected in ${mdPath}, please replace them by content.")
        }
        GlobalConfigMarkDown markDown = getExistGlobalConfigMarkDown(mdPath)
        if (markDown.desc_CN.isEmpty()
                || markDown.name_CN.isEmpty()
                || markDown.valueRangeRemark.isEmpty()
                || markDown.defaultValueRemark.isEmpty()
                || markDown.resourcesGranularitiesRemark.isEmpty()
                || markDown.additionalRemark.isEmpty()
                || markDown.backgroundInformation.isEmpty()
                || markDown.isUIExposed.isEmpty()
                || markDown.isCLIExposed.isEmpty()
        ) {
            throw new CloudRuntimeException("The necessary information of ${mdPath} is missing, please complete the information before submission.")
        }
        if (!isConsistent(markDown, globalConfig)) {
            throw new CloudRuntimeException("${mdPath} is not match with its definition, please use Repair mode to correct it.")
        }
    }

    class RequestParamColumn {
        private String _name
        private String _desc
        private boolean _optional
        private List _values
        private String _since
        private String _type
        private String _location
        private String _enclosedIn

        def enclosedIn(String v) {
            _enclosedIn = v
        }

        def location(String v) {
            _location = v
        }

        def since(String v) {
            _since = v
        }

        def type(String v) {
            _type = v
        }

        def name(String v) {
            _name = v
        }

        def desc(String v) {
            _desc = v
        }

        def optional(boolean v) {
            _optional = v
        }

        def values(Object...args) {
            _values = args as List
        }
    }

    class RequestParamRef extends RequestParam {
        Class refClass
    }

    class RequestParam {
        private List<RequestParamColumn> _cloumns = []
        private String _desc

        def column(Closure c) {
            def rc = c.delegate = new RequestParamColumn()
            c.resolveStrategy = Closure.DELEGATE_FIRST
            c()

            _cloumns.add(rc)
        }

        def desc(String v) {
            _desc = v
        }
    }

    class Rest {
        private Request _request
        private Response _response

        def response(Closure c) {
            c.delegate = new Response()
            c.resolveStrategy = Closure.DELEGATE_FIRST
            c()

            _response = c.delegate as Response
        }

        def request(Closure c) {
            c.delegate = new Request()
            c.resolveStrategy = Closure.DELEGATE_FIRST
            c()

            _request = c.delegate as Request
        }
    }

    class Response {
        private Class _clz
        private String _desc

        def clz(Class v)  {
            _clz = v
        }

        def desc(String v) {
            _desc = v
        }
    }

    class Header {
        String key
        String value
        String comment

        String toString() {
            return "header (${key}: '$value')"
        }
    }

    class Url {
        String url
        String comment

        String toString() {
            return "url \"${url}\""
        }
    }

    class Request {
        private List<Url> _urls = []
        private List<Header> _headers = []
        private String _desc
        private RequestParam _params
        private LinkedHashMap _body
        private Class _clz

        def url(String v, String comment) {
            def u = new Url()
            u.url = v
            u.comment = comment
            _urls.add(u)
        }

        def url(String v) {
            url(v, null)
        }

        def header(Map v) {
            header(v, null)
        }

        def header(Map v, String comment) {
            Map.Entry e = v.entrySet().iterator().next()
            def h = new Header()
            h.key = e.key
            h.value = e.value
            h.comment = comment
            _headers.add(h)
        }

        def desc(String v) {
            _desc = v
        }

        def params(Class v) {
            def p = new RequestParamRef()
            p.refClass = v

            _params = p
        }

        def params(Closure c) {
            c.delegate = new RequestParam()
            c.resolveStrategy = Closure.DELEGATE_FIRST
            c()

            _params = c.delegate
        }

        def body(Closure c) {
            def j = new JsonBuilder()
            j.call(c)
            _body = JSONObjectUtil.toObject(j.toString(), LinkedHashMap.class)
        }

        def clz(Class v){
            _clz = v
        }
    }

    class DocField {
        String _name
        String _desc
        String _type
        String _since

        def name(String v) {
            _name = v
        }

        def desc(String v) {
            _desc = v
        }

        def type(String v) {
            _type = v
        }

        def since(String v) {
            _since = v
        }
    }

    class DocFieldRef {
        String _name
        String _type
        String _path
        String _desc
        Class _clz
        String _since
        Boolean _overrideDesc

        def since(String v) {
            _since = v
        }

        def name(String v) {
            _name = v
        }

        def type(String v) {
            _type = v
        }

        def path(String v) {
            _path = v
        }

        def desc(String v, Boolean o) {
            _desc = v
            _overrideDesc = o
        }

        def desc(String v) {
            desc(v, null)
        }

        def clz(Class v) {
            _clz = v
        }
    }

    class Doc {
        private String _title
        private String _category
        private String _desc
        private Rest _rest
        private List<DocField> _fields = []
        private List<DocFieldRef> _refs = []

        void merge(Doc n) {
            Request oreq = _rest._request
            Request nreq = n._rest._request

            oreq._urls = nreq._urls
            oreq._headers = nreq._headers
            oreq._clz = nreq._clz

            if (!(oreq._params instanceof RequestParamRef) && oreq._params?._cloumns != null && nreq._params?._cloumns != null) {
                oreq._params._cloumns.each { ocol ->
                    RequestParamColumn ncol = nreq._params._cloumns.find { it._name == ocol._name }
                    if (ncol == null) {
                        // the column is removed
                        return
                    }

                    ocol._enclosedIn = ncol._enclosedIn
                    ocol._location = ncol._location
                    ocol._type = ncol._type
                    ocol._optional = ncol._optional
                }

                if(nreq._params._cloumns.size() > oreq._params._cloumns.size()){
                    nreq._params._cloumns.each { ncol ->
                        RequestParamColumn ocol = oreq._params._cloumns.find { it._name == ncol._name }
                        if(ocol == null){
                            oreq._params._cloumns.add(ncol)
                        }
                    }
                }
            }

            Response orsp = _rest._response
            Response nrsp = n._rest._response
            orsp._clz = nrsp._clz
        }

        def title(String v) {
            _title = v
        }

        def category(String v) {
            _category = v
        }

        def desc(String v) {
            _desc = v
        }

        def rest(Closure c) {
            c.delegate = new Rest()
            c.resolveStrategy = Closure.DELEGATE_FIRST
            c()

            _rest = c.delegate
        }

        def field(Closure c) {
            c.delegate = new DocField()
            c.resolveStrategy = Closure.DELEGATE_FIRST
            c()

            _fields.add(c.delegate as DocField)
        }

        def ref(Closure c) {
            c.delegate = new DocFieldRef()
            c.resolveStrategy = Closure.DELEGATE_FIRST
            c()

            _refs.add(c.delegate as DocFieldRef)
        }
    }

    Doc createDocFromGroobyScript(Script script) {
        ExpandoMetaClass emc = new ExpandoMetaClass(script.getClass(), false, true)

        installClosure(emc, { ExpandoMetaClass e ->
            e.doc = { Closure cDoc ->
                cDoc.delegate = new Doc()
                cDoc.resolveStrategy = DELEGATE_FIRST

                cDoc()

                return cDoc.delegate
            }
        })

        emc.initialize()

        script.setMetaClass(emc)

        return script.run() as Doc
    }

    Doc createDocFromString(String scriptText) {
        Script script = new GroovyShell().parse(scriptText)
        return createDocFromGroobyScript(script)
    }

    Doc createDoc(String docTemplatePath) {
        Script script = new GroovyShell().parse(new File(docTemplatePath))
        return createDocFromGroobyScript(script)
    }

    class QueryMarkDown extends MarkDown {
        QueryMarkDown(String docTemplatePath) {
            super(docTemplatePath)
        }

        @Override
        String params() {
            Class clz = doc._rest._request._clz

            String apiName = StringUtils.removeStart(clz.simpleName, "API")
            apiName = StringUtils.removeEnd(apiName, "Msg")

            return """\
### 可查询字段

运行`zstack-cli`命令行工具，输入`${apiName}`并按Tab键查看所有可查询字段以及可跨表查询的资源名。
"""
        }

        List<String> getQueryConditionExampleOfTheClass(Class clz) {
            try {
                Method m = clz.getMethod("__example__")

                if (!Modifier.isStatic(m.modifiers)) {
                    throw new CloudRuntimeException("__example__() of ${clz.name} must be declared as a static method")
                }

                return m.invoke(null) as List<String>
            } catch (NoSuchMethodException e) {
                //throw new CloudRuntimeException("class[${clz.name}] doesn't have static __example__ method", e)
                logger.warn("class[${clz.name}] doesn't have static __example__ method")
            }
        }

        @Override
        String javaSdk() {
            Class clz = doc._rest._request._clz

            String actionName = getSdkActionName()

            def cols = ["${actionName} action = new ${actionName}();"]
            List<String> conds = getQueryConditionExampleOfTheClass(clz)
            conds = conds.collect { return "\"" + it + "\""}
            cols.add("action.conditions = asList(${conds.join(",")});")
            cols.add("""action.sessionId = "b86c9016b4f24953a9edefb53ca0678c\";""")
            cols.add("${actionName}.Result res = action.call();")

            return """\
```
${cols.join("\n")}
```
"""
        }

        @Override
        String pythonSdk() {
            Class clz = doc._rest._request._clz

            String actionName = getSdkActionName()

            List<String> cols = ["action = ${actionName}()".toString()]
            List<String> conds = getQueryConditionExampleOfTheClass(clz)
            conds = conds.collect { return "\"" + it + "\""}
            cols.add("action.conditions = [${conds.join(",")}]".toString())
            cols.add("action.sessionId = \"b86c9016b4f24953a9edefb53ca0678c\"")
            cols.add("res = action.call()")

            return """\
```
${cols.join("\n")}
```
"""
        }

        String curlExample() {
            if (doc._rest._request._clz == null) {
                return ""
            }

            Class clz = doc._rest._request._clz
            RestRequest at = clz.getAnnotation(RestRequest.class)

            Set<String> paths = []
            if (at.path() != "null") {
                paths.add(at.path())
            }
            paths.addAll(at.optionalPaths())

            List<String> examples = paths.collect {
                def curl = ["curl -H \"Content-Type: application/json;charset=UTF-8\""]
                curl.add("-H \"Authorization: ${RestConstants.HEADER_OAUTH} b86c9016b4f24953a9edefb53ca0678c\"")

                curl.add("-X ${at.method()}")

                if (it.contains("uuid")) {
                    // for "GET /v1/xxx/{uuid}, because the query example
                    // may not have uuid field specified
                    String urlPath = substituteUrl("${RestConstants.API_VERSION}${it}", ["uuid": UUID.nameUUIDFromBytes(it.getBytes()).toString().replaceAll("-", "")])
                    curl.add("${CURL_URL_BASE}${urlPath}")
                } else {
                    List<String> qstr = getQueryConditionExampleOfTheClass(clz)
                    qstr = qstr.collect {
                        return "q=${it}"
                    }
                    curl.add("${CURL_URL_BASE}${RestConstants.API_VERSION}${it}?${qstr.join("&")}")
                }

                return """\
```
${curl.join(" ")}
```
"""
            }


            return """\
### Curl示例

${examples.join("\n")}

"""
        }


        @Override
        String generate() {
            def urlScript = ""
            def headerScript = ""
            def requestExampleScript = ""
            def curlExampleScript = ""
            def paramsScript = ""
            def responseDescScript = ""
            def responseExampleScript = ""
            def javaSdkScript = ""
            def pythonSdkScript = ""

            try {
                urlScript = url()
            } catch (Exception e) { logger.warn("failed to generate url of ${doc._title}", e) }
            try {
                headerScript = headers()
            } catch (Exception e) { logger.warn("failed to generate header of ${doc._title}", e) }
            try {
                requestExampleScript = requestExample()
            } catch (Exception e) { logger.warn("failed to generate requestExample of ${doc._title}", e) }
            try {
                curlExampleScript = curlExample()
            } catch (Exception e) { logger.warn("failed to generate curlExample of ${doc._title}", e) }
            try {
                paramsScript = params()
            } catch (Exception e) { logger.warn("failed to generate params of ${doc._title}", e) }
            try {
                responseDescScript = responseDesc()
            } catch (Exception e) { logger.warn("failed to generate responseDesc of ${doc._title}", e) }
            try {
                responseExampleScript = responseExample()
            } catch (Exception e) { logger.warn("failed to generate responseExample of ${doc._title}", e) }
            try {
                javaSdkScript = javaSdk()
            } catch (Exception e) { logger.warn("failed to generate javaSdk of ${doc._title}", e) }
            try {
                pythonSdkScript = pythonSdk()
            } catch (Exception e) { logger.warn("failed to generate pythonSdk of ${doc._title}", e) }

            return  """\
## ${doc._category} - ${doc._title}

${doc._desc}

## API请求

${urlScript}

${headerScript}

${requestExampleScript}

${curlExampleScript}

${paramsScript}

## API返回

${responseDescScript}

${responseExampleScript}

## SDK示例

### Java SDK

${javaSdkScript}

### Python SDK

${pythonSdkScript}
"""
        }
    }

    class MarkDown {
        Doc doc

        MarkDown(String docTemplatePath) {
            doc = createDoc(docTemplatePath)

            if (doc._rest._request._params instanceof RequestParamRef) {
                Class refClass = (doc._rest._request._params as RequestParamRef).refClass
                String docFilePath = getDocTemplatePathFromClass(refClass)
                Doc refDoc = createDoc(docFilePath)

                doc._rest._request._params = refDoc._rest._request._params
            }
        }

        String url() {
            def urls = doc._rest._request._urls
            if (urls == null || urls.isEmpty()) {
                throw new CloudRuntimeException("cannot find urls for the class[${doc._rest._request._clz.name}]")
            }

            def txts = urls.collect {
                return it.comment == null ? it.url : "${it.url} #${it.comment}"
            }

            Class clz = doc._rest._request._clz
            RestRequest at = clz.getAnnotation(RestRequest.class)
            List<String> urlVars = getVarNamesFromUrl(at.path())

            List<String> otherVars = doc._rest._request._params._cloumns.findAll {
                return it._location == LOCATION_URL && !urlVars.contains(it._name)
            }.collect {
                return it._name
            }

            List params = []
            otherVars.each {
                params.add("$it={$it}")
            }
            if (params.size() > 0) {
                txts = txts.collect {
                    "${it}?${params.join("&")}"
                }
            }

            return """\
### URLs

```
${txts.join("\n")}
```
"""
        }

        String headers() {
            if (doc._rest._request._headers.isEmpty()) {
                return ""
            }

            def hs = []
            doc._rest._request._headers.each { h ->
                if (h.comment != null) {
                    hs.add("${h.key}: ${h.value} #${h.comment}")
                } else {
                    hs.add("${h.key}: ${h.value}")
                }
            }

            return """\
### Headers

```
${hs.join("\n")}
```
"""
        }

        String params() {
            if (doc._rest._request._params == null) {
                return ""
            }

            def cols = doc._rest._request._params?._cloumns

            if (cols == null || cols.isEmpty()) {
                return ""
            }


            def table = ["|名字|类型|位置|描述|可选值|起始版本|"]
            table.add("|---|---|---|---|---|---|")
            cols.each {
                def col = []

                String enclosed = null
                if (it._enclosedIn != "" && it._location == LOCATION_BODY) {
                    enclosed = "包含在`${it._enclosedIn}`结构中"
                }

                col.add(it._optional ? "${it._name} (可选)" : "${it._name}")
                col.add(it._type)

                def loc = it._location == LOCATION_BODY ? "body${enclosed == null ? "" : "(${enclosed})"}" : it._location
                col.add(loc)
                col.add("${it._desc}")
                if (it._values == null || it._values.isEmpty()) {
                    col.add("")
                } else {
                    def vals = it._values.collect { v ->
                        return "<li>${v}</li>"
                    }
                    col.add("<ul>${vals.join("")}</ul>")
                }
                col.add(it._since)

                table.add("|${col.join("|")}|")
            }

            return """\
### 参数列表

${doc._rest._request._params._desc == null ? "" : doc._rest._request._params._desc}

${table.join("\n")}
"""
        }

        LinkedHashMap getApiExampleOfTheClass(Class clz) {
            try {
                Method m = clz.getMethod("__example__")

                if (!Modifier.isStatic(m.modifiers)) {
                    throw new CloudRuntimeException("__example__() of ${clz.name} must be declared as a static method")
                }

                def example = m.invoke(null)
                DocUtils.removeApiUuidMap(example.class.name)


                if (example instanceof APIMessage) {
                    example.validate()
                }

                LinkedHashMap map = JSONObjectUtil.rehashObject(example, LinkedHashMap.class)

                List<Field> apiFields = getApiFieldsOfClass(clz)

                def apiFieldNames = apiFields.collect {
                    return it.name
                }

                LinkedHashMap paramMap = [:]
                map.each { k, v ->
                    if (apiFieldNames.contains(k)) {
                        paramMap[k] = v
                    }
                }

                return paramMap
            } catch (InvalidApiMessageException ie) {
                throw new CloudRuntimeException("cannot generate the markdown document for the class[${clz.name}], the __example__() method has an error: ${String.format(ie.getMessage(), ie.getArguments())}")
            } catch (NoSuchMethodException e) {
                //throw new CloudRuntimeException("class[${clz.name}] doesn't have static __example__ method", e)
                logger.warn("class[${clz.name}] doesn't have static __example__ method")
            }
        }

        String curlExample() {
            if (doc._rest._request._clz == null) {
                return ""
            }

            Class clz = doc._rest._request._clz
            RestRequest at = clz.getAnnotation(RestRequest.class)

            Set<String> paths = []
            if (at.path() != "null") {
                paths.add(at.path())
            }
            paths.addAll(at.optionalPaths())

            List<String> examples = paths.collect {
                def curl = ["curl -H \"Content-Type: application/json;charset=UTF-8\""]
                if (!clz.isAnnotationPresent(SuppressCredentialCheck.class)) {
                    curl.add("-H \"Authorization: ${RestConstants.HEADER_OAUTH} b86c9016b4f24953a9edefb53ca0678c\"")
                }

                boolean queryString = (at.method() == HttpMethod.GET || at.method() == HttpMethod.DELETE)

                curl.add("-X ${at.method()}")

                Map allFields = getApiExampleOfTheClass(clz)

                String urlPath = substituteUrl("${RestConstants.API_VERSION}${it}", allFields)

                if (!queryString) {
                    def apiFields = getRequestBody()
                    if (apiFields != null) {
                        curl.add("-d '${JSONObjectUtil.toJsonString(apiFields)}'")
                    }
                    curl.add("${CURL_URL_BASE}${urlPath}")
                } else {
                    List<String> urlVars = getVarNamesFromUrl(at.path())

                    List<String> queryVars = doc._rest._request._params._cloumns.findAll {
                        return it._location == LOCATION_QUERY || (it._location == LOCATION_URL && !urlVars.contains(it._name))
                    }.collect {
                        return it._name
                    }

                    List qstr = []
                    allFields.findAll { k, v ->
                        return queryVars.contains(k)
                    }.each { k, v ->
                        if (v instanceof Collection) {
                            for (vv in v) {
                                qstr.add("${k}=${vv}")
                            }
                        } else if (v instanceof Map) {
                            v.each { kk, vv ->
                                if (vv instanceof Collection) {
                                    for (vvv in vv) {
                                        qstr.add("${k}.${kk}=${vvv}")
                                    }
                                } else {
                                    qstr.add("${k}.${kk}=${vv}")
                                }
                            }
                        } else {
                            qstr.add("${k}=${v}")
                        }
                    }

                    curl.add("${CURL_URL_BASE}${urlPath}?${qstr.join("&")}")
                }

                return """\
```
${curl.join(" ")}
```
"""
            }


            return """\
### Curl示例

${examples.join("\n")}

"""

        }

        Map getRequestBody() {
            Class clz = doc._rest._request._clz
            RestRequest at = clz.getAnnotation(RestRequest.class)

            String paramName = null
            if (at.parameterName() != "" && at.parameterName() != "null") {
                paramName = at.parameterName()
            } else if (at.isAction()) {
                paramName = paramNameFromClass(clz)
            }

            if (paramName == null) {
                // no body
                return null
            }

            // the API has a body
            Map apiFields = getApiExampleOfTheClass(clz)
            List<String> urlVars = getVarNamesFromUrl(at.path())
            apiFields = apiFields.findAll { k, v -> !urlVars.contains(k) }
            return [(paramName): apiFields]
        }

        String requestExample() {
            if (doc._rest._request._clz == null) {
                return ""
            }

            def apiFields = getRequestBody()
            if (apiFields == null) {
                return ""
            }

            apiFields["systemTags"] = []
            apiFields["userTags"] = []

            return """\
### Body

```
${JSONObjectUtil.dumpPretty(apiFields)}
```

> 上述示例中`systemTags`、`userTags`字段可以省略。列出是为了表示body中可以包含这两个字段。
"""
        }

        String responseDesc() {
            return doc._rest._response._desc == null ? "" : doc._rest._response._desc
        }

        String responseExample() {
            Class clz = doc._rest._response._clz
            if (clz == null) {
                throw new CloudRuntimeException("${doc._rest} doesn't have 'clz' specified in the response body")
            }

            String docFilePath = getDocTemplatePathFromClass(clz)
            Doc doc = createDoc(docFilePath)
            DataStructMarkDown dmd = new DataStructMarkDown(clz, doc)

            LinkedHashMap map = getApiExampleOfTheClass(clz)
            // the success field is not exposed to Restful APIs
            map.remove("success")

            def cols = []
            if (map.isEmpty()) {
                cols.add("""\
该API成功时返回一个空的JSON结构`{}`，出错时返回的JSON结构包含一个error字段，例如：

```
{
\t"error": {
\t\t"code": "SYS.1001",
\t\t"description": "A message or a operation timeout",
\t\t"details": "Create VM on KVM timeout after 300s"
\t}
}
```


""")
            } else {
                cols.add("""\
### 返回示例

```
${JSONObjectUtil.dumpPretty(map)}
```

${dmd.generate()}

""")
            }

            return cols.join("\n")
        }

        String getSdkActionName() {
            Class clz = doc._rest._request._clz
            String aname = StringUtils.removeStart(clz.simpleName, "API")
            aname = (aname - "Msg") + "Action"
            return aname
        }

        String pythonSdk() {
            Class clz = doc._rest._request._clz

            String actionName = getSdkActionName()

            def apiFields = getApiExampleOfTheClass(doc._rest._request._clz)
            def e = apiFields.find { k, v ->
                return !v.getClass().name.startsWith("java.")
            }

            if (e != null) {
                return "Python SDK未能自动生成"
            }

            List<String> cols = ["action = ${actionName}()"]
            cols.addAll(apiFields.collect { k, v ->
                if (v instanceof String) {
                    return "action.${k} = \"${v}\"".toString()
                } else {
                    return "action.${k} = ${v}".toString()
                }
            })

            if (!clz.isAnnotationPresent(SuppressCredentialCheck.class)) {
                cols.add("action.sessionId = \"b86c9016b4f24953a9edefb53ca0678c\"")
            }

            cols.add("res = action.call()")

            return """\
```
${cols.join("\n")}
```
"""
        }

        String javaSdk() {
            Class clz = doc._rest._request._clz

            String actionName = getSdkActionName()

            def apiFields = getApiExampleOfTheClass(doc._rest._request._clz)
            def e = apiFields.find { k, v ->
                // don't use v.class here, things break if the v is of type Map
                return !v.getClass().name.startsWith("java.")
            }

            if (e != null) {
                return "Java SDK未能自动生成"
            }

            def cols = ["${actionName} action = new ${actionName}();"]
            cols.addAll(apiFields.collect { k, v ->
                if (v instanceof String) {
                    return """action.${k} = "${v}";"""
                } else if (v instanceof Collection) {
                    Collection c = v as Collection
                    if (c.isEmpty()) {
                        return "action.${k} = new ArrayList();"
                    } else {
                        def item = c[0]

                        if (item instanceof String) {
                            def lst = c.collect {
                                return "\"" + it + "\""
                            }

                            return "action.${k} = asList(${lst.join(",")});"
                        } else {
                            return "action.${k} = asList(${c.join(",")});"
                        }
                    }

                } else {
                    return "action.${k} = ${v};"
                }
            })

            if (!clz.isAnnotationPresent(SuppressCredentialCheck.class)) {
                cols.add("""action.sessionId = "b86c9016b4f24953a9edefb53ca0678c";""")
            }

            cols.add("${actionName}.Result res = action.call();")

            return """\
```
${cols.join("\n")}
```
"""
        }

        String generate() {
            def urlScript = ""
            def headerScript = ""
            def requestExampleScript = ""
            def curlExampleScript = ""
            def paramsScript = ""
            def responseDescScript = ""
            def responseExampleScript = ""
            def javaSdkScript = ""
            def pythonSdkScript = ""

            try {
                urlScript = url()
            } catch (Exception e) { logger.warn("failed to generate url of ${doc._title}", e) }
            try {
                headerScript = headers()
            } catch (Exception e) { logger.warn("failed to generate header of ${doc._title}", e) }
            try {
                requestExampleScript = requestExample()
            } catch (Exception e) { logger.warn("failed to generate requestExample of ${doc._title}", e) }
            try {
                curlExampleScript = curlExample()
            } catch (Exception e) { logger.warn("failed to generate curlExample of ${doc._title}", e) }
            try {
                paramsScript = params()
            } catch (Exception e) { logger.warn("failed to generate params of ${doc._title}", e) }
            try {
                responseDescScript = responseDesc()
            } catch (Exception e) { logger.warn("failed to generate responseDesc of ${doc._title}", e) }
            try {
                responseExampleScript = responseExample()
            } catch (Exception e) { logger.warn("failed to generate responseExample of ${doc._title}", e) }
            try {
                javaSdkScript = javaSdk()
            } catch (Exception e) { logger.warn("failed to generate javaSdk of ${doc._title}", e) }
            try {
                pythonSdkScript = pythonSdk()
            } catch (Exception e) { logger.warn("failed to generate pythonSdk of ${doc._title}", e) }

            return """\
## ${doc._category} - ${doc._title}

${doc._desc}

## API请求

${urlScript}

${headerScript}

${requestExampleScript}

${curlExampleScript}

${paramsScript}

## API返回

${responseDescScript}

${responseExampleScript}

## SDK示例

### Java SDK

${javaSdkScript}

### Python SDK

${pythonSdkScript}
"""
        }
    }

    class DataStructMarkDown {
        Doc doc
        Class clz
        Map<Class, String> resolvedRefs = [:]

        DataStructMarkDown(Class clz, Doc doc) {
            this.clz = clz
            this.doc = doc
        }

        String generate() {
            def tables = []

            def rows = ["|名字|类型|描述|起始版本|"]
            rows.add("|---|---|---|---|")

            doc._fields.each {
                def row = []
                row.add(it._name)
                row.add(it._type)
                row.add(it._desc)
                row.add(it._since)

                rows.add("|${row.join("|")}|")
            }

            doc._refs.each {
                def row = []
                row.add(it._name)
                row.add(it._type)
                if (it._overrideDesc == null) {
                    row.add("详情参考[${it._name}](#${it._path})")
                } else if (it._overrideDesc) {
                    row.add(it._desc)
                } else {
                    row.add("${it._desc}。 详情参考[${it._name}](#${it._path})")
                }
                row.add(it._since)

                rows.add("|${row.join("|")}|")
            }

            tables.add(rows.join("\n"))

            def refs = doc._refs.findAll { clz != it._clz }

            // generate dependent tables

            refs.each {
                if (it._clz.name.startsWith("java.")) {
                    return ""
                }

                String txt = resolvedRefs[it._clz]

                if (txt == null) {
                    String path = getDocTemplatePathFromClass(it._clz)
                    Doc refDoc = createDoc(path)
                    def dmd = new DataStructMarkDown(it._clz, refDoc)
                    dmd.resolvedRefs = resolvedRefs
                    txt = dmd.generate()
                    resolvedRefs[it._clz] = txt
                }

                tables.add("""\

<a name="${it._path}"> **#${it._name}**<a/>

${txt}
""")
            }

            return tables.join("\n")
        }
    }

    String getDocTemplatePathFromClass(Class clz) {
        boolean inner = clz.getEnclosingClass() != null
        String srcFileName = (inner ? clz.getEnclosingClass().simpleName : clz.simpleName) - ".java"

        File srcFile = getSourceFile(srcFileName)
        String docName = makeFileNameForChinese(clz)
        return PathUtil.join(srcFile.parent, docName)
    }

    def PRIMITIVE_TYPES = [
            int.class,
            long.class,
            float .class,
            boolean .class,
            double.class,
            short.class,
            char.class,
            String.class,
            Enum.class,
    ]

    class ApiResponseDocTemplate {
        Set<Class> laterResolveClasses = []

        Class responseClass
        RestResponse at

        Set<String> imports = []
        Map<String, Object> fsToAdd = [:]
        List<String> fieldStrings = []

        ApiResponseDocTemplate(Class responseClass) {
            this.responseClass = responseClass

            at = responseClass.getAnnotation(RestResponse.class)
            if (at != null) {
                findFieldsForRestResponse()
            } else {
                findFieldsForNormalClass()
            }
        }

        def getAllFieldsAndEnums(Class clazz) {
            List<Object> allValues = new ArrayList<Object>()

            while (true) {
                if (Enum.class.isAssignableFrom(clazz)) {
                    Enum[] es = clazz.getEnumConstants()

                    if (es != null) {
                        Collections.addAll(allValues, es)
                    }
                } else {
                    Field[] fs = clazz.getDeclaredFields()
                    Collections.addAll(allValues, fs)
                }

                clazz = clazz.getSuperclass()

                if (clazz == Object.class) {
                    break
                }
            }

            return allValues
        }

        def findFieldsForNormalClass() {
            List<Object> allAttributes = getAllFieldsAndEnums(responseClass)
            allAttributes = allAttributes.findAll {
                if (it instanceof Field && !it.isAnnotationPresent(APINoSee.class) && !Modifier.isStatic(it.modifiers)) {
                    return true
                } else return it instanceof Enum
            }
            allAttributes.each {
                if (it instanceof Field) {
                    fsToAdd[it.name] = it
                } else if (it instanceof Enum) {
                    fsToAdd[it.toString()] = it
                }
            }
        }

        def findFieldsForRestResponse() {
            if (at.allTo() == null && at.fieldsTo().length == 0) {
                return ""
            }

            if (at.allTo() != "") {
                fsToAdd[at.allTo()] = getFieldRecursively(responseClass, at.allTo())
                supplementFields('success', responseClass, at)
                supplementFields('error', responseClass, at)
            } else if (at.fieldsTo().length == 1 && at.fieldsTo()[0] == "all") {
                findFieldsForNormalClass()
            } else {
                supplementFields('success', responseClass, at)
                supplementFields('error', responseClass, at)
                at.fieldsTo().each {
                    def kv = it.split("=")

                    if (kv.length == 1) {
                        fsToAdd[kv[0]] = FieldUtils.getField(kv[0], responseClass)
                    } else {
                        def k = kv[0].trim()
                        def v = kv[1].trim()
                        fsToAdd[k] = FieldUtils.getField(v, responseClass)
                    }
                }
            }

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

        void supplementFields(String fieldName, Class clz, RestResponse at){
            if(!at.fieldsTo().contains(fieldName)){
                fsToAdd.put(fieldName, FieldUtils.getField(fieldName, clz))
            }
        }

        String createField(String n, String desc, String type) {
            if (MUTUAL_FIELDS.containsKey(n)) {
                desc = "${desc == null || desc.isEmpty() ? MUTUAL_FIELDS[n] : MUTUAL_FIELDS[n] + "," + desc}"
            }

            return """\tfield {
\t\tname "${n}"
\t\tdesc "${desc == null ? "" : desc}"
\t\ttype "${type}"
\t\tsince "${projectVersion}"
\t}"""
        }

        String createRef(String n, String path, String desc, String type, Class clz) {
            DebugUtils.Assert(!PRIMITIVE_TYPES.contains(clz), "${clz.name} is a primitive class!!!")
            if (!ErrorCode.getClass().isAssignableFrom(clz)) {
                laterResolveClasses.add(clz)
            }
            imports.add("import ${clz.canonicalName}")

            return """\tref {
\t\tname "${n}"
\t\tpath "${path}"
\t\tdesc "${desc}"
\t\ttype "${type}"
\t\tsince "${projectVersion}"
\t\tclz ${clz.simpleName}.class
\t}"""
        }

        String fields() {
            fsToAdd.each { k, v ->


                if (v instanceof Enum) {
                    logger.info("generating enum ${responseClass.name}.${k} ${v.class.getSimpleName()}")
                    fieldStrings.add(createField(k, "", v.class.getSimpleName()))
                } else if (v instanceof Field) {
                    logger.info("generating field ${responseClass.name}.${k} ${v.type.name}")
                    if (PRIMITIVE_TYPES.contains(v.type)) {
                        fieldStrings.add(createField(k, "", v.type.simpleName))
                    } else if (v.type.name.startsWith("java.")) {
                        if (Collection.class.isAssignableFrom(v.type) || Map.class.isAssignableFrom(v.type)) {
                            Class gtype = FieldUtils.getGenericType(v)

                            if (gtype == null || gtype.name.startsWith("java.")) {
                                fieldStrings.add(createField(k, "", v.type.simpleName))
                            } else {
                                fieldStrings.add(createRef(k, "${responseClass.canonicalName}.${v.name}", null, v.type.simpleName, gtype))
                            }
                        } else {
                            // java time but not primitive, needs import
                            imports.add("import ${v.type.name}")
                            fieldStrings.add(createField(k, "", v.type.simpleName))
                        }
                    } else {
                        String desc = null
                        if(ErrorCode.isAssignableFrom(v.type)){
                            desc = "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
                        }
                        fieldStrings.add(createRef("${k}", "${responseClass.canonicalName}.${v.name}", desc, v.type.simpleName, v.type))
                    }
                }

            }

            return fieldStrings.join("\n")
        }

        String generate() {
            String fieldStr = fields()

            return """${responseClass.package}

${imports.join("\n")}

doc {

\ttitle "在这里输入结构的名称"

${fieldStr}
}
"""
        }
    }

    class ApiRequestDocTemplate {
        Class apiClass
        File sourceFile
        RestRequest at

        Set<String> imports = []
        static Map<String, String> apiCategories = [:]

        static {
            // Categories from PackageDescription routes.
            // Intentionally use APIMessage.apiMessageClasses only: classes without @RestRequest
            // (e.g. APIGenerateApiJsonTemplateMsg — CloudBus/internal APIs) are skipped because
            // this is the RestDocumentation generator, not a full API catalog.
            try {
                APIMessage.apiMessageClasses.each { Class clz ->
                    String sid = ApiRouteUtils.resolveServiceIdFromRegistry(clz)
                    if (sid != null) {
                        apiCategories[clz.name] = sid
                    }
                }
            } catch (Throwable ignored) {
            }
        }

        ApiRequestDocTemplate(Class apiClass) {
            this.apiClass = apiClass
            this.sourceFile = getSourceFile(apiClass.simpleName - ".java")
            at = apiClass.getAnnotation(RestRequest.class)
            imports.add("import ${at.responseClass().canonicalName}")
        }

        String headers() {
            if (apiClass.isAnnotationPresent(SuppressCredentialCheck.class)) {
                return ""
            }

            return "\t\t\t" + """header (Authorization: '${RestConstants.HEADER_OAUTH} the-session-uuid')"""
        }

        String getParamName() {
            if (at.parameterName() != "" && at.parameterName() != "null") {
                return at.parameterName()
            } else if (at.isAction()) {
                return paramNameFromClass(apiClass)
            } else {
                return null
            }
        }

        String params() {
            if (APIQueryMessage.class.isAssignableFrom(apiClass)) {
                imports.add("import ${APIQueryMessage.class.canonicalName}")

                return """\t\t\tparams ${APIQueryMessage.class.simpleName}.class"""
            }

            def apiFields = getApiFieldsOfClass(apiClass)

            if (apiFields.isEmpty()) {
                return ""
            }

            String paramName = getParamName()

            List<String> urlVars = getVarNamesFromUrl(at.path())
            def cols = []
            for (Field af : apiFields) {
                APIParam ap = af.getAnnotation(APIParam.class)

                String values = null
                if (ap != null && ap.validValues().length != 0) {
                    def l = []
                    ap.validValues().each {
                        l.add("\"${it}\"")
                    }
                    values = "values (${l.join(",")})"
                } else if (ap != null && ap.validEnums().length != 0) {
                    def l = []
                    def validValues = CollectionUtils.valuesForEnums(ap.validEnums()).collect(Collectors.toList())
                    validValues.each {
                        l.add("\"${it}\"")
                    }
                    values = "values (${l.join(",")})"
                }

                String desc = MUTUAL_FIELDS.get(af.name)

                String enclosedIn
                if (af.name == "systemTags" || af.name == "userTags") {
                    enclosedIn = ""
                } else {
                    enclosedIn = paramName == null ? "" : paramName
                }

                def location
                if (urlVars.contains(af.name)) {
                    location = LOCATION_URL
                } else if (at.method() == HttpMethod.GET || at.method() == HttpMethod.DELETE) {
                    location = LOCATION_QUERY
                } else {
                    location = LOCATION_BODY
                }

                def col = """\t\t\t\tcolumn {
\t\t\t\t\tname "${af.name}"
\t\t\t\t\tenclosedIn "${enclosedIn}"
\t\t\t\t\tdesc "${desc == null  ? "" : desc}"
\t\t\t\t\tlocation "${location}"
\t\t\t\t\ttype "${af.type.simpleName}"
\t\t\t\t\toptional ${ap == null ? true : !ap.required()}
\t\t\t\t\tsince "${projectVersion}"
"""
                if (values != null) {
                    col += "\t\t\t\t\t${values}\n"
                }

                col += "\t\t\t\t}"
                cols.add(col)
            }

            if (cols.isEmpty()) {
                return ""
            }

            return """\t\t\tparams {

${cols.join("\n")}
\t\t\t}"""
        }

        String imports() {
            return imports.isEmpty() ? "" : imports.join("\n")
        }

        String urls() {
            Set<String> paths = []
            if ("null" != at.path()) {
                paths.add(at.path())
            }

            paths.addAll(at.optionalPaths())
            paths = paths.collect {
                return "${at.method().toString()} ${RestConstants.API_VERSION}${it}"
            }

            return paths.collect {
                return "\t\t\t" + "url \"${it}\""
            }.join("\n")
        }

        String generate(Doc doc) {
            String paramString
            if (doc._rest._request._params instanceof RequestParamRef) {
                imports.add("import ${doc._rest._request._params.refClass.canonicalName}")
                paramString = "\t\t\tparams ${doc._rest._request._params.refClass.simpleName}.class"
            } else {
                List cols = doc._rest._request._params._cloumns.collect {
                    String values = it._values != null && !it._values.isEmpty() ? "(${it._values.collect { "\"$it\"" }.join(",")})" : null

                    if (values == null) {
                        return """\t\t\t\tcolumn {
\t\t\t\t\tname "${it._name}"
\t\t\t\t\tenclosedIn "${it._enclosedIn}"
\t\t\t\t\tdesc "${it._desc}"
\t\t\t\t\tlocation "${it._location}"
\t\t\t\t\ttype "${it._type}"
\t\t\t\t\toptional ${it._optional}
\t\t\t\t\tsince "${it._since}"
\t\t\t\t}"""
                    } else {
                        return """\t\t\t\tcolumn {
\t\t\t\t\tname "${it._name}"
\t\t\t\t\tenclosedIn "${it._enclosedIn}"
\t\t\t\t\tdesc "${it._desc}"
\t\t\t\t\tlocation "${it._location}"
\t\t\t\t\ttype "${it._type}"
\t\t\t\t\toptional ${it._optional}
\t\t\t\t\tsince "${it._since}"
\t\t\t\t\tvalues ${values}
\t\t\t\t}"""
                    }
                }

                paramString = """\t\t\tparams {

${cols.join("\n")}
\t\t\t}"""
            }

            return """package ${apiClass.package.name}

${imports()}

doc {
\ttitle "${doc._title}"

\tcategory "${doc._category}"

\tdesc \"\"\"${doc._desc}\"\"\"

\trest {
\t\trequest {
${doc._rest._request._urls.collect { "\t\t\t" + it.toString() }.join("\n")}

${doc._rest._request._headers.collect { "\t\t\t" + it.toString() }.join("\n")}

\t\t\tclz ${doc._rest._request._clz.simpleName}.class

\t\t\tdesc \"\"\"${doc._rest._request._desc}\"\"\"

${paramString}
\t\t}

\t\tresponse {
\t\t\tclz ${doc._rest._response._clz.simpleName}.class
\t\t}
\t}
}"""
        }

        String generate() {
            String paramString = params()

            String title = StringUtils.removeStart(apiClass.simpleName, "API")
            title = StringUtils.removeEnd(title, "Msg")
            String category = apiCategories[apiClass.name]
            category = category == null ? at.category() : category
            if (category == "") {
                category = "未知类别"
            }

            return """package ${apiClass.package.name}

${imports()}

doc {
\ttitle "${title}"

\tcategory "${category}"

\tdesc \"\"\"在这里填写API描述\"\"\"

\trest {
\t\trequest {
${urls()}

${headers()}

\t\t\tclz ${apiClass.simpleName}.class

\t\t\tdesc \"\"\"\"\"\"

${paramString}
\t\t}

\t\tresponse {
\t\t\tclz ${at.responseClass().simpleName}.class
\t\t}
\t}
}"""
        }

        void repair(String docFilePath) {
            Doc oldDoc
            if (!new File(docFilePath).exists()) {
                logger.info("cannot find ${docFilePath}, generate a doc template as repairing result")
                oldDoc = createDocFromString(generate())
            } else {
                oldDoc = createDoc(docFilePath)
                Doc newDoc = createDocFromString(generate())
                oldDoc.merge(newDoc)
            }

            new File(docFilePath).write generate(oldDoc)
            logger.info("re-written a request doc template ${docFilePath}")
        }

        void generateDocFile(DocMode mode) {
            def docFilePath = getDocTemplatePathFromClass(apiClass)

            if (mode == DocMode.RECREATE_ALL) {
                new File(docFilePath).write generate()
                logger.info("written a request doc template ${docFilePath}")
            } else if (mode == DocMode.REPAIR) {
                repair(docFilePath)
            } else if (mode == DocMode.CREATE_MISSING) {
                if (!new File(docFilePath).exists()) {
                    new File(docFilePath).write generate()
                    logger.info("written a request doc template ${docFilePath}")
                } else {
                    logger.info("${docFilePath} exists, skip it")
                }
            } else {
                throw new CloudRuntimeException("unknown doc mode ${mode}")
            }
        }
    }

    class Config {
        String name = ""
        String description = ""
        String type = ""
        String category = ""
        String valueRange = ""
        String defaultValue = ""
        List<String> resources = new ArrayList<>()
    }

    class GlobalConfigMarkDown {
        Config globalConfig
        String name_CN = PLACEHOLDER + "中文名-必填##"
        String desc_CN = PLACEHOLDER + "该条目的作用是什么-必填##"
        String defaultValueRemark = PLACEHOLDER + "对默认值的解读-如无需写：无##"
        String additionalRemark = PLACEHOLDER + "该条目有哪些注意事项-如无需写：无##"
        String resourcesGranularitiesRemark = PLACEHOLDER + "该条目支持的资源粒度-如无需写：无##"
        String backgroundInformation = PLACEHOLDER + "触发该条目增删改的背景-如无需写：无##"
        String isUIExposed = PLACEHOLDER + "该条目是否需UI暴露？-必填##"
        String isCLIExposed = PLACEHOLDER + "该条目是否需CLI手册暴露？-必填##"
        String valueRangeRemark = PLACEHOLDER + "对取值范围的解读-如无需写：无##"

        GlobalConfigMarkDown(GlobalConfig config, List<String> classes, String validatorString) {
            translate(config)
            if (validatorString == null) {
                if (config.type == "java.lang.Boolean") {
                    validatorString = "{true, false}"
                } else {
                    validatorString = ""
                }
            }
            globalConfig.valueRange = validatorString
            globalConfig.resources = classes
        }

        GlobalConfigMarkDown() {
            globalConfig = new Config()
        }

        void translate(GlobalConfig gc) {
            globalConfig = new Config()
            globalConfig.name = gc.name
            globalConfig.description = gc.description
            globalConfig.description = gc.description == null ? "" : gc.description
            globalConfig.category = gc.category
            globalConfig.type = gc.type == null ? "java.lang.String" : gc.type
            globalConfig.defaultValue = gc.defaultValue == null ? "" : gc.defaultValue
        }

        void merge(GlobalConfigMarkDown newMd) {
            if (globalConfig.name != newMd.globalConfig.name) {
                name_CN = PLACEHOLDER + name_CN
            }
            if (globalConfig.valueRange != newMd.globalConfig.valueRange) {
                if (globalConfig.valueRange != null || !newMd.globalConfig.valueRange.isEmpty()) {
                    valueRangeRemark = PLACEHOLDER + valueRangeRemark
                }
            }
            if (globalConfig.defaultValue != newMd.globalConfig.defaultValue) {
                defaultValueRemark = PLACEHOLDER + defaultValueRemark
            }
            globalConfig = newMd.globalConfig
        }

        String generate() {
            String text = ""
            if (!globalConfig.resources.isEmpty()) {
                text = """||
|---|
|${globalConfig.resources.join("\n|")}"""
            }
            return """
## Name

```
${globalConfig.name}(${name_CN})
```

### Description

```
${globalConfig.description}
```

### 含义

```
${desc_CN}
```

### Type

```
${globalConfig.type}
```

### Category

```
${globalConfig.category}
```

### 取值范围

```
${globalConfig.valueRange}
```

### 取值范围补充说明

```
${valueRangeRemark}
```

### DefaultValue

```
${globalConfig.defaultValue}
```

### 默认值补充说明

```
${defaultValueRemark}
```

### 支持的资源级配置

${text}

### 资源粒度说明

```
${resourcesGranularitiesRemark}
```

### 背景信息

```
${backgroundInformation}
```

### UI暴露

```
${isUIExposed}
```

### CLI手册暴露

```
${isCLIExposed}
```

## 注意事项

```
${additionalRemark}
```
"""
        }
    }

    List<Field> getApiFieldsOfClass(Class apiClass) {
        def apiFields = []

        FieldUtils.getAllFields(apiClass).each {
            if (it.isAnnotationPresent(APINoSee.class)) {
                return
            }

            if (Modifier.isStatic(it.modifiers)) {
                return
            }

            apiFields.add(it)
        }

        return apiFields
    }

    void generateResponseDocTemplate(List<Class> aClasses, DocMode mode) {
        Set<Class> resolved = []

        aClasses.each {
            logger.info("generating response doc template for class[${it.name}]")
            String path = getDocTemplatePathFromClass(it)
            if (new File(path).exists() && mode != DocMode.RECREATE_ALL) {
                logger.info("${path} exists, skip it")
                return
            }

            Set<Class> todo = []
            Map<String, String> docFiles = [:]
            def tmp = new ApiResponseDocTemplate(it)
            docFiles[path] = tmp.generate()
            todo.addAll(tmp.laterResolveClasses)

            while (!todo.isEmpty()) {
                Set<Class> set = []
                todo.each {
                    def t = new ApiResponseDocTemplate(it)
                    path = getDocTemplatePathFromClass(it)
                    docFiles[path] = t.generate()

                    resolved.add(it)
                    set.addAll(t.laterResolveClasses)
                }

                todo = set - resolved
            }

            docFiles = docFiles.findAll { p, _ ->
                def fname = new File(p).name

                // for pre-defined doc templates, won't generate them
                return [ErrorCode.class].find {
                    return fname.startsWith(it.simpleName)
                } == null
            }

            docFiles.each { k, v ->
                def f = new File(k)
                f.write(v)
                logger.info("written doc template ${k}")
            }
        }
    }

    File getSourceFile(String n) {
        File f = sourceFiles[n]
        if (f == null) {
            throw new CloudRuntimeException("cannot find source file ${n}.java")
        }

        return f
    }

    List<String> getVarNamesFromUrl(String url) {
        Pattern pattern = Pattern.compile("\\{(.+?)\\}")
        Matcher matcher = pattern.matcher(url)

        List<String> urlVars = []
        while (matcher.find()) {
            urlVars.add(matcher.group(1))
        }

        return urlVars
    }

    String substituteUrl(String url, Map<String, Object> tokens) {
        Pattern pattern = Pattern.compile("\\{(.+?)\\}")
        Matcher matcher = pattern.matcher(url)
        StringBuffer buffer = new StringBuffer()
        while (matcher.find()) {
            String varName = matcher.group(1)
            Object replacement = tokens.get(varName)
            if (replacement == null) {
                throw new CloudRuntimeException(String.format("cannot find value for URL variable[%s]", varName))
            }

            matcher.appendReplacement(buffer, "")
            buffer.append(replacement.toString())
        }

        matcher.appendTail(buffer)
        return buffer.toString()
    }

    def paramNameFromClass(Class clz) {
        def paramName = StringUtils.removeStart(clz.simpleName, "API")
        paramName = StringUtils.removeEnd(paramName, "Msg")
        paramName = StringUtils.uncapitalize(paramName)
        return paramName
    }

    def scanDocTemplateFiles() {
        ShellResult res = ShellUtils.runAndReturn("find ${rootPath} -name '*Doc_*.groovy' -not -path '${rootPath}/sdk/*'")
        List<String> paths = res.stdout.split("\n")
        paths = paths.findAll { !(it - "\n" - "\r" - "\t").trim().isEmpty() }
        return paths
    }

    def scanJavaSourceFiles() {
        ShellResult res = ShellUtils.runAndReturn("find ${rootPath} -name '*.java' -not -path '${rootPath}/sdk/*'")
        List<String> paths = res.stdout.split("\n")
        paths = paths.findAll { !(it - "\n" - "\r" - "\t").trim().isEmpty()}

        paths.each {
            def f = new File(it)
            def key = f.name - ".java"
            if (sourceFiles[key] == null) {
                sourceFiles[key] = f
                return
            }

            // Two class names have the same name
            // 1. Exclude classes containing '/plugin/expon/' paths
            if (sourceFiles[key].absolutePath.contains("/plugin/expon/")) {
                sourceFiles[key] = f
            }
        }
    }

    boolean ignoreError() {
        return System.getProperty("ignoreError") != null
    }

    void testGlobalConfigTemplateAndMarkDown() {
        Map<String, GlobalConfig> allConfigs = initializer.configs
        allConfigs.each {
            String newPath =
                    PathUtil.join(PathUtil.join(Paths.get("../doc").toAbsolutePath().normalize().toString(),
                            "globalconfig"), it.value.category, it.value.name) + DEPRECATED + ".md"
            if (new File(newPath).exists()) {
                return
            }
            String mdPath =
                    PathUtil.join(PathUtil.join(Paths.get("../doc").toAbsolutePath().normalize().toString(),
                            "globalconfig"), it.value.category, it.value.name) + ".md"
            File mdFile = new File(mdPath)
            if (!mdFile.exists()) {
                throw new CloudRuntimeException("Not found the document markdown of the global config ${it.value.name} , please generate it first.")
            }
            checkMD(mdPath, it.value)
        }
    }
}
