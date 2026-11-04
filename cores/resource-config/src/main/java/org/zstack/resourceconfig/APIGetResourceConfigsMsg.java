package org.zstack.resourceconfig;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vo.ResourceVO;

import java.util.Arrays;
import java.util.List;

@RestRequest(path = "/resource-configurations/{resourceUuid}/{category}",
        method = HttpMethod.GET, responseClass = APIGetResourceConfigsReply.class)
public class APIGetResourceConfigsMsg  extends APISyncCallMessage implements ResourceConfigMessage {
    @APIParam
    private String category;
    @APIParam(nonempty = true)
    private List<String> names;
    @APIParam(resourceType = ResourceVO.class)
    private String resourceUuid;

    @Override
    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public List<String> getNames() {
        return names;
    }

    public void setNames(List<String> names) {
        this.names = names;
    }

    public String getResourceUuid() {
        return resourceUuid;
    }

    public void setResourceUuid(String resourceUuid) {
        this.resourceUuid = resourceUuid;
    }

    public static APIGetResourceConfigsMsg __example__() {
        APIGetResourceConfigsMsg msg = new APIGetResourceConfigsMsg();
        msg.category = "host";
        msg.names = Arrays.asList("cpu.overProvisioning.ratio", "reconnectAllOnBoot");
        msg.resourceUuid = uuid(ResourceVO.class);
        return msg;
    }
}
