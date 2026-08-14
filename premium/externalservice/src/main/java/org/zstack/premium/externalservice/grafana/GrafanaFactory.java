package org.zstack.premium.externalservice.grafana;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.externalservice.ExternalServiceFactory;
import org.zstack.core.externalservice.ExternalServiceManager;
import org.zstack.core.externalservice.ExternalServiceType;
import org.zstack.header.Component;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

/**
 * Created by mingjian.deng on 2019/8/21.
 */
public class GrafanaFactory implements ExternalServiceFactory, Component {
    protected static final CLogger logger = Utils.getLogger(GrafanaFactory.class);
    public static final ExternalServiceType type = new ExternalServiceType("Grafana");
    @Autowired
    private ExternalServiceManager manager;

    private Grafana instance;
    private Grafana needCloseInstance;

    @Override
    public String getExternalServiceType() {
        return type.toString();
    }

    private void initialGrafana() {
        instance = (Grafana) manager.getService(new GrafanaImpl6().getName(), GrafanaImpl6::new);
    }

    private void closeGrafana() {
        needCloseInstance = (Grafana)manager.getService(new GrafanaImpl6().getName(), GrafanaImpl6::new);
    }

    @Override
    public boolean start() {
        String mode = GrafanaGlobalProperty.VERSION_MODE;
        switch (mode) {
            case "6.4.2": {
                initialGrafana();
                break;
            }
            case "none": {
                closeGrafana();
                break;
            }
            default: {
                throw new CloudRuntimeException(String.format("invalid value [%s] of GlobalProperty Grafana.versionMode has been found!", mode));
            }
        }
        if (needCloseInstance != null) {
            needCloseInstance.stop();
        }

        if (instance != null) {
            instance.start();
            instance.resetPassword("admin", GrafanaGlobalConfig.GRAFANA_ADMIN_PASSWORD.value());
            instance.resetPrometheusDataSource();
            instance.resetMysqlDataSource();
        }

        GrafanaGlobalConfig.GRAFANA_ADMIN_PASSWORD.installUpdateExtension((oldConfig, newConfig) -> {
            if (instance != null && instance.isAlive()) {
                instance.resetPassword(oldConfig.value(), newConfig.value());
            }
        });

        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }
}
