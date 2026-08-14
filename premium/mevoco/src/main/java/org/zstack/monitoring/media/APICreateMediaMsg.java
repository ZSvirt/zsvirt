package org.zstack.monitoring.media;

import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIParam;

/**
 * Created by xing5 on 2017/6/11.
 */
@Deprecated
public abstract class APICreateMediaMsg extends APICreateMessage {
    @APIParam(maxLength = 255)
    private String name;
    @APIParam(maxLength = 2048, required = false)
    private String description;

    public abstract String getType();

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
