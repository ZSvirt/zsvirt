package org.zstack.zwatch.alarm;


import org.zstack.header.message.NeedReplyMessage;

import java.util.List;

public class UpdateEventSubscriptionMsg  extends NeedReplyMessage implements EventSubscriptionMessage {

    public String uuid;
    public String subscriptionUuid;
    public String name;
    public String emergencyLevel;
    public List<APICreateAlarmMsg.ActionParam> actions;

    @Override
    public String getSubscriptionUuid() {
        return uuid;
    }

    public void setSubscriptionUuid(String subscriptionUuid) {
        this.subscriptionUuid = subscriptionUuid;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmergencyLevel() {
        return emergencyLevel;
    }

    public void setEmergencyLevel(String emergencyLevel) {
        this.emergencyLevel = emergencyLevel;
    }

    public List<APICreateAlarmMsg.ActionParam> getActions() {
        return actions;
    }

    public void setActions(List<APICreateAlarmMsg.ActionParam> actions) {
        this.actions = actions;
    }


}
