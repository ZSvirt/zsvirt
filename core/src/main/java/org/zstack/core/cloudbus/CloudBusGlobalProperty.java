package org.zstack.core.cloudbus;

import org.zstack.core.GlobalProperty;
import org.zstack.core.GlobalPropertyDefinition;

/**
 */
@GlobalPropertyDefinition
public class CloudBusGlobalProperty {
    @GlobalProperty(name="CloudBus3ManagementNodeLifeCycleTracker.timeoutMessageCleanupInterval", defaultValue = "600")
    public static int CLOUDBUS3_MESSAGE_TRACKER_CLEANUP_INTERVAL;
    @GlobalProperty(name="CloudBus.messageLogFilterAll", defaultValue = "true")
    public static boolean MESSAGE_LOG_FILTER_ALL;
    @GlobalProperty(name="CloudBus.messageLog")
    public static String MESSAGE_LOG;
    @GlobalProperty(name="CloudBus.readAPILogOff", defaultValue = "true")
    public static boolean READ_API_LOG_OFF;
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
