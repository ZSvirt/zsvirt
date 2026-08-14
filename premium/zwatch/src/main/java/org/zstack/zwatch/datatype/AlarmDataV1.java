package org.zstack.zwatch.datatype;

import org.zstack.utils.DebugUtils;
import org.zstack.zwatch.ZWatchConstants;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class AlarmDataV1 extends AlarmData {
    public static final String TAG_ALARM_UUID = "alarmUuid";
    public static final String TAG_RESOURCE_UUID = "resourceUuid";
    public static final String TAG_RESOURCE_TYPE = "resourceType";
    public static final String TAG_ACCOUNT_UUID = "accountUuid";
    public static final String TAG_METRIC_NAME = "metricName";
    public static final String TAG_NAMESPACE = "namespace";
    public static final String TAG_ALARM_STATUS = "alarmStatus";
    public static final String TAG_DATA_UUID = "dataUuid";


    public static final String FIELD_CONTEXT = "context";
    public static final String FIELD_READ_STATUS = ZWatchConstants.DATA_READ_STATUS;

    public static Set<String> queryableLabels = new HashSet<>();

    static {
        queryableLabels.add(TAG_RESOURCE_UUID);
        queryableLabels.add(TAG_RESOURCE_TYPE);
        queryableLabels.add(TAG_ALARM_UUID);
        queryableLabels.add(TAG_ACCOUNT_UUID);
        queryableLabels.add(TAG_METRIC_NAME);
        queryableLabels.add(TAG_NAMESPACE);
        queryableLabels.add(TAG_ALARM_STATUS);
        queryableLabels.add(TAG_DATA_UUID);

        queryableLabels.add(FIELD_READ_STATUS);
    }

    @Override
    public Map<String, Object> asFields() {
        Map<String, Object> fields = new HashMap<>();
        fields.put("alarmName", alarmName);
        fields.put("threshold", threshold);
        fields.put("period", period);
        fields.put("labels", labels);
        fields.put("metricValue", metricValue);
        fields.put("comparisonOperator", comparisonOperator);
        fields.put(FIELD_CONTEXT, context);
        fields.put(ZWatchConstants.DATA_READ_STATUS, readStatus);
        return fields;
    }

    @Override
    public Map<String, String> asTags() {
        DebugUtils.Assert(namespace != null, "namespace cannot be null");
        DebugUtils.Assert(metricName != null, "metricName cannot be null");
        DebugUtils.Assert(accountUuid != null, "accountUuid cannot be null");
        DebugUtils.Assert(alarmUuid != null, "alarmUuid cannot be null");
        DebugUtils.Assert(alarmStatus != null, "alarmStatus cannot be null");

        Map<String, String> tags = new HashMap<>();
        tags.put(TAG_ACCOUNT_UUID, accountUuid);
        tags.put(TAG_RESOURCE_UUID, resourceUuid);
        tags.put(TAG_RESOURCE_TYPE, resourceType);
        tags.put(TAG_METRIC_NAME, metricName);
        tags.put(TAG_ALARM_UUID, alarmUuid);
        tags.put(TAG_NAMESPACE, namespace);
        tags.put(TAG_ALARM_STATUS, alarmStatus);
        tags.put(TAG_DATA_UUID, dataUuid);

        return tags;
    }
}
