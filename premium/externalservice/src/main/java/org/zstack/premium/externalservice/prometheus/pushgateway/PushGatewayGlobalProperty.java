package org.zstack.premium.externalservice.prometheus.pushgateway;

import org.zstack.core.GlobalProperty;
import org.zstack.core.GlobalPropertyDefinition;

/**
 * Created by xing5 on 2016/7/6.
 */
@GlobalPropertyDefinition
public class PushGatewayGlobalProperty {
    @GlobalProperty(name="Prometheus.pushgateway.webListenAddress", defaultValue = "AUTO")
    public static String LISTEN_ADDRESS;
    @GlobalProperty(name="Prometheus.pushgateway.persistence.file", defaultValue = "AUTO")
    public static String PERSISTENCE_FILE;
    @GlobalProperty(name="Prometheus.pushgateway.persistence.interval", defaultValue = "1m")
    public static String PERSISTENCE_INTERVAL;
}
