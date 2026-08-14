package org.zstack.resourceconfig;

import org.springframework.http.HttpMethod;
import org.zstack.core.Platform;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vm.metadata.MetadataImpact;
import org.zstack.header.vo.ResourceVO;

@RestRequest(path = "/resource-configurations/{category}/{name}/{resourceUuid}",
        method = HttpMethod.DELETE,
        responseClass = APIDeleteResourceConfigEvent.class)
@MetadataImpact(value = MetadataImpact.Impact.CONFIG, resolver = "ResourceUuidToVmUuidResolver", field = "resourceUuid")
public class APIDeleteResourceConfigMsg extends APIDeleteMessage implements ResourceConfigMessage {
    @APIParam
    private String category;
    @APIParam
    private String name;
    @APIParam(successIfResourceNotExisting = true, resourceType = ResourceVO.class)
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
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getResourceUuid() {
        return resourceUuid;
    }

    public void setResourceUuid(String resourceUuid) {
        this.resourceUuid = resourceUuid;
    }

    public static APIDeleteResourceConfigMsg __example__() {
        APIDeleteResourceConfigMsg msg = new APIDeleteResourceConfigMsg();
        msg.category = "host";
        msg.name = "cpu.overProvisioning.ratio";
        msg.resourceUuid = uuid(ResourceVO.class);
        return msg;
    }
}
