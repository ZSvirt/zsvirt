package org.zstack.zwatch.datatype;

import org.zstack.core.DynamicObject;
import org.zstack.core.Platform;
import org.zstack.core.db.Q;
import org.zstack.header.message.DocUtils;
import org.zstack.header.rest.SDK;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vo.ResourceVO;
import org.zstack.header.vo.ResourceVO_;
import org.zstack.identity.Account;
import org.zstack.utils.DebugUtils;
import org.zstack.zwatch.namespace.VmNamespace;

import java.util.HashMap;
import java.util.Map;

@SDK
public class EventData implements DynamicObject {
    public static final String LABEL_RESOURCE_ID = "ResourceId";
    public static final String LABEL_EMERGENCY_LEVEL = "EmergencyLevel";

    public static final String FIELD_NONE = "__none__";

    protected String namespace;
    protected String name;
    protected Map<String, String> labels = new HashMap<>();
    protected EventFamily.EmergencyLevel emergencyLevel;
    protected String resourceId;
    protected String resourceName;
    protected String error;
    protected long time = System.currentTimeMillis();
    protected String dataUuid;
    protected String accountUuid;
    protected String subscriptionUuid;
    protected String readStatus;

    public static EventData __example__() {
        EventData event = new EventData();
        event.setNamespace("ZStack/VM");
        event.setName(VmNamespace.VMHAStarted.getName());
        event.setEmergencyLevel(EventFamily.EmergencyLevel.Normal.name());
        event.setResourceId(DocUtils.createFixedUuid(VmInstanceVO.class));
        event.setResourceName(VmInstanceVO.class.getSimpleName());
        event.setTime(DocUtils.date);
        return event;
    }

    public EventData(EventData other) {
        this.namespace = other.namespace;
        this.name = other.name;
        this.labels = other.labels;
        this.emergencyLevel = other.emergencyLevel;
        this.resourceId = other.resourceId;
        this.resourceName = other.resourceName;
        this.error = other.error;
        this.time = other.time;
        this.dataUuid = other.dataUuid;
        this.accountUuid = other.accountUuid;
        this.subscriptionUuid = other.subscriptionUuid;
        this.readStatus = other.readStatus;
    }

    public EventData() {
    }

    public Map<String, String> asQueryLabels() {
        Map<String, String> ret = new HashMap<>();
        ret.put(LABEL_RESOURCE_ID, resourceId);
        ret.put(LABEL_EMERGENCY_LEVEL, emergencyLevel.toString());
        ret.putAll(labels);
        return ret;
    }

    public EventData(EventFamily family, EventFamily.Event event) {
        namespace = family.getNamespace();
        name = family.getName();

        if (event.getError() == null) {
            DebugUtils.Assert(family.getLabelNames().size() == event.getLabelValues().size(),
                    String.format("the number of labelNames[%s] not matching the number of label values[%s], names%S, values%s",
                            family.getLabelNames().size(), event.getLabelValues().size(), family.getLabelNames(), event.getLabelValues()));

            for (int i = 0; i < family.getLabelNames().size(); i++) {
                String name = family.getLabelNames().get(i);
                String value = event.getLabelValues().get(i);
                labels.put(name, value);
            }
        } else  {
            error = event.getError();
        }

        emergencyLevel = family.getEmergencyLevel();
        resourceId = event.getResourceId();
        resourceName = Q.New(ResourceVO.class).select(ResourceVO_.resourceName).eq(ResourceVO_.uuid, resourceId).findValue();
        dataUuid = Platform.getUuid();

        if (resourceId != null) {
            accountUuid = Account.getAccountUuidOfResource(resourceId);
        }
    }

    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    public void setDuration(long duration) {
        labels.put(FIELD_NONE, String.valueOf(duration));
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, String> getLabels() {
        return labels;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public void setLabels(Map<String, String> labels) {
        this.labels = labels;
    }

    public EventFamily.EmergencyLevel getEmergencyLevel() {
        return emergencyLevel;
    }

    public void setEmergencyLevel(String emergencyLevel) {
        this.emergencyLevel = EventFamily.EmergencyLevel.valueOf(emergencyLevel);
    }

    public String getDataUuid() {
        return dataUuid;
    }

    public void setDataUuid(String dataUuid) {
        this.dataUuid = dataUuid;
    }

    public String getAccountUuid() {
        return accountUuid;
    }

    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }

    public String getSubscriptionUuid() {
        return subscriptionUuid;
    }

    public void setSubscriptionUuid(String subscriptionUuid) {
        this.subscriptionUuid = subscriptionUuid;
    }

    public String getReadStatus() {
        return readStatus;
    }

    public void setReadStatus(String readStatus) {
        this.readStatus = readStatus;
    }
}
