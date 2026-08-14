package org.zstack.zwatch.alarm.sns;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang.StringUtils;
import org.zstack.core.Platform;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.cluster.ClusterVO_;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.HostVO_;
import org.zstack.header.storage.backup.BackupStorageVO;
import org.zstack.header.vm.*;
import org.zstack.header.vo.ResourceVO;
import org.zstack.header.vo.ResourceVO_;
import org.zstack.storage.backup.imagestore.ImageStoreBackupStorageVO;
import org.zstack.storage.backup.imagestore.ImageStoreBackupStorageVO_;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.IPv6Constants;
import org.zstack.zwatch.alarm.*;
import org.zstack.zwatch.datatype.EmergencyLevel;
import org.zstack.zwatch.datatype.Namespace;
import org.zstack.zwatch.datatype.UnitCovertRes;
import org.zstack.zwatch.datatype.ZWatchI18n;
import org.zstack.zwatch.datatype.metric.Metric;
import org.zstack.zwatch.utils.ParserUtils;

import java.lang.reflect.Field;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

public abstract class AbstractTextTemplate {
    private final static CLogger logger = Utils.getLogger(AbstractTextTemplate.class);

    public static final String DEFAULT_NULL_CONTENT = "-";

    public static final String PARAM_ALARM_NAME = "ALARM_NAME";
    public static final String PARAM_ALARM_UUID = "ALARM_UUID";
    public static final String PARAM_ALARM_COMPARISON_OPERATOR = "ALARM_COMPARISON_OPERATOR";
    public static final String PARAM_ALARM_COMPARISON_OPERATOR_REVERSE = "ALARM_COMPARISON_OPERATOR_REVERSE";
    public static final String PARAM_ALARM_METRIC = "ALARM_METRIC";
    public static final String PARAM_ALARM_NAMESPACE = "ALARM_NAMESPACE";
    public static final String PARAM_ALARM_THRESHOLD = "ALARM_THRESHOLD";
    public static final String PARAM_ALARM_LABELS = "ALARM_LABELS";
    public static final String PARAM_ALARM_DURATION = "ALARM_DURATION";
    public static final String PARAM_ALARM_PREVIOUS_STATUS = "ALARM_PREVIOUS_STATUS";
    public static final String PARAM_ALARM_CURRENT_STATUS = "ALARM_CURRENT_STATUS";
    public static final String PARAM_ALARM_CURRENT_VALUE = "ALARM_CURRENT_VALUE";
    public static final String PARAM_ALARM_TIME = "ALARM_TIME";
    public static final String PARAM_ALARM_RESOURCE_TYPE = "ALARM_RESOURCE_TYPE";
    public static final String PARAM_ALARM_RESOURCE_ID = "ALARM_RESOURCE_ID";
    public static final String PARAM_ALARM_RESOURCE_NAME = "ALARM_RESOURCE_NAME";
    public static final String PARAM_TITLE_ALARM_RESOURCE_NAME = "TITLE_ALARM_RESOURCE_NAME";
    public static final String PARAM_ALARM_ACCOUNT_UUID = "ALARM_ACCOUNT_UUID";
    public static final String PARAM_ALARM_DATA_UUID = "ALARM_DATA_UUID";
    public static final String PARAM_ALARM_EMERGENCY_LEVEL = "ALARM_EMERGENCY_LEVEL";
    public static final String PARAM_ALARM_RESOURCE_IP = "ALARM_RESOURCE_IP";
    public static final String PARAM_ALARM_RESOURCE_CLUSTER_UUID = "ALARM_RESOURCE_CLUSTER_UUID";
    public static final String PARAM_ALARM_RESOURCE_CLUSTER_NAME = "ALARM_RESOURCE_CLUSTER_NAME";

    public static final String PARAM_EVENT_NAMESPACE = "EVENT_NAMESPACE";
    public static final String PARAM_EVENT_NAME = "EVENT_NAME";
    public static final String PARAM_EVENT_LABELS = "EVENT_LABELS";
    public static final String PARAM_EVENT_EMERGENCY_LEVEL = "EVENT_EMERGENCY_LEVEL";
    public static final String PARAM_EVENT_RESOURCE_ID = "EVENT_RESOURCE_ID";
    public static final String PARAM_EVENT_RESOURCE_NAME = "EVENT_RESOURCE_NAME";
    public static final String PARAM_EVENT_ERROR = "EVENT_ERROR";
    public static final String PARAM_EVENT_TIME = "EVENT_TIME";
    public static final String PARAM_EVENT_SUBSCRIPTION_UUID = "EVENT_SUBSCRIPTION_UUID";
    public static final String PARAM_EVENT_ACCOUNT_UUID = "EVENT_ACCOUNT_UUID";
    public static final String PARAM_EVENT_DATA_UUID = "EVENT_DATA_UUID";
    public static final String PARAM_EVENT_RESOURCE_IP = "EVENT_RESOURCE_IP";
    public static final String PARAM_EVENT_RESOURCE_CLUSTER_UUID = "EVENT_RESOURCE_CLUSTER_UUID";
    public static final String PARAM_EVENT_RESOURCE_CLUSTER_NAME = "EVENT_RESOURCE_CLUSTER_NAME";

    public static Map<String, Object> defaultAlarmMap = new HashMap<>();
    public static Map<String, Object> defaultEventMap = new HashMap<>();

    public final static Map<SNSTextTemplateType, Map<String, Object>> defaultTemplateMap = ImmutableMap.of(
            SNSTextTemplateType.ALARM, defaultAlarmMap,
            SNSTextTemplateType.EVENT, defaultEventMap);

    public static List<String> defaultSupportedAlarmParams = Arrays.asList(
        PARAM_ALARM_NAME,
        PARAM_ALARM_UUID,
        PARAM_ALARM_COMPARISON_OPERATOR,
        PARAM_ALARM_COMPARISON_OPERATOR_REVERSE,
        PARAM_ALARM_METRIC,
        PARAM_ALARM_NAMESPACE,
        PARAM_ALARM_THRESHOLD,
        PARAM_ALARM_LABELS,
        PARAM_ALARM_DURATION,
        PARAM_ALARM_PREVIOUS_STATUS,
        PARAM_ALARM_CURRENT_STATUS,
        PARAM_ALARM_CURRENT_VALUE,
        PARAM_ALARM_TIME,
        PARAM_ALARM_RESOURCE_TYPE,
        PARAM_ALARM_RESOURCE_ID,
        PARAM_ALARM_RESOURCE_NAME,
        PARAM_TITLE_ALARM_RESOURCE_NAME,
        PARAM_ALARM_ACCOUNT_UUID,
        PARAM_ALARM_DATA_UUID,
        PARAM_ALARM_EMERGENCY_LEVEL,
        PARAM_ALARM_RESOURCE_IP,
        PARAM_ALARM_RESOURCE_CLUSTER_UUID,
        PARAM_ALARM_RESOURCE_CLUSTER_NAME);

    public static List<String> defaultSupportedEventParams = Arrays.asList(
        PARAM_EVENT_NAMESPACE,
        PARAM_EVENT_NAME,
        PARAM_EVENT_LABELS,
        PARAM_EVENT_EMERGENCY_LEVEL,
        PARAM_EVENT_RESOURCE_ID,
        PARAM_EVENT_RESOURCE_NAME,
        PARAM_EVENT_ERROR,
        PARAM_EVENT_TIME,
        PARAM_EVENT_SUBSCRIPTION_UUID,
        PARAM_EVENT_ACCOUNT_UUID,
        PARAM_EVENT_DATA_UUID,
        PARAM_EVENT_RESOURCE_IP,
        PARAM_EVENT_RESOURCE_CLUSTER_UUID,
        PARAM_EVENT_RESOURCE_CLUSTER_NAME);

    public static Map<SNSTextTemplateType, List<String>> defaultSupportedParams = ImmutableMap.of(
        SNSTextTemplateType.ALARM, defaultSupportedAlarmParams,
        SNSTextTemplateType.EVENT, defaultSupportedEventParams);

    static {
        /*for (Field field : AbstractTextTemplate.class.getDeclaredFields()) {
            field.setAccessible(true);
            if (!field.isAccessible() || !String.class.isAssignableFrom(field.getType())) {
                continue;
            }

            try {
                String param = (String) field.get(null);
                defaultSupportedParams.add(param);
            } catch (IllegalAccessException e) {
                logger.debug(String.format("failed to load field %s", field.toString()));
            }
        }*/

        defaultAlarmMap.put(PARAM_ALARM_NAME, "testAlarm");
        defaultAlarmMap.put(PARAM_ALARM_UUID, "4a3cb114b10d41e19545ab693222c134");
        defaultAlarmMap.put(PARAM_ALARM_METRIC, "ConnectedHostCount");
        defaultAlarmMap.put(PARAM_ALARM_NAMESPACE, "ZStack/Host");
        defaultAlarmMap.put(PARAM_ALARM_THRESHOLD, "ConnectedHostCount GreaterThan 0.0");
        defaultAlarmMap.put(PARAM_ALARM_LABELS, "hostUuid=4a3cb114b10d41e19545ab693222c134");
        defaultAlarmMap.put(PARAM_ALARM_DURATION, "10 seconds");
        defaultAlarmMap.put(PARAM_ALARM_COMPARISON_OPERATOR, "ConnectedHostCount GreaterThan 0.0");
        defaultAlarmMap.put(PARAM_ALARM_PREVIOUS_STATUS, "nomal");
        defaultAlarmMap.put(PARAM_ALARM_CURRENT_STATUS, "alarm");
        defaultAlarmMap.put(PARAM_ALARM_EMERGENCY_LEVEL, "Important");

        defaultAlarmMap.put(PARAM_ALARM_CURRENT_VALUE, "5.0");
        defaultAlarmMap.put(PARAM_ALARM_ACCOUNT_UUID, "4a3cb114b10d41e19545ab693222c134");
        defaultAlarmMap.put(PARAM_ALARM_DATA_UUID, "4a3cb114b10d41e19545ab693222c134");
        defaultAlarmMap.put(PARAM_ALARM_TIME, System.currentTimeMillis());
        // hack a default value to confirm value can be parsed in groovy template
        defaultAlarmMap.put(PARAM_ALARM_RESOURCE_ID, "4a3cb114b10d41e19545ab693222c134");
        defaultAlarmMap.put(PARAM_TITLE_ALARM_RESOURCE_NAME, "hostTest");
        defaultAlarmMap.put(PARAM_ALARM_RESOURCE_TYPE, "ZStack/Host");
        defaultAlarmMap.put(PARAM_ALARM_RESOURCE_NAME, "test");


        defaultEventMap.put(PARAM_EVENT_NAMESPACE, "ZStack/Host");
        defaultEventMap.put(PARAM_EVENT_NAME, "HostDisconnected");
        defaultEventMap.put(PARAM_EVENT_LABELS, "[Error:unable to connect to KVM[ip:172.20.196.157, username:root3, sshPort:22] to check the management node connectivity,please check if username/password is wrong; null]");
        defaultEventMap.put(PARAM_EVENT_RESOURCE_ID, "5d52a0165531490c8b2b1383f54df5bf");
        defaultEventMap.put(PARAM_EVENT_RESOURCE_NAME, "Host-1-g");
        defaultEventMap.put(PARAM_EVENT_ERROR, null);
        defaultEventMap.put(PARAM_EVENT_TIME, "2020-07-21 14:54:50");
        defaultEventMap.put(PARAM_EVENT_SUBSCRIPTION_UUID, "4a3cb114b10d41e19545ab693222c134");
        defaultEventMap.put(PARAM_EVENT_ACCOUNT_UUID, "4a3cb114b10d41e19545ab693222c134");
        defaultEventMap.put(PARAM_EVENT_DATA_UUID, "4a3cb114b10d41e19545ab693222c134");
    }

    protected Map<String, Object> makeTemplateBindings(AlarmAction.TakeAlarmActionParam param, boolean doi18nTranslate) {
        Map<String, Object> bindings = new HashMap<>();
        AlarmInventory alarmInventory = param.alarm;
        bindings.put(PARAM_ALARM_NAME, param.alarm.getName());
        bindings.put(PARAM_ALARM_UUID, param.alarm.getUuid());
        bindings.put(PARAM_ALARM_NAMESPACE, param.alarm.getNamespace());
        bindings.put(PARAM_ALARM_THRESHOLD, param.alarm.getThreshold());
        bindings.put(PARAM_ALARM_LABELS, param.alarm.getLabels().stream().map(l->String.format("%s %s %s", l.getKey(), l.getOperator(), l.getValue())).collect(Collectors.toList()));
        bindings.put(PARAM_ALARM_DURATION, param.alarm.getPeriod());

        if (doi18nTranslate) {
            bindings.put(PARAM_ALARM_COMPARISON_OPERATOR, param.alarm.getComparisonOperator().toi18nString());
            bindings.put(PARAM_ALARM_COMPARISON_OPERATOR_REVERSE, param.alarm.getComparisonOperator().getReverseOperator().toi18nString());
            bindings.put(PARAM_ALARM_PREVIOUS_STATUS, param.previousStatus.toi18nString());
            bindings.put(PARAM_ALARM_CURRENT_STATUS, param.currentStatus.toi18nString());
            bindings.put(PARAM_ALARM_METRIC, ZWatchI18n.generateDescriptionFromName(param.alarm.getNamespace(), param.alarm.getMetricName()));
            if (alarmInventory.getEmergencyLevel() != null) {
                EmergencyLevel emergencyLevel = EmergencyLevel.valueOf(alarmInventory.getEmergencyLevel());
                bindings.put(PARAM_ALARM_EMERGENCY_LEVEL, emergencyLevel.toi18nString());
            } else {
                bindings.put(PARAM_ALARM_EMERGENCY_LEVEL, DEFAULT_NULL_CONTENT);
            }
        } else {
            bindings.put(PARAM_ALARM_COMPARISON_OPERATOR, param.alarm.getComparisonOperator().toString());
            bindings.put(PARAM_ALARM_COMPARISON_OPERATOR_REVERSE,
                    param.alarm.getComparisonOperator().getReverseOperator().toString()
            );
            bindings.put(PARAM_ALARM_METRIC, param.alarm.getMetricName());
            bindings.put(PARAM_ALARM_PREVIOUS_STATUS, param.previousStatus.toString());
            bindings.put(PARAM_ALARM_CURRENT_STATUS, param.currentStatus.toString());
            bindings.put(PARAM_ALARM_EMERGENCY_LEVEL,
                    alarmInventory.getEmergencyLevel() != null ? alarmInventory.getEmergencyLevel() : ""
            );
        }

        bindings.put(PARAM_ALARM_ACCOUNT_UUID, param.alarmAccountUuid);
        bindings.put(PARAM_ALARM_DATA_UUID, param.alarmDataUuid);
        bindings.put(PARAM_ALARM_TIME, param.timeMillis);
        // hack a default value to confirm value can be parsed in groovy template
        bindings.put(PARAM_ALARM_RESOURCE_ID, DEFAULT_NULL_CONTENT);
        bindings.put(PARAM_ALARM_RESOURCE_NAME, DEFAULT_NULL_CONTENT);
        bindings.put(PARAM_TITLE_ALARM_RESOURCE_NAME, DEFAULT_NULL_CONTENT);
        bindings.put(PARAM_ALARM_RESOURCE_IP, DEFAULT_NULL_CONTENT);
        bindings.put(PARAM_ALARM_RESOURCE_CLUSTER_UUID, DEFAULT_NULL_CONTENT);
        bindings.put(PARAM_ALARM_RESOURCE_CLUSTER_NAME, DEFAULT_NULL_CONTENT);

        Namespace ns = Namespace.getMetricNameSpace(alarmInventory.getNamespace(), alarmInventory.getMetricName());
        if (ns != null) {
            if (ns.getResourceType() != null) {
                bindings.put(PARAM_ALARM_RESOURCE_TYPE, ns.getResourceType());
            }

            if (ns.getIdentityLabelName() != null && alarmInventory.getLabels() != null) {
                Optional<AlarmLabelInventory> opt = alarmInventory.getLabels().stream().filter(l -> l.getKey().equals(ns.getIdentityLabelName())).findFirst();
                opt.ifPresent(alarmLabelInventory -> bindings.put(PARAM_ALARM_RESOURCE_ID, alarmLabelInventory.getValue()));
            }

            if (param.identifyLabel != null) {
                Map<String, String> identifyLabel = ParserUtils.parseIdentifyLabel(param.identifyLabel);

                if (identifyLabel.get(ns.getIdentityLabelName()) != null) {
                    bindings.put(PARAM_ALARM_RESOURCE_ID, identifyLabel.get(ns.getIdentityLabelName()));
                }
            }

            String resourceUuid = (String) bindings.get(PARAM_ALARM_RESOURCE_ID);
            if (StringUtils.isNotEmpty(resourceUuid) && !resourceUuid.equals(DEFAULT_NULL_CONTENT)) {
                String resourceName = Q.New(ResourceVO.class)
                        .select(ResourceVO_.resourceName)
                        .eq(ResourceVO_.uuid, resourceUuid)
                        .findValue();

                if (StringUtils.isNotEmpty(resourceName)) {
                    bindings.put(PARAM_ALARM_RESOURCE_NAME, resourceName);
                    bindings.put(PARAM_TITLE_ALARM_RESOURCE_NAME,
                            Platform.getLocale().equals(Locale.SIMPLIFIED_CHINESE) ?
                                    String.format("中资源 %s", resourceName) :
                                    String.format("Resource %s", resourceName)
                    );
                } else {
                    String value = ns.getResourceNameIfNull(param);
                    if (StringUtils.isNotEmpty(value)) {
                        bindings.put(PARAM_ALARM_RESOURCE_NAME, value);
                    }
                }

                String resourceIP = getResourceIP(resourceUuid, ns.getResourceType());
                if (StringUtils.isNotBlank(resourceIP)) bindings.put(PARAM_ALARM_RESOURCE_IP, resourceIP);

                String clusterUuid = getClusterUuid(resourceUuid, ns.getResourceType());
                if (StringUtils.isNotBlank(clusterUuid)) bindings.put(PARAM_ALARM_RESOURCE_CLUSTER_UUID, clusterUuid);

                String clusterName = getClusterName(clusterUuid);
                if (StringUtils.isNotBlank(resourceName)) bindings.put(PARAM_ALARM_RESOURCE_CLUSTER_NAME, clusterName);

                bindings.put(PARAM_ALARM_NAMESPACE, param.getNamespace(resourceUuid));
                if (doi18nTranslate) {
                    bindings.put(PARAM_ALARM_METRIC, param.getI18nMetric(resourceUuid,
                            ZWatchI18n.generateDescriptionFromName(param.alarm.getNamespace(), param.alarm.getMetricName())));
                }
            }

            Optional<Metric> metric = ns.getMetrics().stream()
                    .filter(m -> param.alarm.getMetricName().equals(m.getName()))
                    .findAny();

            if (!metric.isPresent()) {
                bindings.put(PARAM_ALARM_THRESHOLD, getUnitTransStr(param.alarm.getThreshold(), null));
                bindings.put(PARAM_ALARM_CURRENT_VALUE, getUnitTransStr(param.currentValue, null));
            } else {
                Metric m = metric.get();
                UnitCovertRes tRes = m.convertUnit(param.alarm.getThreshold());
                UnitCovertRes vRes = m.convertUnit(param.currentValue);

                bindings.put(PARAM_ALARM_THRESHOLD, getUnitTransStr(tRes.getValue(), tRes.getUnit()));
                bindings.put(PARAM_ALARM_CURRENT_VALUE, getUnitTransStr(vRes.getValue(), vRes.getUnit()));
            }
        }

        return bindings;
    }

    String getUnitTransStr(double value, String unit) {
        String res = String.format("%.2f", value);
        if (unit != null) {
            res = res + unit;
        }
        return res;
    }

    protected Map<String, Object> makeTemplateBindings(AlarmAction.TakeAlarmActionParam param) {
        return makeTemplateBindings(param, true);
    }

    protected Map<String, Object> makeTemplateBindings(AlarmAction.TakeEventSubscriptionActionParam param) {
        return makeTemplateBindings(param, true);
    }

    protected Map<String, Object> makeTemplateBindings(AlarmAction.TakeEventSubscriptionActionParam param, boolean doi18nTranslate) {
        EmergencyLevel emergencyLevel = Q.New(EventSubscriptionVO.class)
                .eq(EventSubscriptionVO_.uuid, param.subscriptionUuid)
                .select(EventSubscriptionVO_.emergencyLevel)
                .findValue();

        Map<String, Object> bindings = new HashMap<>();
        bindings.put(PARAM_EVENT_NAMESPACE, param.event.getNamespace());
        bindings.put(PARAM_EVENT_NAME, param.event.getName());
        bindings.put(PARAM_EVENT_LABELS, param.event.getLabels());

        if (doi18nTranslate) {
            String emergencyLevelValue = emergencyLevel != null ? emergencyLevel.toi18nString() : param.event.getEmergencyLevel().toString();
            bindings.put(PARAM_EVENT_EMERGENCY_LEVEL, emergencyLevelValue);
        } else {
            String emergencyLevelValue = emergencyLevel != null ? emergencyLevel.name() : param.event.getEmergencyLevel().toString();
            bindings.put(PARAM_EVENT_EMERGENCY_LEVEL, emergencyLevelValue);
        }

        bindings.put(PARAM_EVENT_RESOURCE_ID, param.event.getResourceId());
        bindings.put(PARAM_EVENT_RESOURCE_NAME, param.event.getResourceName());
        bindings.put(PARAM_EVENT_ERROR, param.event.getError());
        bindings.put(PARAM_EVENT_TIME, new Timestamp(param.event.getTime()));
        bindings.put(PARAM_EVENT_SUBSCRIPTION_UUID, param.subscriptionUuid);
        bindings.put(PARAM_EVENT_ACCOUNT_UUID, param.subscriptionAccountUuid);
        bindings.put(PARAM_EVENT_DATA_UUID, param.dataUuid);
        bindings.put(PARAM_EVENT_RESOURCE_IP, DEFAULT_NULL_CONTENT);
        bindings.put(PARAM_EVENT_RESOURCE_CLUSTER_UUID, DEFAULT_NULL_CONTENT);
        bindings.put(PARAM_EVENT_RESOURCE_CLUSTER_NAME, DEFAULT_NULL_CONTENT);

        String resourceUuid = param.event.getResourceId();
        Namespace ns = Namespace.getEventNameSpace(param.event.getNamespace(), param.event.getName());
        if (StringUtils.isNotBlank(resourceUuid) && ns != null) {
            String resourceIP = getResourceIP(resourceUuid, ns.getResourceType());
            if (StringUtils.isNotBlank(resourceIP)) bindings.put(PARAM_EVENT_RESOURCE_IP, resourceIP);

            String clusterUuid = getClusterUuid(resourceUuid, ns.getResourceType());
            if (StringUtils.isNotBlank(clusterUuid)) bindings.put(PARAM_EVENT_RESOURCE_CLUSTER_UUID, clusterUuid);

            String clusterName = getClusterName(clusterUuid);
            if (StringUtils.isNotBlank(param.event.getResourceName())) bindings.put(PARAM_EVENT_RESOURCE_CLUSTER_NAME, clusterName);

            bindings.put(PARAM_EVENT_NAMESPACE, param.getNamespace(resourceUuid));
        }
        return bindings;
    }

    private String getResourceIP(String resourceUuid, String resourceType) {
        if (resourceType.equals(VmInstanceVO.class.getSimpleName())) {
            VmInstanceVO vmInstanceVO = Q.New(VmInstanceVO.class)
                    .eq(VmInstanceVO_.uuid, resourceUuid)
                    .find();
            if (vmInstanceVO == null) return "";

            VmNicVO defaultVmNic = Q.New(VmNicVO.class)
                    .eq(VmNicVO_.l3NetworkUuid, vmInstanceVO.getDefaultL3NetworkUuid())
                    .eq(VmNicVO_.vmInstanceUuid, resourceUuid)
                    .eq(VmNicVO_.ipVersion, IPv6Constants.IPv4)
                    .find();

            if (defaultVmNic != null) return defaultVmNic.getIp();
        } else if (resourceType.equals(HostVO.class.getSimpleName())) {
            HostVO hostVO = Q.New(HostVO.class)
                    .eq(HostVO_.uuid, resourceUuid)
                    .find();
            if (hostVO != null) return hostVO.getManagementIp();
        } else if (resourceType.equals(BackupStorageVO.class.getSimpleName())) {
            ImageStoreBackupStorageVO imageStoreBackupStorageVO = Q.New(ImageStoreBackupStorageVO.class)
                    .eq(ImageStoreBackupStorageVO_.uuid, resourceUuid)
                    .find();
            if (imageStoreBackupStorageVO != null) return imageStoreBackupStorageVO.getHostname();
        }
        return "";
    }

    private String getClusterUuid(String resourceUuid, String resourceType) {
        if (StringUtils.isBlank(resourceUuid) || StringUtils.isBlank(resourceType)) return "";

        String concreteResourceType = Q.New(ResourceVO.class)
                .select(ResourceVO_.concreteResourceType)
                .eq(ResourceVO_.uuid, resourceUuid)
                .findValue();
        if (StringUtils.isBlank(concreteResourceType)) return "";

        Class clazz;
        try {
            clazz = Class.forName(concreteResourceType);
        } catch (ClassNotFoundException e) {
            logger.error(String.format("getClusterName clazz not found %s", concreteResourceType));
            return "";
        }

        List<Field> fields = new ArrayList<>();
        getAllFields(clazz, fields);
        boolean existsClusterUuidFieldFlag = fields.stream().anyMatch(f->f.getName().equals("clusterUuid"));
        if (!existsClusterUuidFieldFlag) return "";

        String sql = String.format("SELECT clusterUuid FROM %s WHERE uuid = '%s'", resourceType, resourceUuid);
        return SQL.New(sql).find();
    }

    private String getClusterName(String clusterUuid) {
        if (StringUtils.isBlank(clusterUuid)) return "";

        return Q.New(ClusterVO.class)
                .select(ClusterVO_.name)
                .eq(ClusterVO_.uuid, clusterUuid)
                .findValue();
    }

    private void getAllFields(Class clazz, List<Field> fields) {
        if (clazz == null) return;
        fields.addAll(Arrays.asList(clazz.getDeclaredFields()));
        if (clazz.getSuperclass() != null) getAllFields(clazz.getSuperclass(), fields);
    }
}
