package org.zstack.core;

import org.zstack.core.rest.RESTApiConstant;
import org.zstack.core.validator.AvailableValues;
import org.zstack.core.validator.NumberRange;

import java.util.List;

/**
 */
@GlobalPropertyDefinition
public class CoreGlobalProperty {
    @GlobalProperty(name = "unitTestOn", defaultValue = "false")
    @AvailableValues(value ={"true","false"})
    public static boolean UNIT_TEST_ON;
    @GlobalProperty(name = "beanRefContextConf", defaultValue = "beanRefContext.xml")
    public static String BEAN_REF_CONTEXT_CONF;
    @GlobalProperty(name = "beanConf", defaultValue = "zstack.xml")
    public static String BEAN_CONF;
    @GlobalProperty(name = "vmTracerOn", defaultValue = "true")
    public static boolean VM_TRACER_ON;
    @GlobalProperty(name = "profiler.workflow", defaultValue = "false")
    public static boolean PROFILER_WORKFLOW;
    @GlobalProperty(name = "profiler.httpCall", defaultValue = "false")
    public static boolean PROFILER_HTTP_CALL;
    @GlobalProperty(name = "exitJVMOnBootFailure", defaultValue = "true")
    public static boolean EXIT_JVM_ON_BOOT_FAILURE;
    @GlobalProperty(name = "checkBoxTypeInInventory", defaultValue = "false")
    public static boolean CHECK_BOX_TYPE_IN_INVENTORY;
    @GlobalProperty(name = "pidFilePath", defaultValue = "{user.home}/management-server.pid")
    public static String PID_FILE_PATH;
    @GlobalProperty(name = "consoleProxyOverriddenIp", defaultValue = "0.0.0.0")
    public static String CONSOLE_PROXY_OVERRIDDEN_IP;
    @GlobalProperty(name = "exposeSimulatorType", defaultValue = "false")
    public static boolean EXPOSE_SIMULATOR_TYPE;
    @GlobalProperty(name = "exitJVMOnStop", defaultValue = "true")
    public static boolean EXIT_JVM_ON_STOP;
    @GlobalProperty(name = "locale", defaultValue = "en_US")
    public static String LOCALE;
    @GlobalProperty(name = "user.home")
    public static String USER_HOME;
    @GlobalProperty(name = "RESTFacade.readTimeout", defaultValue = "300000")
    public static int REST_FACADE_READ_TIMEOUT;
    @GlobalProperty(name = "RESTFacade.connectTimeout", defaultValue = "15000")
    public static int REST_FACADE_CONNECT_TIMEOUT;
    @GlobalProperty(name = "RESTFacade.echoTimeout", defaultValue = "60")
    public static int REST_FACADE_ECHO_TIMEOUT;
    @GlobalProperty(name = "RESTFacade.maxPerRoute", defaultValue = "2")
    public static int REST_FACADE_MAX_PER_ROUTE;
    @GlobalProperty(name = "RESTFacade.maxTotal", defaultValue = "128")
    public static int REST_FACADE_MAX_TOTAL;
    @GlobalProperty(name = "RESTFacade.keepAliveTimeMillis", defaultValue = "5000")
    public static int REST_FACADE_KEEPALIVE_TIME_MILLIS;
    /**
     * When set RestServer.maskSensitiveInfo to true, sensitive info will be
     * masked see @NoLogging.
     *
     * Set default value as false to keep back-compatible to avoid breaking users who
     * depend on plaintext API result
     */
    @GlobalProperty(name="Rest.maskSensitiveInfo", defaultValue = "false")
    public static boolean MASK_SENSITIVE_INFO;
    @GlobalProperty(name = "upgradeStartOn", defaultValue = "false")
    public static boolean IS_UPGRADE_START;
    @GlobalProperty(name = "shadowEntityOn", defaultValue = "false")
    public static boolean SHADOW_ENTITY_ON;
    @NumberRange(min = 1, max = 65535)
    @GlobalProperty(name = "consoleProxyPort", defaultValue = "4900")
    public static int CONSOLE_PROXY_PORT;       // for vnc
    @GlobalProperty(name = "httpConsoleProxyPort", defaultValue = "4901")
    @NumberRange(min = 1, max = 65535)
    public static int HTTP_CONSOLE_PROXY_PORT;  // for terminal
    @GlobalProperty(name = "consoleProxyCertFile", defaultValue = "")
    public static String CONSOLE_PROXY_CERT_FILE;
    @GlobalProperty(name = "dataDir", defaultValue = "/var/lib/zstack/")
    public static String DATA_DIR;
    @GlobalProperty(name = "logDir", defaultValue = "{catalina.home}/logs/")
    public static String LOG_DIR;
    @GlobalProperty(name="chrony.serverIp.")
    public static List<String> CHRONY_SERVERS;
    @GlobalProperty(name="management.server.vip")
    public static String MN_VIP;
    @GlobalProperty(name = "simulatorsOn", defaultValue = "false")
    public static boolean SIMULATORS_ON;
    @GlobalProperty(name = "startMode", defaultValue = "")
    public static String START_MODE;
    @GlobalProperty(name = "updatePkgWhenConnect", defaultValue = "true")
    public static boolean UPDATE_PKG_WHEN_CONNECT;
    @GlobalProperty(name = "syncNodeTime", defaultValue = "true")
    public static boolean SYNC_NODE_TIME;
    @GlobalProperty(name = "enableElaboration", defaultValue = "true")
    public static boolean ENABLE_ELABORATION;
    @GlobalProperty(name = "recordElaboration", defaultValue = "false")
    public static boolean RECORD_TO_DB_ELABORATION;
    @GlobalProperty(name = "chain.task.qos", defaultValue = "false")
    public static boolean CHAIN_TASK_QOS;
    @NumberRange(min = RESTApiConstant.REST_API_RESULT_MIN_LENGTH, max = RESTApiConstant.REST_API_RESULT_MAX_LENGTH)
    @GlobalProperty(name = "rest.api.result.max.length", defaultValue = "64000")
    public static int REST_API_RESULT_MAX_LENGTH;
    @GlobalProperty(name = "pending.queue.minimum.threshold", defaultValue = "50")
    public static int PENDING_QUEUE_MINIMUM_THRESHOLD;

    @GlobalProperty(name = "core.reply.location.url.protocol", defaultValue = "http")
    public static String REPLY_LOCATION_URL_PROTOCOL;
    @GlobalProperty(name = "core.reply.location.url.ip", defaultValue = "") // Note: null -> use callbackHostName
    public static String REPLY_LOCATION_URL_IP;
    @GlobalProperty(name = "core.reply.location.url.port", defaultValue = "0")
    @NumberRange(min = 0, max = 65535) // Note: 0 -> use default value (management node service port)
    public static int REPLY_LOCATION_URL_PORT;
}
