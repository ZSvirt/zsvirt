package org.zstack.directory;

import org.zstack.header.message.NeedReplyMessage;

/**
 * @author shenjin
 * @date 2022/12/1 14:20
 */
public class CreateDirectoryMsg extends NeedReplyMessage {
    private String uuid;

    private String name;

    private String parentUuid;

    private String type;

    private String zoneUuid;

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

    public String getParentUuid() {
        return parentUuid;
    }

    public void setParentUuid(String parentUuid) {
        this.parentUuid = parentUuid;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getZoneUuid() {
        return zoneUuid;
    }

    public void setZoneUuid(String zoneUuid) {
        this.zoneUuid = zoneUuid;
    }
}
