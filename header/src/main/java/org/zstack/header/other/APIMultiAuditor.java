package org.zstack.header.other;

import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;

import java.util.List;

public interface APIMultiAuditor {
    List<APIAuditor.Result> multiAudit(APIMessage msg, APIEvent rsp);
}
