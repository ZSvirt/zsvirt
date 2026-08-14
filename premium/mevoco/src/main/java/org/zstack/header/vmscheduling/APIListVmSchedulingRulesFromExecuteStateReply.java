package org.zstack.header.vmscheduling;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;
import java.util.Map;

@RestResponse(allTo = "uuids")
public class APIListVmSchedulingRulesFromExecuteStateReply extends APIReply {
    List<String> uuids;

    public List<String> getUuids() {
        return uuids;
    }

    public void setUuids(List<String> uuids) {
        this.uuids = uuids;
    }
}
