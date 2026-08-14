package org.zstack.log;

import org.zstack.header.message.NeedReplyMessage;

public class ApplyLogConfigurationMsg extends NeedReplyMessage {
    private LogConfigurationStruct struct;
    private String labelKey;

    public LogConfigurationStruct getStruct() {
        return struct;
    }

    public void setStruct(LogConfigurationStruct struct) {
        this.struct = struct;
    }

    public String getLabelKey() {
        return labelKey;
    }

    public void setLabelKey(String labelKey) {
        this.labelKey = labelKey;
    }
}
