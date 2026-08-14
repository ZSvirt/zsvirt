package org.zstack.header.host;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

@RestRequest(
        path = "/hosts/{uuid}/numa",
        responseClass = APIGetHostNUMATopologyEvent.class,
        method = HttpMethod.POST,
        parameterName = "params"
)
public class APIGetHostNUMATopologyMsg extends APIMessage implements HostMessage {
    @APIParam(resourceType = HostVO.class)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public static List<String> __example__() {
        return asList("uuid="+uuid());
    }

    @Override
    public String getHostUuid() {
        return getUuid();
    }
}
