package org.zstack.snmp.agent.mib;

import org.snmp4j.smi.OctetString;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.timeout.TimeHelper;
import org.zstack.zwatch.datatype.Datapoint;
import org.zstack.zwatch.datatype.Label;
import org.zstack.zwatch.datatype.MetricQueryObject;
import org.zstack.zwatch.datatype.Namespace;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @Author : jingwang
 * @create 2023/8/10 19:28
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class PrometheusSnmpGetHandler implements SnmpGetHandler {
    @Autowired
    TimeHelper timeHelper;

    @Override
    public OctetString handle(String namespace, String metricName, String uuid) {
        String namespaceCanonicalName = Namespace.zstackNamespaceName(namespace);
        Namespace ns = Namespace.getMetricNameSpace(namespaceCanonicalName, metricName);
        MetricQueryObject qo = buildMetricQueryObject(ns, metricName, uuid);
        List<Datapoint> data = ns.query(qo);
        if (!data.isEmpty()) {
            return new OctetString(String.valueOf(data.get(data.size() - 1).getValue()));
        }
        return new OctetString();
    }

    public MetricQueryObject buildMetricQueryObject(Namespace ns, String metricName, String uuid) {
        long endTime = TimeUnit.MILLISECONDS.toSeconds(timeHelper.getCurrentTimeMillis());
        long startTime = endTime;
        Label label = new Label(String.format("%s%s%s", ns.getBasicIdentityLabelName(), Label.Operator.Regex, uuid));
        return MetricQueryObject.New()
                .namespace(ns.getName())
                .startTime(startTime)
                .endTime(endTime)
                .labels(Collections.singletonList(label))
                .metricName(metricName)
                .build();
    }
}
