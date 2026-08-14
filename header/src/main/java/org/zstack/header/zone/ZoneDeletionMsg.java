package org.zstack.header.zone;

import org.zstack.header.message.DeletionMessage;

/**
 */
public class ZoneDeletionMsg extends DeletionMessage implements ZoneMessage {
    private String zoneUuid;

    @Override
    public String getZoneUuid() {
        return zoneUuid;
    }

    public void setZoneUuid(String zoneUuid) {
        this.zoneUuid = zoneUuid;
    }
}
