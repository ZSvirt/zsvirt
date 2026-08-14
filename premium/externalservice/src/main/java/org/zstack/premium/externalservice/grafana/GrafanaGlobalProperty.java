package org.zstack.premium.externalservice.grafana;

import org.zstack.core.GlobalProperty;
import org.zstack.core.GlobalPropertyDefinition;

/**
 * Created by mingjian.deng on 2019/8/21.
 */
@GlobalPropertyDefinition
public class GrafanaGlobalProperty {
    @GlobalProperty(name = "grafana.db.path", defaultValue = "/var/lib/grafana/grafana.db")
    public static String GRAFANA_DATA_PATH;
    @GlobalProperty(name = "grafana.server.port", defaultValue = "3000")
    public static int GRAFANA_SERVER_PORT;
    /**
     * available values are 6.4.2, none
     * none means stop grafana service
     */
    @GlobalProperty(name="Grafana.versionMode", defaultValue = "none")
    public static String VERSION_MODE;
}
