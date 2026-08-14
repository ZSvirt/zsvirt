package org.zstack.header.configuration;

import org.zstack.header.message.DeletionMessage;

/**
 */
public class InstanceOfferingDeletionMsg extends DeletionMessage implements InstanceOfferingMessage {
    private String instanceOfferingUuid;

    public String getInstanceOfferingUuid() {
        return instanceOfferingUuid;
    }

    public void setInstanceOfferingUuid(String instanceOfferingUuid) {
        this.instanceOfferingUuid = instanceOfferingUuid;
    }
}
