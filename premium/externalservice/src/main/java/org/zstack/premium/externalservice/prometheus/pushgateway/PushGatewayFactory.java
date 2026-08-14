package org.zstack.premium.externalservice.prometheus.pushgateway;

import org.zstack.core.externalservice.ExternalServiceFactory;
import org.zstack.core.externalservice.ExternalServiceType;
import org.zstack.header.Component;
import org.zstack.header.core.BypassWhenUnitTest;
import org.zstack.premium.externalservice.prometheus.PreparePrometheusConfigExtensionPoint;
import org.zstack.premium.externalservice.prometheus.PrometheusConfig;

public class PushGatewayFactory implements ExternalServiceFactory, PreparePrometheusConfigExtensionPoint, Component {
    public static final ExternalServiceType type = new ExternalServiceType("PrometheusPushGateway");

    private PushGateway instance;

    @Override
    public String getExternalServiceType() {
        return type.toString();
    }

    public PushGateway getPushGateway() {
        return instance;
    }

    @Override
    @BypassWhenUnitTest
    public boolean start() {
        instance = new PushGatewayImpl();
        instance.start();
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public void prepareConfig(PrometheusConfig config) {
        PushGatewayImpl.preparePrometheusConfig(config);
    }
}
