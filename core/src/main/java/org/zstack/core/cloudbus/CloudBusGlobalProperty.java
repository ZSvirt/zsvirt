package org.zstack.core.cloudbus;

import org.zstack.core.GlobalProperty;
import org.zstack.core.GlobalPropertyDefinition;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 */
@GlobalPropertyDefinition
public class CloudBusGlobalProperty {
    @GlobalProperty(name="CloudBus.serverIp.", required = true)
    public static List<String> SERVER_IPS;
    @GlobalProperty(name="CloudBus.closeTracker", defaultValue = "false")
    public static boolean CLOSE_TRACKER;
    @GlobalProperty(name="CloudBus.messageTrackerGarbageCollectorInterval", defaultValue = "600")
    public static int TRACKER_GARBAGE_COLLECTOR_INTERVAL;
    @GlobalProperty(name="CloudBus3ManagementNodeLifeCycleTracker.timeoutMessageCleanupInterval", defaultValue = "600")
    public static int CLOUDBUS3_MESSAGE_TRACKER_CLEANUP_INTERVAL;
    @GlobalProperty(name="CloudBus.messageLogFilterAll", defaultValue = "true")
    public static boolean MESSAGE_LOG_FILTER_ALL;
    @GlobalProperty(name="CloudBus.messageLog")
    public static String MESSAGE_LOG;
    @GlobalProperty(name="CloudBus.readAPILogOff", defaultValue = "true")
    public static boolean READ_API_LOG_OFF;
    @GlobalProperty(name="CloudBus.rabbitmqUsername")
    public static String RABBITMQ_USERNAME;
    @GlobalProperty(name="CloudBus.rabbitmqPassword")
    public static String RABBITMQ_PASSWORD;
    @GlobalProperty(name="CloudBus.rabbitmqVirtualHost")
    public static String RABBITMQ_VIRTUAL_HOST;
    @GlobalProperty(name="CloudBus.rabbitmqHeartbeatTimeout", defaultValue = "60")
    public static int RABBITMQ_HEART_BEAT_TIMEOUT;
    @GlobalProperty(name="CloudBus.rabbitmqConnectionTimeout", defaultValue = "10")
    public static int RABBITMQ_CONNECTION_TIMEOUT;
    @GlobalProperty(name="CloudBus.rabbitmqRetryDelayOnReturn", defaultValue = "5")
    public static int RABBITMQ_RETRY_DELAY_ON_RETURN;
    @GlobalProperty(name="CloudBus.rabbitmqRecoverableSendTimes", defaultValue = "5")
    public static int RABBITMQ_RECOVERABLE_SEND_TIMES;
    @GlobalProperty(name="CloudBus.rabbitmqNetworkRecoveryInterval", defaultValue = "1")
    public static int RABBITMQ_NETWORK_RECOVER_INTERVAL;
    @GlobalProperty(name="CloudBus.compressNonApiMessage", defaultValue = "false")
    public static boolean COMPRESS_NON_API_MESSAGE;
    @GlobalProperty(name="CloudBus.channelPoolSize", defaultValue = "100")
    public static int CHANNEL_POOL_SIZE;
    @GlobalProperty(name="CloudBus.messageTTL", defaultValue = "300")
    public static int MESSAGE_TTL;
    @GlobalProperty(name="CloudBus.httpPort", defaultValue = "8080")
    public static int HTTP_PORT;
    @GlobalProperty(name="CloudBus.httpMaxConnection", defaultValue = "50")
    public static int HTTP_MAX_CONN;
    @GlobalProperty(name="CloudBus.alwaysUseHttp", defaultValue = "false")
    public static boolean HTTP_ALWAYS;
    @GlobalProperty(name="CloudBus.httpContextPath", defaultValue = "/zstack")
    public static String HTTP_CONTEXT_PATH;
    // Unit of SYNC_CALL_TIMEOUT: millisecond
    @GlobalProperty(name="Cloudbus.syncCallTimeout", defaultValue = "900000")
    public static int SYNC_CALL_TIMEOUT;
}
