package org.zstack.drs.message;

import org.zstack.drs.DRSMessage;
import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by lining on 2019/12/13.
 */
public class ExecuteDRSSchedulingMsg extends NeedReplyMessage implements DRSMessage {
    private String drsUuid;

    public String getDrsUuid() {
        return drsUuid;
    }

    public void setDrsUuid(String drsUuid) {
        this.drsUuid = drsUuid;
    }

    @Override
    public String getDRSUuid() {
        return drsUuid;
    }
}
