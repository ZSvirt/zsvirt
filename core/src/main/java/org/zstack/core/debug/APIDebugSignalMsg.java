package org.zstack.core.debug;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

import java.util.Arrays;
import java.util.List;

/**
 * Created by xing5 on 2016/7/25.
 */
@RestRequest(
        path = "/debug",
        method = HttpMethod.POST,
        parameterName = "params",
        responseClass = APIDebugSignalEvent.class
)
public class APIDebugSignalMsg extends APIMessage {
    @APIParam
    private List<String> signals;

    public List<String> getSignals() {
        return signals;
    }

    public void setSignals(List<String> signals) {
        this.signals = signals;
    }
 
    public static APIDebugSignalMsg __example__() {
        APIDebugSignalMsg msg = new APIDebugSignalMsg();
        msg.signals = Arrays.asList();

        return msg;
    }

}
