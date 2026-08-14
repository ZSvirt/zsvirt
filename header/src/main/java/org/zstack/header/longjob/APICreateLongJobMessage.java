package org.zstack.header.longjob;

import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIParam;

/**
 * Created by GuoYi on 2018-09-26.
 */
public abstract class APICreateLongJobMessage extends APICreateMessage {
    @APIParam(required = false, maxLength = 255)
    private String longJobName;
    @APIParam(required = false, maxLength = 2048)
    private String longJobDescription;

    public String getLongJobName() {
        return longJobName;
    }

    public void setLongJobName(String longJobName) {
        this.longJobName = longJobName;
    }

    public String getLongJobDescription() {
        return longJobDescription;
    }

    public void setLongJobDescription(String longJobDescription) {
        this.longJobDescription = longJobDescription;
    }
}
