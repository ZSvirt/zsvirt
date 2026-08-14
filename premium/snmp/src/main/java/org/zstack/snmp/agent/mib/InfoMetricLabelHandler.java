package org.zstack.snmp.agent.mib;

import org.snmp4j.smi.OctetString;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.zwatch.datatype.Datapoint;
import org.zstack.zwatch.datatype.MetricQueryObject;
import org.zstack.zwatch.datatype.Namespace;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Generic handler for querying Info metrics (VmInfo, HostInfo, etc.) and extracting label values
 * Can be reused across different resource types (VM, Host, PrimaryStorage, etc.)
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class InfoMetricLabelHandler extends PrometheusSnmpGetHandler {
    private static final CLogger logger = Utils.getLogger(InfoMetricLabelHandler.class);

    private final String infoMetricName;  // e.g., "VmInfo", "HostInfo"

    public InfoMetricLabelHandler(String infoMetricName) {
        this.infoMetricName = infoMetricName;
    }

    @Override
    public OctetString handle(String namespace, String labelName, String uuid) {
        String namespaceCanonicalName = Namespace.zstackNamespaceName(namespace);
        Namespace ns = Namespace.getMetricNameSpace(namespaceCanonicalName, infoMetricName);
        if (ns.getInfoMetric() == null || !Objects.equals(ns.getInfoMetric().getName(), infoMetricName)) {
            logger.warn(String.format("namespace[%s] has no Info metric defined", namespace));
            return new OctetString("");
        }

        MetricQueryObject qo = buildMetricQueryObject(ns, infoMetricName, uuid);
        List<Datapoint> data = ns.query(qo);

        if (!data.isEmpty()) {
            Datapoint dp = data.get(data.size() - 1);
            Map<String, String> labels = dp.getLabels();

            if (labels != null && labels.containsKey(labelName)) {
                String value = labels.get(labelName);
                return new OctetString(value != null ? value : "");
            } else {
                logger.warn(String.format("label %s not found in labels: %s", labelName, labels));
            }
        } else {
            logger.warn(String.format("No data returned for namespace[%s] metric[%s] with uuid %s", namespaceCanonicalName, infoMetricName, uuid));
        }

        return new OctetString("");
    }
}
