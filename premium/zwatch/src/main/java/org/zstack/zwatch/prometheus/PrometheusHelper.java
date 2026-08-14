package org.zstack.zwatch.prometheus;

import org.zstack.premium.externalservice.prometheus.PrometheusGlobalProperty;

public class PrometheusHelper {
    static boolean isPrometheusDisabled() {
        return PrometheusGlobalProperty.VERSION_MODE.equals("none");
    }
}
