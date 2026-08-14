package org.zstack.zwatch.datatype;

import org.zstack.header.rest.SDK;

import java.util.HashMap;

import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

// the custom data points written by applications
@SDK
public class MetricDatum extends Datapoint {
    protected String metricName;

    public MetricDatum() {
        labels = new HashMap<>();
    }

    public String getMetricName() {
        return metricName;
    }

    public void setMetricName(String metricName) {
        this.metricName = metricName;
    }

    public static MetricDatum __example__() {
        MetricDatum ret = new MetricDatum();
        ret.metricName = "UserDefinedName";
        ret.value = 100.01;
        ret.labels = map(e("label1","value1"));
        return ret;
    }
}
