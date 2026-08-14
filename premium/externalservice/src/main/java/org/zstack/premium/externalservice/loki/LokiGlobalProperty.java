package org.zstack.premium.externalservice.loki;

import org.zstack.core.GlobalProperty;
import org.zstack.core.GlobalPropertyDefinition;

/**
 * Created by mingjian.deng on 2019/9/6.
 */
@GlobalPropertyDefinition
public class LokiGlobalProperty {
    @GlobalProperty(name = "loki.server.port", defaultValue = "3100")
    public static int LOKI_SERVER_PORT;
    @GlobalProperty(name = "promtail.port", defaultValue = "9080")
    public static int PROMTAIL_PORT;
    @GlobalProperty(name = "loki.grpc.port", defaultValue = "9095")
    public static int LOKI_GRPC_PORT;
    /**
     * available values are 0.3.0, none
     * none means stop loki service
     */
    @GlobalProperty(name="Loki.versionMode", defaultValue = "none")
    public static String LOKI_VERSION_MODE;
    @GlobalProperty(name="Loki.promtail.ansiblePlaybook", defaultValue = "promtail.py")
    public static String PROMTAIL_PLAYBOOK_NAME;

    public static boolean isLokiOn() {
        return !LOKI_VERSION_MODE.equals("none");
    }
}
