package org.zstack.header.host;

import org.zstack.header.message.MessageReply;

/**
 * Created by LiangHanYu on 2021/8/13 16:32
 */
public class CompareCpuFunctionOnHostReply extends MessageReply {
    private boolean match;
    private String compareError;

    public boolean isMatch() {
        return match;
    }

    public void setMatch(boolean match) {
        this.match = match;
    }

    public String getCompareError() {
        return compareError;
    }

    public void setCompareError(String compareError) {
        this.compareError = compareError;
    }
}
