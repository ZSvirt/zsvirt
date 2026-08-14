package org.zstack.zwatch.legacy;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.monitoring.MonitorConstants;
import org.zstack.monitoring.MonitorProvider;
import org.zstack.monitoring.MonitorProviderFactory;

public class ZWatchProviderFactory implements MonitorProviderFactory {
    @Autowired
    private ZWatchProvider provider;

    @Override
    public String getMonitorProviderType() {
        return MonitorConstants.PROMETHEUS_PROVIDER;
    }

    @Override
    public MonitorProvider createProvider() {
        return provider;
    }
}
