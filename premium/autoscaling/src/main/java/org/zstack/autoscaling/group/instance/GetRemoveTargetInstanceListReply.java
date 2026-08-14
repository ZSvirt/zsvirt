package org.zstack.autoscaling.group.instance;

import org.zstack.header.message.MessageReply;
import java.util.List;

/**
 * Created by lining on 2018/10/17.
 */
public class GetRemoveTargetInstanceListReply extends MessageReply {
    private List<String> result;

    public List<String> getResult() {
        return result;
    }

    public void setResult(List<String> result) {
        this.result = result;
    }
}
