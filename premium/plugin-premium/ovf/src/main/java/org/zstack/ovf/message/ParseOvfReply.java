package org.zstack.ovf.message;

import org.zstack.header.message.MessageReply;
import org.zstack.ovf.datatype.OvfInfo;

/**
 * Created by Qi Le on 2022/3/7
 */
public class ParseOvfReply extends MessageReply {
    private OvfInfo ovfInfo;

    public OvfInfo getOvfInfo() {
        return ovfInfo;
    }

    public void setOvfInfo(OvfInfo ovfInfo) {
        this.ovfInfo = ovfInfo;
    }
}
