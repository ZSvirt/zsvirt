package org.zstack.premium.externalservice.grafana;

import org.zstack.header.configuration.PythonClass;

/**
 * Created by mingjian.deng on 2019/8/21.
 */
@PythonClass
public interface GrafanaConstant {
    String SERVICE_ID = "grafana";
    String ACTION_CATEGORY = "grafana";

    String InitialGrafanaData = "grafanaData/dashboard_db_data";
}
