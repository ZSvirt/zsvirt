package org.zstack.nas;

import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIParam;

/**
 * Created by mingjian.deng on 2018/3/5.
 */
public abstract class APICreateNasFileSystemMsg extends APICreateMessage {
    @APIParam(required = false, validValues = {"NFS", "SMB"})
    private String protocol;
    @APIParam(maxLength = 255, emptyString = false)
    private String name;
    @APIParam(maxLength = 1024, required = false)
    private String description;

    public abstract String getType();

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
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
