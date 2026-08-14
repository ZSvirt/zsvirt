package org.zstack.zwatch.mysql;

import org.zstack.zwatch.datatype.*;
import org.zstack.zwatch.datatype.metric.Metric;

import java.util.*;

public abstract class AbstractMysqlNamespace implements MysqlNamespace {
    protected Namespace namespace;
    protected Map<String, Metric> metrics = new HashMap<>();

    public AbstractMysqlNamespace(Namespace namespace) {
        this.namespace = namespace;

        if (namespace.getMetrics() != null) {
            namespace.getMetrics().forEach(m -> metrics.put(m.getName(), m));
        }
    }

    @Override
    public List<Datapoint> query(MetricQueryObject queryObject) {
        List<Datapoint> dps = doQuery(queryObject);

        if (dps.isEmpty()) {
            return new ArrayList<>();
        }

        return dps;
    }

    @Override
    public List<Map> queryLabelValues(LabelValueQueryObject qo) {
        return null;
    }

    protected abstract List<Datapoint> doQuery(MetricQueryObject queryObject);

    public List<Datapoint> transformSingleValueToDataPointList(double value) {
        Datapoint dp = new Datapoint();
        dp.setValue(value);
        dp.setTime(System.currentTimeMillis());
        dp.setLabels(new HashMap<>());

        return Collections.singletonList(dp);
    }

    public List<Datapoint> transformResourceQuotaUsageToDataPointList(List<AccountResourceQuotaData> resourceQuotas) {
        List<Datapoint> list = new ArrayList<>();

        for (AccountResourceQuotaData resourceQuota : resourceQuotas) {
            Datapoint dp = new Datapoint();
            dp.setValue(resourceQuota.getQuota() == 0 ? 0 : ((double)resourceQuota.getUseNumber() / resourceQuota.getQuota()) * 100);
            dp.setTime(System.currentTimeMillis());
            Map<String, String> label = new HashMap<>();
            label.put(resourceQuota.getResporceName(), resourceQuota.getResourceUuid());
            dp.setLabels(label);
            list.add(dp);
        }

        return list;
    }

}
