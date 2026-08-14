package org.zstack.monitoring.actions;

import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.monitoring.MonitorTriggerVO;

import java.util.List;

@Deprecated
public abstract class APICreateMonitorTriggerActionMsg extends APICreateMessage {
    @APIParam(maxLength = 255)
    private String name;
    @APIParam(maxLength = 2048, required = false)
    private String description;
    @APIParam(resourceType = MonitorTriggerVO.class, nonempty = true, required = false)
    private List<String> triggerUuids;

    public abstract String getType();

    public List<String> getTriggerUuids() {
        return triggerUuids;
    }

    public void setTriggerUuids(List<String> triggerUuids) {
        this.triggerUuids = triggerUuids;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
