package org.zstack.core.debug;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

import java.util.Arrays;
import java.util.List;

@RestResponse(allTo = "signals")
public class APIGetDebugSignalReply extends APIReply {
    private List<String> signals;

    public List<String> getSignals() {
        return signals;
    }

    public void setSignals(List<String> signals) {
        this.signals = signals;
    }

    public static APIGetDebugSignalReply __example__() {
        APIGetDebugSignalReply reply = new APIGetDebugSignalReply();
        reply.setSignals(Arrays.asList("DumpTaskQueue"));

        return reply;
    }
}
