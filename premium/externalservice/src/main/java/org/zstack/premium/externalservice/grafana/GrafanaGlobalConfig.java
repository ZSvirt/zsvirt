package org.zstack.premium.externalservice.grafana;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;

/**
 * Created by mingjian.deng on 2019/8/21.
 */
@GlobalConfigDefinition
public class GrafanaGlobalConfig {
    public static final String CATEGORY = "grafana";

    @GlobalConfigValidation
    public static GlobalConfig GRAFANA_ADMIN_PASSWORD = new GlobalConfig(CATEGORY, "grafana.admin.password");

}
