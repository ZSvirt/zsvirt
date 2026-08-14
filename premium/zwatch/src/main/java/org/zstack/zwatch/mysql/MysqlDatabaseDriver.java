package org.zstack.zwatch.mysql;

import org.zstack.header.Component;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.zwatch.api.APIGetAllMetricMetadataReply;
import org.zstack.zwatch.datatype.*;
import org.zstack.zwatch.driver.DatabaseDriver;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.zstack.core.Platform.operr;

public class MysqlDatabaseDriver implements Component, DatabaseDriver {
    private Map<String, MysqlNamespace> namespaces = new HashMap<>();

    @Override
    public boolean start() {
        Namespace.namespaces.forEach((name, namespaceList) -> {
            namespaceList.forEach(ns -> {
                if (MysqlDatabaseDriver.class == ns.getDatabaseDriver().getClass() && ns.getMetrics() != null && !ns.getMetrics().isEmpty()) {
                    namespaces.put(name, MysqlNamespace.getMysqlNamespace(ns));
                }
            });
        });

        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public List<Datapoint> query(MetricQueryObject queryObject) {
        MysqlNamespace ns = namespaces.get(queryObject.getNamespaceName());
        if (ns == null) {
            throw new OperationFailureException(operr("no mysql namespace[%s] found", queryObject.getNamespaceName()));
        }
        return ns.query(queryObject);
    }

    @Override
    public List<Map> queryLabelValues(LabelValueQueryObject qo) {
        MysqlNamespace ns = namespaces.get(qo.getNamespaceName());
        if (ns == null) {
            throw new OperationFailureException(operr("no mysql namespace[%s] found", qo.getNamespaceName()));
        }
        return ns.queryLabelValues(qo);
    }

    @Override
    public List<String> getAllMetricNames() {
        return null;
    }

    @Override
    public Map<String, List<String>> queryPrometheusLabelValues(LabelValueQueryObject qo) {
        throw new UnsupportedOperationException("MySQL database driver does not support queryPrometheusLabelValues yet");
    }

    @Override
    public List<APIGetAllMetricMetadataReply.MetricStruct> getAllNonZStackMetricMetadata(Map<String, String> filteredLabels) {
        return null;
    }

    @Override
    public boolean deleteAll(String namespaceName, String metricName, List<Label> labels) {
        return true;
    }
}
