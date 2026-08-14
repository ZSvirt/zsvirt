package org.zstack.nas;

import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIParam;

/**
 * Created by mingjian.deng on 2018/3/8.
 */
public abstract class APICreateNasMountTargetMsg extends APICreateMessage {
    @APIParam(resourceType = NasFileSystemVO.class)
    private String nasFSUuid;
    @APIParam(maxLength = 255, emptyString = false)
    private String name;
    @APIParam(maxLength = 1024, required = false)
    private String description;

    public abstract String getType();

    public String getNasFSUuid() {
        return nasFSUuid;
    }

    public void setNasFSUuid(String nasFSUuid) {
        this.nasFSUuid = nasFSUuid;
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
