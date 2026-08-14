package org.zstack.header.host;

import org.zstack.header.message.MessageReply;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by mingjian.deng on 2019/11/25.
 */
public class ScanVmPortReply extends MessageReply {
    Map<String, String> status = new HashMap<>();
    private boolean supportScan;

    public Map<String, String> getStatus() {
        return status;
    }

    public void setStatus(Map<String, String> status) {
        this.status = status;
    }

    public boolean isSupportScan() {
        return supportScan;
    }

    public void setSupportScan(boolean supportScan) {
        this.supportScan = supportScan;
    }
}
