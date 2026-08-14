package org.zstack.zwatch.prometheus;

import io.prometheus.client.Collector;
import io.prometheus.client.exporter.HTTPServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.ResourceDestinationMaker;
import org.zstack.core.thread.AsyncThread;
import org.zstack.header.Component;
import org.zstack.header.core.BypassWhenUnitTest;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.premium.externalservice.prometheus.PrometheusGlobalProperty;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class PrometheusCollector extends Collector implements Component, ManagementNodeReadyExtensionPoint {
    private static CLogger logger = Utils.getLogger(PrometheusCollector.class);

    @Autowired
    private ResourceDestinationMaker destinationMaker;

    private HTTPServer server;
    private static List<MetricCollector> collectors = new ArrayList<>();

    public static void registerMetricCollector(MetricCollector c) {
        collectors.add(c);
    }

    static List<MetricCollector> getCollectors() {
        return collectors;
    }

    @AsyncThread
    private void asyncStart() {
        register();
    }

    @Override
    @BypassWhenUnitTest
    public boolean start() {
        try {
            server = new HTTPServer(PrometheusGlobalProperty.EXPORTER_PORT);
        } catch (IOException e) {
            throw new CloudRuntimeException(e);
        }

        return true;
    }

    @Override
    @BypassWhenUnitTest
    public boolean stop() {
        server.stop();
        return true;
    }

    @Override
    public List<MetricFamilySamples> collect() {
        List<MetricFamilySamples> metrics = new ArrayList<>();
        collectors.forEach(c -> {
            if (!c.skipManagementNodeCheck() && !destinationMaker.isManagedByUs(c.getCollectorName())) {
                return;
            }
            try {
                metrics.addAll(c.collect());
            } catch (Exception e) {
                // catch but not throw(print log), equals skip error collect
                logger.error(String.format("management-server-exporter collect [%s] error", c.getCollectorName()), e);
            }
        });
        return metrics;
    }

    @Override
    @BypassWhenUnitTest
    public void managementNodeReady() {
        asyncStart();
    }
}
