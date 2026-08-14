package org.zstack.core;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.LocaleUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.reflections.Reflections;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.web.context.WebApplicationContext;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.componentloader.ComponentLoaderImpl;
import org.zstack.core.config.GlobalConfigFacade;
import org.zstack.core.db.DatabaseGlobalProperty;
import org.zstack.core.encrypt.EncryptRSA;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.validator.ValidatorTool;
import org.zstack.core.search.SearchGlobalProperty;
import org.zstack.core.statemachine.StateMachine;
import org.zstack.core.statemachine.StateMachineImpl;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.Component;
import org.zstack.header.core.I18nMessage;
import org.zstack.header.core.StaticInit;
import org.zstack.header.core.encrypt.ENCRYPT;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.identity.AccountConstant;
import org.zstack.header.vo.BaseResource;
import org.zstack.utils.*;
import org.zstack.utils.data.StringTemplate;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.logging.CLoggerImpl;
import org.zstack.utils.network.NetworkUtils;
import org.zstack.utils.path.PathUtil;
import org.zstack.utils.string.Uuid;
import org.zstack.utils.zsha2.ZSha2Helper;
import org.zstack.utils.zsha2.ZSha2Info;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;
import static org.zstack.utils.StringDSL.ln;

public class Platform {
    private static final CLogger logger = CLoggerImpl.getLogger(Platform.class);
    public static final Object[] EMPTY_ARGS = new Object[0];

    private static ComponentLoader loader;
    private static String msId;
    private static String managementServerIp;
    private static String managementServerCidr;
    private static MessageSource messageSource;
    private static String encryptionKey = EncryptRSA.generateKeyString("ZStack open source");
    private static EncryptRSA rsa = new EncryptRSA();
    private static Map<String, Double> errorCounter = new HashMap<>();

    public static final String COMPONENT_CLASSPATH_HOME = "componentsHome";
    public static final String FAKE_UUID = "THIS_IS_A_FAKE_UUID";
    public static final String BASE_PACKAGE_NAME = AccountConstant.POLICY_BASE_PACKAGE;

    private static final Map<String, String> globalProperties = new HashMap<String, String>();

    private static Locale locale;

    public static volatile boolean IS_RUNNING = true;

    private static Reflections reflections = BeanUtils.reflections;

    public static Reflections getReflections() {
        return reflections;
    }

    public static Set<Method> encryptedMethodsMap;

    public static Map<String, String> childResourceToBaseResourceMap = new HashMap<>();

    static Map<Class, DynamicObjectMetadata> dynamicObjectMetadata = new HashMap<>();

    public static Locale getLocale() {
        return locale;
    }

    private static Map<String, String> linkGlobalPropertyMap(String prefix) {
        Map<String, String> ret = new HashMap<>();
        Map<String, String> map = getGlobalPropertiesStartWith(prefix);
        if (map.isEmpty()) {
            return ret;
        }

        for (Map.Entry<String, String> e : map.entrySet()) {
            String key = StringDSL.stripStart(e.getKey(), prefix).trim();
            ret.put(key, e.getValue().trim());
        }

        return ret;
    }

    private static void linkGlobalProperty(Class<?> clz, Map<String, String> propertiesMap) {
        for (Field f : clz.getDeclaredFields()) {
            GlobalProperty at = f.getAnnotation(GlobalProperty.class);
            if (at == null) {
                continue;
            }

            if (!Modifier.isStatic(f.getModifiers())) {
                throw new CloudRuntimeException(String.format("%s.%s is annotated by @GlobalProperty but it's not defined with static modifier", clz.getName(), f.getName()));
            }

            Object valueToSet = null;
            String name = at.name();
            if (Map.class.isAssignableFrom(f.getType())) {
                Map<String, String> ret = linkGlobalPropertyMap(name);
                if (ret.isEmpty() && at.required()) {
                    throw new IllegalArgumentException(String.format("A required global property[%s] missing in zstack.properties", name));
                }

                if (at.encrypted()) {
                    ret.forEach((k, v) -> ret.put(k, rsa.decrypt(v, encryptionKey)));
                }
                valueToSet = ret;
            }  else if (List.class.isAssignableFrom(f.getType())) {
                List<String> ret = linkGlobalPropertyList(name);
                if (ret.isEmpty() && at.defaultListValue().length > 0) {
                    ret = Arrays.asList(at.defaultListValue());
                }

                if (ret.isEmpty() && at.required()) {
                    throw new IllegalArgumentException(String.format("A required global property[%s] missing in zstack.properties", name));
                }

                if (at.encrypted()) {
                    ret = ret.stream().map(it -> rsa.decrypt(it, encryptionKey)).collect(Collectors.toList());
                }

                valueToSet = ret;
            } else {
                String value = propertiesMap.get(name);
                if (value == null && at.defaultValue().equals(GlobalProperty.DEFAULT_NULL_STRING) && at.required()) {
                    throw new IllegalArgumentException(String.format("A required global property[%s] missing in zstack.properties", name));
                }

                if (value == null) {
                    value = at.defaultValue();
                }

                if (GlobalProperty.DEFAULT_NULL_STRING.equals(value)) {
                    value = null;
                }

                if (value != null) {
                    if (at.encrypted()) {
                        value = rsa.decrypt(value, encryptionKey);
                    }
                    value = StringTemplate.substitute(value, propertiesMap);
                }

                if (Integer.class.isAssignableFrom(f.getType()) || Integer.TYPE.isAssignableFrom(f.getType())) {
                    valueToSet =  TypeUtils.stringToValue(value, Integer.class, 0);
                } else if (Long.class.isAssignableFrom(f.getType()) || Long.TYPE.isAssignableFrom(f.getType())) {
                    valueToSet =  TypeUtils.stringToValue(value, Long.class, 0L);
                } else if (Float.class.isAssignableFrom(f.getType()) || Float.TYPE.isAssignableFrom(f.getType())) {
                    valueToSet = TypeUtils.stringToValue(value, Float.class, 0F);
                } else if (Double.class.isAssignableFrom(f.getType()) || Double.TYPE.isAssignableFrom(f.getType())) {
                    valueToSet = TypeUtils.stringToValue(value, Double.class, 0D);
                } else if (String.class.isAssignableFrom(f.getType())) {
                    valueToSet = value;
                } else if (Boolean.class.isAssignableFrom(f.getType()) || Boolean.TYPE.isAssignableFrom(f.getType())) {
                    valueToSet = TypeUtils.stringToValue(value, Boolean.class);
                } else {
                    throw new CloudRuntimeException(String.format("%s.%s of type[%s] is unsupported by global property. try use Platform.getGlobalProperty() and parse by yourself",
                            clz.getName(), f.getName(), f.getType().getName()));
                }
            }

            f.setAccessible(true);
            try {
                f.set(null, valueToSet);
                globalProperties.put(name, valueToSet == null ? "null" : valueToSet.toString());
                if (logger.isTraceEnabled()) {
                    logger.trace(String.format("linked global property[%s.%s], value: %s", clz.getName(), f.getName(), valueToSet));
                }
            } catch (IllegalAccessException e) {
                throw new CloudRuntimeException(String.format("unable to link global property[%s.%s]", clz.getName(), f.getName()), e);
            }
        }
    }

    public static Map<String, String> getGlobalProperties() {
        return globalProperties;
    }

    private static List<String> linkGlobalPropertyList(String name) {
        Map<String, String> map = getGlobalPropertiesStartWith(name);
        List<String> ret = new ArrayList<>(map.size());
        if (map.isEmpty()) {
            return ret;
        }

        List<String> orderedKeys = new ArrayList<>(map.keySet());
        Collections.sort(orderedKeys);

        for (String key : orderedKeys) {
            String index = StringDSL.stripStart(key, name).trim();
            try {
                Long.parseLong(index);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(String.format("[Illegal List Definition] %s is an invalid list key" +
                        " definition, the last character must be a number, for example %s1. %s is not a number", key, key, index));

            }

            ret.add(map.get(key));
        }

        return ret;
    }

    private static void validateGlobalProperty() {
        ValidatorTool validatorTool = new ValidatorTool();

        System.getProperties().stringPropertyNames().forEach(name->{
            String value = System.getProperty(name);
            validatorTool.checkProperty(name, value);
        });
    }

    private static void linkGlobalProperty() {
        Set<Class<?>> clzs = reflections.getTypesAnnotatedWith(GlobalPropertyDefinition.class);

        boolean noTrim = System.getProperty("DoNotTrimPropertyFile") != null;

        List<String> lst = new ArrayList<String>();
        Map<String, String> propertiesMap = new HashMap<String, String>();
        for (final String name: System.getProperties().stringPropertyNames()) {
            String value = System.getProperty(name);
            if (!noTrim) {
                value = value.trim();
            }
            propertiesMap.put(name, value);
            lst.add(String.format("%s=%s", name, value));
        }

        logger.debug(String.format("system properties:\n%s", StringUtils.join(lst, ",")));

        for (Class<?> clz : clzs) {
            linkGlobalProperty(clz, propertiesMap);
        }
    }

    public static String getManagementPid() {
        if (CoreGlobalProperty.UNIT_TEST_ON) {
            return "";
        }
        return ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
    }

    private static void writePidFile() throws IOException {
        if (CoreGlobalProperty.UNIT_TEST_ON) {
            return;
        }

        File pidFile = new File(CoreGlobalProperty.PID_FILE_PATH);
        if (pidFile.exists()) {
            String pidStr = FileUtils.readFileToString(pidFile);
            try {
                long pid = Long.parseLong(pidStr);
                String processProcDir = String.format("/proc/%s", pid);
                File processProcDirFile = new File(processProcDir);
                if (processProcDirFile.exists()) {
                    throw new CloudRuntimeException(String.format("pid file[%s] exists and the process[pid:%s] that the pid file points to is still running", CoreGlobalProperty.PID_FILE_PATH, pidStr));
                }
            } catch (NumberFormatException e) {
                logger.warn(String.format("pid file[%s] includes an invalid pid[%s] that is not a long number, ignore it",
                        CoreGlobalProperty.PID_FILE_PATH, pidStr));
            }

            logger.info(String.format("stale pid file[%s], ignore it", CoreGlobalProperty.PID_FILE_PATH));
        }

        pidFile.deleteOnExit();
        String pid = ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
        FileUtils.writeStringToFile(pidFile, pid);
    }

    @SuppressWarnings("unchecked")
    private static void prepareDefaultDbProperties() {
        if (DatabaseGlobalProperty.DbUrl != null) {
            String dbUrl = DatabaseGlobalProperty.DbUrl;
            if (dbUrl.endsWith("/")) {
                dbUrl = dbUrl.substring(0, dbUrl.length()-1);
            }

            if (getGlobalProperty("DbFacadeDataSource.jdbcUrl") == null) {
                String url;
                if (dbUrl.contains("{database}")) {
                    url = ln(dbUrl).formatByMap(
                            map(e("database", "zstack"))
                    );
                    url = url.trim();
                } else {
                    url = String.format("%s/zstack", dbUrl);
                }

                System.setProperty("DbFacadeDataSource.jdbcUrl", url);
                logger.debug(String.format("default DbFacadeDataSource.jdbcUrl to DB.url [%s]", url));
            }
            if (getGlobalProperty("RESTApiDataSource.jdbcUrl") == null) {
                String url;
                if (dbUrl.contains("{database}")) {
                    url = ln(dbUrl).formatByMap(
                            map(e("database", "zstack_rest"))
                    );
                    url = url.trim();
                } else {
                    url = String.format("%s/zstack_rest", dbUrl);
                }

                System.setProperty("RESTApiDataSource.jdbcUrl", url);
                logger.debug(String.format("default RESTApiDataSource.jdbcUrl to DB.url [%s]", url));
            }
        }
        if (DatabaseGlobalProperty.DbUser != null) {
            if (getGlobalProperty("DbFacadeDataSource.user") == null) {
                System.setProperty("DbFacadeDataSource.user", DatabaseGlobalProperty.DbUser);
                logger.debug(String.format("default DbFacadeDataSource.user to DB.user [%s]", DatabaseGlobalProperty.DbUser));
            }
            if (getGlobalProperty("RESTApiDataSource.user") == null) {
                System.setProperty("RESTApiDataSource.user", DatabaseGlobalProperty.DbUser);
                logger.debug(String.format("default RESTApiDataSource.user to DB.user [%s]", DatabaseGlobalProperty.DbUser));
            }
        }
        if (DatabaseGlobalProperty.DbPassword != null) {
            if (getGlobalProperty("DbFacadeDataSource.password") == null) {
                System.setProperty("DbFacadeDataSource.password", DatabaseGlobalProperty.DbPassword);
                logger.debug(String.format("default DbFacadeDataSource.password to DB.password [%s]", DatabaseGlobalProperty.DbPassword));
            }
            if (getGlobalProperty("RESTApiDataSource.password") == null) {
                System.setProperty("RESTApiDataSource.password", DatabaseGlobalProperty.DbPassword);
                logger.debug(String.format("default RESTApiDataSource.password to DB.password [%s]", DatabaseGlobalProperty.DbPassword));
            }
        }
        if (DatabaseGlobalProperty.DbMaxIdleTime != null) {
            if (getGlobalProperty("DbFacadeDataSource.maxIdleTime") == null) {
                System.setProperty("DbFacadeDataSource.maxIdleTime", DatabaseGlobalProperty.DbMaxIdleTime);
                logger.debug(String.format("default DbFacadeDataSource.maxIdleTime to DB.maxIdleTime [%s]", DatabaseGlobalProperty.DbMaxIdleTime));
            }
            if (getGlobalProperty("ExtraDataSource.maxIdleTime") == null) {
                System.setProperty("ExtraDataSource.maxIdleTime", DatabaseGlobalProperty.DbMaxIdleTime);
                logger.debug(String.format("default ExtraDataSource.maxIdleTime to DB.maxIdleTime [%s]", DatabaseGlobalProperty.DbMaxIdleTime));
            }
            if (getGlobalProperty("RESTApiDataSource.maxIdleTime") == null) {
                System.setProperty("RESTApiDataSource.maxIdleTime", DatabaseGlobalProperty.DbMaxIdleTime);
                logger.debug(String.format("default RESTApiDataSource.maxIdleTime to DB.maxIdleTime [%s]", DatabaseGlobalProperty.DbMaxIdleTime));
            }
        }
        if (DatabaseGlobalProperty.DbIdleConnectionTestPeriod != null) {
            if (getGlobalProperty("DbFacadeDataSource.idleConnectionTestPeriod") == null) {
                System.setProperty("DbFacadeDataSource.idleConnectionTestPeriod", DatabaseGlobalProperty.DbIdleConnectionTestPeriod);
                logger.debug(String.format("default DbFacadeDataSource.idleConnectionTestPeriod to DB.idleConnectionTestPeriod [%s]", DatabaseGlobalProperty.DbIdleConnectionTestPeriod));
            }
            if (getGlobalProperty("ExtraDataSource.idleConnectionTestPeriod") == null) {
                System.setProperty("ExtraDataSource.idleConnectionTestPeriod", DatabaseGlobalProperty.DbIdleConnectionTestPeriod);
                logger.debug(String.format("default ExtraDataSource.idleConnectionTestPeriod to DB.idleConnectionTestPeriod [%s]", DatabaseGlobalProperty.DbIdleConnectionTestPeriod));
            }
            if (getGlobalProperty("RESTApiDataSource.idleConnectionTestPeriod") == null) {
                System.setProperty("RESTApiDataSource.idleConnectionTestPeriod", DatabaseGlobalProperty.DbIdleConnectionTestPeriod);
                logger.debug(String.format("default RESTApiDataSource.idleConnectionTestPeriod to DB.idleConnectionTestPeriod [%s]", DatabaseGlobalProperty.DbIdleConnectionTestPeriod));
            }
        }
    }

    private static void prepareHibernateSearchProperties() {
        if (!SearchGlobalProperty.SearchAutoRegister) {
            System.setProperty("Search.autoRegister", "false");
            logger.debug(String.format("default Search.autoRegister to Search.autoRegister [%s]", SearchGlobalProperty.SearchAutoRegister));
        }
        if (SearchGlobalProperty.SearchIndexBaseDir != null) {
            if (getGlobalProperty("Search.indexBaseDir") == null) {
                System.setProperty("Search.indexBaseDir", SearchGlobalProperty.SearchIndexBaseDir);
                logger.debug(String.format("default Search.indexBaseDir to Search.indexBaseDir [%s]", SearchGlobalProperty.SearchIndexBaseDir));
            }
        }
        if (SearchGlobalProperty.IndexWorkerExecution != null) {
            if (getGlobalProperty("IndexWorker.execution") == null) {
                System.setProperty("IndexWorker.execution", SearchGlobalProperty.IndexWorkerExecution);
                logger.debug(String.format("default IndexWorker.execution to IndexWorker.execution [%s]", SearchGlobalProperty.IndexWorkerExecution));
            }
        }
        if (SearchGlobalProperty.IndexWorkerFlushInterval != null) {
            if (getGlobalProperty("IndexWorker.flushInterval") == null) {
                System.setProperty("IndexWorker.flushInterval", SearchGlobalProperty.IndexWorkerFlushInterval);
                logger.debug(String.format("default IndexWorker.flushInterval to IndexWorker.flushIntervalr [%s]", SearchGlobalProperty.IndexWorkerFlushInterval));
            }
        }
        if (SearchGlobalProperty.JGroupInfinispanPort != null) {
            if (getGlobalProperty("JGroup.InfinispanPort") == null) {
                System.setProperty("JGroup.InfinispanPort", SearchGlobalProperty.JGroupInfinispanPort);
                logger.debug(String.format("default JGroup.InfinispanPort to JGroup.InfinispanPort [%s]", SearchGlobalProperty.JGroupInfinispanPort));
            }
        }
        if (SearchGlobalProperty.JGroupBackendPort != null) {
            if (getGlobalProperty("JGroup.BackendPort") == null) {
                System.setProperty("JGroup.BackendPort", SearchGlobalProperty.JGroupBackendPort);
                logger.debug(String.format("default JGroup.BackendPort to JGroup.BackendPort [%s]", SearchGlobalProperty.JGroupBackendPort));
            }
        }

        if (ZSha2Helper.isMNHaEnvironment()) {
            // jgroup configuration is required in multi node environment
            // if failed to get zsha2 info, this method thrown exception to stop management node from startup
            // so there is no need to handle exceptions
            ZSha2Info info = ZSha2Helper.getInfo(false);
            if (info.getNodeip() == null) {
                throw new RuntimeException("the ip of this node was null, please check the config of zsha2");
            }
            if (info.getPeerip() == null) {
                throw new RuntimeException("the ip of peer node was null, please check the config of zsha2");
            }
            SearchGlobalProperty.JGroupInfinispanInitialHosts = String.format("%s[%s],%s[%s]",
                    info.getNodeip(), SearchGlobalProperty.JGroupInfinispanPort,
                    info.getPeerip(), SearchGlobalProperty.JGroupInfinispanPort);
            SearchGlobalProperty.JGroupBackendInitialHosts = String.format("%s[%s],%s[%s]",
                    info.getNodeip(), SearchGlobalProperty.JGroupBackendPort,
                    info.getPeerip(), SearchGlobalProperty.JGroupBackendPort);
            if (getGlobalProperty("JGroup.TcppingInitialHosts") == null) {
                System.setProperty("JGroup.InfinispanInitialHosts", SearchGlobalProperty.JGroupInfinispanInitialHosts);
                logger.debug(String.format("default JGroup.InfinispanInitialHosts to JGroup.InfinispanInitialHosts [%s]", SearchGlobalProperty.JGroupInfinispanInitialHosts));
            }
            if (getGlobalProperty("JGroup.BackendInitialHosts") == null) {
                System.setProperty("JGroup.BackendInitialHosts", SearchGlobalProperty.JGroupBackendInitialHosts);
                logger.debug(String.format("default JGroup.BackendInitialHosts to JGroup.BackendInitialHosts [%s]", SearchGlobalProperty.JGroupBackendInitialHosts));
            }
        }
        if (getGlobalProperty("JGroup.Address") == null) {
            String serverIp = getCanonicalServerIp();
            System.setProperty("JGroup.Address", serverIp);
            logger.debug(String.format("default JGroup.Address to JGroup.Address [%s]", serverIp));
        }
        if (ZSha2Helper.isMNHaEnvironment()) {
            SearchGlobalProperty.JGroupFlushBypass = "false";
            SearchGlobalProperty.JGroupJoinTimeout = "5000";
        } else {
            SearchGlobalProperty.ExclusiveIndexUse = "true";
        }

        System.setProperty("Exclusive.indexUse", SearchGlobalProperty.ExclusiveIndexUse);
        System.setProperty("JGroup.FlushBypass", SearchGlobalProperty.JGroupFlushBypass);
        System.setProperty("JGroup.JoinTimeout", SearchGlobalProperty.JGroupJoinTimeout);
        logger.debug(String.format("default Exclusive.indexUse to Exclusive.indexUse [%s]", SearchGlobalProperty.ExclusiveIndexUse));
        logger.debug(String.format("default JGroup.FlushBypass to JGroup.FlushBypass [%s]", SearchGlobalProperty.JGroupFlushBypass));
        logger.debug(String.format("default JGroup.JoinTimeout to JGroup.JoinTimeout [%s]", SearchGlobalProperty.JGroupJoinTimeout));
    }

    static {
        FileInputStream in = null;
        try {
            Set<Class> baseResourceClasses = reflections.getTypesAnnotatedWith(BaseResource.class).stream()
                    .filter(clz -> clz.isAnnotationPresent(BaseResource.class)).collect(Collectors.toSet());
            for (Class clz : baseResourceClasses) {
                Set<Class> childResourceClasses = reflections.getSubTypesOf(clz);
                childResourceToBaseResourceMap.put(clz.getSimpleName(), clz.getSimpleName());
                for (Class child : childResourceClasses) {
                    childResourceToBaseResourceMap.put(child.getSimpleName(), clz.getSimpleName());
                }
            }

            File globalPropertiesFile = PathUtil.findFileOnClassPath("zstack.properties", true);

            in = new FileInputStream(globalPropertiesFile);
            System.getProperties().load(in);

            // get ms ip should after global property setup
            msId = Uuid.generate(getManagementServerIp().getBytes());

            collectDynamicObjectMetadata();
            linkGlobalProperty();
            validateGlobalProperty();
            prepareDefaultDbProperties();
            prepareHibernateSearchProperties();
            callStaticInitMethods();
            encryptedMethodsMap = getAllEncryptPassword();
            writePidFile();
        } catch (Throwable e) {
            logger.warn(String.format("unhandled exception when in Platform's static block, %s", e.getMessage()), e);
            new BootErrorLog().write(e.getMessage());
            if (CoreGlobalProperty.EXIT_JVM_ON_BOOT_FAILURE) {
                System.exit(1);
            } else {
                throw new RuntimeException(e);
            }
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    logger.warn(String.format("FileInputStream close IOException：%s", e.getMessage()));
                }
            }
        }
    }

    private static void collectDynamicObjectMetadata() {
        reflections.getSubTypesOf(DynamicObject.class).forEach(clz -> {
            DynamicObjectMetadata metadata = new DynamicObjectMetadata();
            FieldUtils.getAllFields(clz).forEach(f -> {
                f.setAccessible(true);
                metadata.fields.put(f.getName(), f);
            });

            Class p = clz;
            while (p != Object.class) {
                for (Method m : p.getDeclaredMethods()) {
                    m.setAccessible(true);
                    metadata.methods.put(m.getName(), m);
                }
                p = p.getSuperclass();
            }

            dynamicObjectMetadata.put(clz, metadata);
        });
    }

    public static String getBaseResourceType(String childResourceType) {
        String type = childResourceToBaseResourceMap.get(childResourceType);
        if (type == null) {
            type = childResourceType;
        }
        return type;
    }

    public static List<String> getAllChildrenResourceType(String baseResourceType) {
        return childResourceToBaseResourceMap.entrySet()
                .stream()
                .filter(map -> baseResourceType.equals(map.getValue()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    private static Set<Method> getAllEncryptPassword() {
        Set<Method> encrypteds = reflections.getMethodsAnnotatedWith(ENCRYPT.class);
        for (Method encrypted: encrypteds) {
            logger.debug(String.format("found encrypted method[%s:%s]", encrypted.getDeclaringClass(), encrypted.getName()));
        }
        return encrypteds;
    }

    private static void callStaticInitMethods() throws InvocationTargetException, IllegalAccessException {
        List<Method> inits = new ArrayList<>(reflections.getMethodsAnnotatedWith(StaticInit.class));
        inits.sort((o1, o2) -> {
            StaticInit a1 = o1.getAnnotation(StaticInit.class);
            StaticInit a2 = o2.getAnnotation(StaticInit.class);
            return a2.order() - a1.order();
        });

        for (Method init : inits)  {
            if (!Modifier.isStatic(init.getModifiers())) {
                throw new CloudRuntimeException(String.format("the method[%s:%s] annotated by @StaticInit is not a static method", init.getDeclaringClass(), init.getName()));
            }

            logger.debug(String.format("calling static init method[%s:%s]", init.getDeclaringClass(), init.getName()));
            init.setAccessible(true);
            init.invoke(null);
        }
    }

    private static void initMessageSource() {
        locale = LocaleUtils.toLocale(CoreGlobalProperty.LOCALE);
        logger.debug(String.format("using locale[%s] for i18n logging messages", locale.toString()));

        if (loader == null) {
            throw new CloudRuntimeException("ComponentLoader is null. i18n has not been initialized, you call it too early");
        }

        BeanFactory beanFactory = loader.getSpringIoc();
        if (beanFactory == null) {
            throw new CloudRuntimeException("BeanFactory is null. i18n has not been initialized, you call it too early");
        }

        if (!(beanFactory instanceof MessageSource)) {
            throw new CloudRuntimeException("BeanFactory is not a spring MessageSource. i18n cannot be used");
        }

        messageSource = (MessageSource)beanFactory;
    }

    private static CloudBus bus;

    {
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                if (bus != null) {
                    bus.stop();
                }
            }
        }));
    }

    public static String getGlobalProperty(String name) {
        return System.getProperty(name);
    }

    public static Map<String, String> getGlobalPropertiesStartWith(String prefix) {
        Properties props = System.getProperties();
        Enumeration<?> e = props.propertyNames();

        Map<String, String> ret = new HashMap<>();
        while (e.hasMoreElements()) {
            String key = (String) e.nextElement();
            if (key.startsWith(prefix)) {
                ret.put(key, System.getProperty(key));
            }
        }

        return ret;
    }

    public static ComponentLoader createComponentLoaderFromWebApplicationContext(WebApplicationContext webAppCtx) {
        assert loader == null;
        try {
            if (webAppCtx != null) {
                loader = new ComponentLoaderImpl(webAppCtx);
            } else {
                loader = new ComponentLoaderImpl();
            }
        } catch (Exception e) {
            String err = "unable to create ComponentLoader";
            logger.warn(e.getMessage(), e);
            throw new CloudRuntimeException(err, e);
        }

        loader.getPluginRegistry();
        GlobalConfigFacade gcf = loader.getComponent(GlobalConfigFacade.class);
        if (gcf != null) {
            ((Component)gcf).start();
        }

        ThreadFacade thdf = loader.getComponent(ThreadFacade.class);
        if (thdf != null) {
            thdf.start();
        }

        bus = loader.getComponentNoExceptionWhenNotExisting(CloudBus.class);
        if (bus != null)  {
            bus.start();
        }

        initMessageSource();

        return loader;
    }

    public static ComponentLoader getComponentLoader() {
		/*
		 * This part cannot be moved to static block at the beginning.
		 * Because component code loaded by Spring may call other functions in Platform which
		 * causes the static block to be executed, which results in cycle initialization of ComponentLoaderImpl.
		 */
        if (loader == null) {
            loader = createComponentLoaderFromWebApplicationContext(null);
        }

        return loader;
    }

    public static String getManagementServerId() {
        return msId;
    }

    public static <K extends Enum<K>, T extends Enum<T>> StateMachine<K, T> createStateMachine() {
        return new StateMachineImpl<K, T>();
    }

    public static String getUuid() {
        return Uuid.generate();
    }

    public static String getUuidFromBytes(byte[] name) {
        return Uuid.generate(name);
    }

    public static String getManagementServerIp() {
        if (managementServerIp == null) {
            managementServerIp = getManagementServerIpInternal();
        }

        return managementServerIp;
    }

    public static int getManagementNodeServicePort() {
        return Integer.parseInt(System.getProperty("RESTFacade.port", "8080"));
    }

    public static String getManagementServerVip() {
        if (!ZSha2Helper.isMNHaEnvironment()) {
            return getManagementServerIp();
        }
        return ZSha2Helper.getInfo(false).getDbvip();
    }

    public static String getCanonicalServerIp() {
        if (!ZSha2Helper.isMNHaEnvironment()) {
            return getManagementServerIp();
        }

        return ZSha2Helper.getInfo(false).getNodeip();
    }

    public static boolean isVIPNode() {
        if (!ZSha2Helper.isMNHaEnvironment() || CoreGlobalProperty.MN_VIP == null) {
            return true;
        }

        String vip = CoreGlobalProperty.MN_VIP;
        try {
            Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
            while (nets.hasMoreElements()) {
                NetworkInterface ifc = nets.nextElement();
                if (!ifc.isUp()) {
                    continue;
                }

                Enumeration<InetAddress> enumAdds = ifc.getInetAddresses();
                while (enumAdds.hasMoreElements()) {
                    if (vip.equals(enumAdds.nextElement().getHostAddress())) {
                        return true;
                    }
                }
            }
        } catch (SocketException e) {
            throw new CloudRuntimeException(e);
        }

        return false;
    }

    private static String getManagementServerCidrInternal() {
        String mgtIp = getManagementServerIp();

        /*# ip add | grep 10.86.4.132
            inet 10.86.4.132/23 brd 10.86.5.255 scope global br_eth0*/
        /* because Linux.shell can not run command with '|', pares the output of ip address in java  */
        Linux.ShellResult ret = Linux.shell("ip -4 add");
        for (String line : ret.getStdout().split("\\n")) {
            if (line.contains(mgtIp)) {
                line = line.trim();
                try {
                    return NetworkUtils.getNetworkAddressFromCidr(line.split(" ")[1]);
                } catch (RuntimeException e) {
                    return null;
                }
            }
        }

        return null;
    }

    public static String getManagementServerCidr() {
        if (managementServerCidr == null) {
            managementServerCidr = getManagementServerCidrInternal();
        }

        return managementServerCidr;
    }

    private static String getManagementServerIpInternal() {
        String ip = System.getProperty("management.server.ip");
        if (ip != null) {
            logger.info(String.format("get management IP[%s] from Java property[management.server.ip]", ip));
            return ip;
        }

        ip = System.getenv("ZSTACK_MANAGEMENT_SERVER_IP");
        if (ip != null) {
            logger.info(String.format("get management IP[%s] from environment variable[ZSTACK_MANAGEMENT_SERVER_IP]", ip));
            return ip;
        }

        Linux.ShellResult ret = Linux.shell("/sbin/ip route");
        String defaultLine = null;
        for (String s : ret.getStdout().split("\n")) {
            if (s.contains("default via")) {
                defaultLine = s;
                break;
            }
        }

        String err = "cannot get management server ip of this machine. there are three ways to get the ip.\n1) search for 'management.server.ip' java property\n2) search for 'ZSTACK_MANAGEMENT_SERVER_IP' environment variable\n3) search for default route printed out by '/sbin/ip route'\nhowever, all above methods failed";
        if (defaultLine == null) {
            throw new CloudRuntimeException(err);
        }

        try {
            Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
            for (NetworkInterface iface : Collections.list(nets)) {
                String name = iface.getName();
                if (defaultLine.contains(name)) {
                    for (InetAddress ia : Collections.list(iface.getInetAddresses())) {
                        ip = ia.getHostAddress();
                        if (ia instanceof Inet4Address) {
                            // we prefer IPv4 address
                            ip = ia.getHostAddress();
                            break;
                        }
                    }
                }
            }
        } catch (SocketException e) {
            throw new CloudRuntimeException(e);
        }

        if (ip == null) {
            throw new CloudRuntimeException(err);
        }

        logger.info(String.format("get management IP[%s] from default route[/sbin/ip route]", ip));
        return ip;
    }

    public static String toI18nString(String code, Object... args) {
        return toI18nString(code, null, args);
    }

    public static String toI18nString(String code, Locale l, List<Object> args) {
        return toI18nString(code, l, args.toArray(EMPTY_ARGS));
    }

    private static String toRawString(String code, Object... args) {
        Object[] detailsArgs = args == null || args.length == 0 ? EMPTY_ARGS : args.clone();
        for (int i = 0; i < detailsArgs.length; i++) {
            if (detailsArgs[i] instanceof I18nMessage) {
                detailsArgs[i] = ((I18nMessage) detailsArgs[i]).getDetails();
            }
        }
        return stringFormat(code, detailsArgs);
    }

    private static String stringFormat(String fmt, Object...args) {
        if (args == null || args.length == 0) {
            return fmt;
        } else {
            return String.format(fmt, args);
        }
    }

    public static String toI18nString(String code, Locale l, Object...args) {
        l = l == null ? locale : l;
        Object[] formatArgs = args == null || args.length == 0 ? EMPTY_ARGS : args.clone();
        for (int i = 0; i < formatArgs.length; i++) {
            if (formatArgs[i] instanceof I18nMessage) {
                formatArgs[i] = ((I18nMessage) formatArgs[i]).getI18nDetails();
            }
        }

        try {
            String ret;
            if (formatArgs.length > 0) {
                ret = messageSource.getMessage(code, formatArgs, l);
            } else {
                ret = messageSource.getMessage(code, null, l);
            }

            // if the result is an empty string which means the string is not translated in the locale,
            // return the original string so users won't get a confusing, empty string
            return ret.isEmpty() ? stringFormat(code, formatArgs) : ret;
        } catch (NoSuchMessageException e) {
            return stringFormat(code, formatArgs);
        }
    }

    public static I18nMessage i18m(String str, Object...args) {
        return I18nMessage.valueOf(toRawString(str, args), toI18nString(str, args));
    }

    public static String i18n(String str, Object...args) {
        return toI18nString(str, args);
    }

    public static String i18n(String str, Map<String, String> args) {
        Map<String, String> nargs = new HashMap<>();
        args.forEach((k, v) -> nargs.put(k, toI18nString(v)));

        return ln(toI18nString(str)).formatByMap(nargs);
    }

    public static boolean killProcess(int pid) {
        return killProcess(pid, 15);
    }

    public static boolean killProcess(int pid, Integer timeout) {
        timeout = timeout == null ? 30 : timeout;

        if (!TimeUtils.loopExecuteUntilTimeoutIgnoreExceptionAndReturn(timeout, 1, TimeUnit.SECONDS, () -> {
            ShellUtils.runAndReturn(String.format("kill %s", pid));
            return !new ProcessFinder().processExists(pid);
        })) {
            logger.warn(String.format("cannot kill the process[PID:%s] after %s seconds, kill -9 it", pid, timeout));
            ShellUtils.runAndReturn(String.format("kill -9 %s", pid));
        }

        if (!TimeUtils.loopExecuteUntilTimeoutIgnoreExceptionAndReturn(5, 1, TimeUnit.SECONDS, () -> !new ProcessFinder().processExists(pid))) {
            logger.warn(String.format("FAILED TO KILL -9 THE PROCESS[PID:%s], THE KERNEL MUST HAVE SOMETHING RUN", pid));
            return false;
        } else {
            return true;
        }
    }

    public static ErrorCode err(Enum<?> errCode, String fmt, Object...args) {
        ErrorCode errorCode = getComponentLoader().getComponent(ErrorFacade.class)
                .instantiateErrorCode(errCode, fmt, args);

        if (fmt != null && fmt.length() > 0) {
            try {
                errorCode.setI18nDetails(SysErrors.INTERNAL == errCode ? errorCode.getDetails() : toI18nString(fmt, args));
            } catch (Exception e) {
                logger.warn("exception happened when build error message i18n");
                logger.warn(e.getMessage());
                errorCode.setI18nDetails(errorCode.getDetails());
            }
        }

        addErrorCounter(errorCode);
        return errorCode;
    }

    public static ErrorCode multiErr(Collection<ErrorCode> causes, String fmt, Object...args) {
        return err(SysErrors.MULTIPLE_REASONS, fmt, args)
                .withCause(causes);
    }

    public static ErrorCode multiErr(Collection<ErrorCode> causes) {
        return err(SysErrors.MULTIPLE_REASONS, "Multiple errors")
                .withCause(causes);
    }

    public static ErrorCode multiErr(ErrorCodeList errorCodeList, String fmt, Object...args) {
        return err(SysErrors.MULTIPLE_REASONS, fmt, args)
                .withCause(errorCodeList);
    }

    public static ErrorCode multiErr(ErrorCodeList errorCodeList) {
        return err(SysErrors.MULTIPLE_REASONS, "Multiple errors")
                .withCause(errorCodeList);
    }

    private static ErrorCode getCoreError(ErrorCode result) {
        if (result.getCause() == null) {
            return result;
        } else {
            return getCoreError(result.getCause());
        }
    }

    public static String missingVariables(Object...args) {
        if (args.length == 1) {
            return String.format("[%s] is required", args[0]);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (Object arg : args) {
            if (arg == null) {
                continue;
            }

            sb.append(arg).append(", ");
        }
        sb.append("] ");
        return sb.append("are required").toString();
    }

    public static ErrorCode inerr(String fmt, Object...args) {
        return err(SysErrors.INTERNAL, fmt, args);
    }

    // format error code from expand components
    public static ErrorCode experr(String fmt, String err, Object...args) {
        return operr(fmt, err, args);
    }

    public static ErrorCode operr(String fmt, Object...args) {
        return err(SysErrors.OPERATION_ERROR, fmt, args);
    }

    public static ErrorCode canerr(String fmt, Object...args) {
        return err(SysErrors.CANCEL_ERROR, fmt, args);
    }

    public static ErrorCode argerr(String fmt, Object...args) {
        return err(SysErrors.INVALID_ARGUMENT_ERROR, fmt, args);
    }

    public static ErrorCode touterr(String fmt, Object...args) {
        return err(SysErrors.TIMEOUT, fmt, args);
    }

    public static ErrorCode ioerr(String fmt, Object...args) {
        return err(SysErrors.IO_ERROR, fmt, args);
    }

    public static ErrorCode httperr(String fmt, Object...args) {
        return err(SysErrors.HTTP_ERROR, fmt, args);
    }

    public static Function<Supplier, Object> functionForMockTestObject = (Supplier t) -> t.get();

    // This is to make objects created by keyword 'new' mockable
    // developers call this method as a factory method like:
    //
    // JavaMailSenderImpl sender = Platform.New(()-> new JavaMailSenderImpl());
    //
    // in unit tests, we can replace functionForMockTestObject with a function which returns a mocked
    // object, for example:
    //
    // Platform.functionForMockTestObject = (Supplier t) -? {
    //      Object obj = t.get();
    //      return Mockito.spy(obj);
    // }
    public static <T> T New(Supplier supplier) {
        return (T) functionForMockTestObject.apply(supplier);
    }

    public static final String EXIT_REASON = "zstack.quit.reason";

    public static final String SKIP_STOP = "skip.mn.exit";

    public static void exit(String reason) {
        new BootErrorLog().write(reason);
        System.setProperty(EXIT_REASON, reason);
        System.exit(1);
    }

    public static String randomAlphanumeric(int count) {
        return RandomStringUtils.randomAlphanumeric(count);
    }

    public static boolean isAfterManagementNodeStart(Timestamp ts) {
        RuntimeMXBean bean = ManagementFactory.getRuntimeMXBean();
        Timestamp startMnTime = new Timestamp(bean.getStartTime());
        return ts.after(startMnTime);
    }

    public static void addErrorCounter(ErrorCode code) {
        errorCounter.compute(code.getCode().split("\\.")[0], (k, v) -> v == null ? 1 : v ++);
    }

    public static Map<String, Double> getErrorCounter() {
        return errorCounter;
    }

    public static boolean isSimulatorOn() {
        return StartMode.SIMULATOR.toString().equals(CoreGlobalProperty.START_MODE) || CoreGlobalProperty.SIMULATORS_ON;
    }

    public static boolean isMinimalOn() {
        return StartMode.MINIMAL.toString().equals(CoreGlobalProperty.START_MODE);
    }
}
