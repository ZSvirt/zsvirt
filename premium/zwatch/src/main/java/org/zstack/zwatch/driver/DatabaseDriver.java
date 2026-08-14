package org.zstack.zwatch.driver;

import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.zwatch.api.APIGetAllMetricMetadataReply;
import org.zstack.zwatch.datatype.*;

import java.util.List;
import java.util.Map;

public interface DatabaseDriver {
    List<Datapoint> query(MetricQueryObject queryObject);

    List<Map> queryLabelValues(LabelValueQueryObject qo);

    Map<String, List<String>> queryPrometheusLabelValues(LabelValueQueryObject qo);

    List<String> getAllMetricNames();

    List<APIGetAllMetricMetadataReply.MetricStruct> getAllNonZStackMetricMetadata(Map<String, String> filteredLabels);

    boolean deleteAll(String namespaceName, String metricName, List<Label> labels);

    default void write(String namespaceName, List<MetricDatum> data) {
        throw new CloudRuntimeException("unimplemetned operation");
    }
}
