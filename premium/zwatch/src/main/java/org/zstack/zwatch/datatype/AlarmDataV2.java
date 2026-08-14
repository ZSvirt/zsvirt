package org.zstack.zwatch.datatype;

import org.zstack.header.rest.SDK;
import org.zstack.utils.DebugUtils;
import org.zstack.zwatch.ZWatchConstants;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@SDK
public class AlarmDataV2 extends AlarmData {
    public static final String TAG_ALARM_UUID = "alarmUuid";
    public static final String TAG_ACCOUNT_UUID = "accountUuid";
    public static final String TAG_EMERGENCY_LEVEL = "emergencyLevel";

    public static final String FIELD_CONTEXT = "context";
    public static final String FIELD_DATAUUID = "dataUuid";
    public static final String FIELD_RESOURCE_TYPE = "resourceType";
    public static final String FIELD_ALARM_STATUS = "alarmStatus";
    public static final String FIELD_NAMESPACE = "namespace";
    public static final String FIELD_METRIC_NAME = "metricName";
    public static final String FIELD_RESOURCE_UUID = "resourceUuid";
    public static final String FIELD_READ_STATUS = ZWatchConstants.DATA_READ_STATUS;

    public static Set<String> queryableLabels = new HashSet<>();

    static {
        queryableLabels.add(FIELD_READ_STATUS);
        queryableLabels.add(TAG_ALARM_UUID);
        queryableLabels.add(TAG_ACCOUNT_UUID);
        queryableLabels.add(TAG_EMERGENCY_LEVEL);
        queryableLabels.add(FIELD_METRIC_NAME);
        queryableLabels.add(FIELD_ALARM_STATUS);
        queryableLabels.add(FIELD_NAMESPACE);
        queryableLabels.add(FIELD_DATAUUID);
        queryableLabels.add(FIELD_RESOURCE_TYPE);
        queryableLabels.add(FIELD_RESOURCE_UUID);
    }

    @Override
    public Map<String, Object> asFields() {
        DebugUtils.Assert(namespace != null, "namespace cannot be null");
        DebugUtils.Assert(alarmStatus != null, "alarmStatus cannot be null");
        DebugUtils.Assert(metricName != null, "metricName cannot be null");
        DebugUtils.Assert(readStatus != null, "readStatus cannot be null");
        Map<String, Object> fields = new HashMap<>();
        fields.put("alarmName", alarmName);
        fields.put("threshold", threshold);
        fields.put("period", period);
        fields.put("labels", labels);
        fields.put(FIELD_DATAUUID, dataUuid);
        fields.put("metricValue", metricValue);
        fields.put("comparisonOperator", comparisonOperator);
        fields.put(FIELD_CONTEXT, context);
        fields.put(FIELD_NAMESPACE, namespace);
        fields.put(FIELD_ALARM_STATUS, alarmStatus);
        fields.put(FIELD_RESOURCE_TYPE, resourceType);
        fields.put(FIELD_RESOURCE_UUID, resourceUuid);
        fields.put(FIELD_METRIC_NAME, metricName);
        fields.put(FIELD_READ_STATUS, readStatus);
        return fields;
    }

    @Override
    public Map<String, String> asTags() {
        DebugUtils.Assert(accountUuid != null, "accountUuid cannot be null");
        DebugUtils.Assert(alarmUuid != null, "alarmUuid cannot be null");
        Map<String, String> tags = new HashMap<>();
        tags.put(TAG_ACCOUNT_UUID, accountUuid);
        tags.put(TAG_ALARM_UUID, alarmUuid);
        if (emergencyLevel != null) {
            tags.put(TAG_EMERGENCY_LEVEL, emergencyLevel);
        }


        return tags;
    }

    private static Set<String> tagsFromV1 = new HashSet<>();
    static {
        tagsFromV1.add(FIELD_RESOURCE_UUID);
        tagsFromV1.add(FIELD_RESOURCE_TYPE);
        tagsFromV1.add(FIELD_METRIC_NAME);
        tagsFromV1.add(FIELD_NAMESPACE);
        tagsFromV1.add(FIELD_ALARM_STATUS);
        tagsFromV1.add(FIELD_DATAUUID);
    }
    public static Set<String> tagToFieldFromV1() {
        return tagsFromV1;
    }
}
