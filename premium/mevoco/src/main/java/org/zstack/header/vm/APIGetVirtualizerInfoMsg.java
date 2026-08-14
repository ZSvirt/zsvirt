package org.zstack.header.vm;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestRequest(
        path = "/vm-instances/virtualizer-info",
        method = HttpMethod.GET,
        responseClass = APIGetVirtualizerInfoReply.class
)
public class APIGetVirtualizerInfoMsg extends APISyncCallMessage {
    @APIParam(nonempty = true)
    private List<String> uuids;

    public List<String> getUuids() {
        return uuids;
    }

    public void setUuids(List<String> uuids) {
        this.uuids = uuids;
    }

    public static APIGetVirtualizerInfoMsg __example__() {
        APIGetVirtualizerInfoMsg msg = new APIGetVirtualizerInfoMsg();
        msg.setUuids(Arrays.stream(new String[3]).map(v -> uuid()).collect(Collectors.toList()));
        return msg;
    }
}
