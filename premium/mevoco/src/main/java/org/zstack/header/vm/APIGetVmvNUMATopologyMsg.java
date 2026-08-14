package org.zstack.header.vm;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

import java.util.List;
import static java.util.Arrays.asList;

@RestRequest(
        path = "/vm-instances/{uuid}/vnuma-topology",
        responseClass = APIGetVmvNUMATopologyReply.class,
        method = HttpMethod.GET
)
public class APIGetVmvNUMATopologyMsg extends APISyncCallMessage implements VmInstanceMessage {
    @APIParam(resourceType = VmInstanceVO.class)
    private String uuid;

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getUuid() {
        return uuid;
    }

    public static List<String> __example__() {
        return asList("uuid="+uuid());
    }

    @Override
    public String getVmInstanceUuid() {
        return getUuid();
    }
}
