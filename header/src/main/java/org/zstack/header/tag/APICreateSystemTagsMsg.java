package org.zstack.header.tag;
import org.springframework.http.HttpMethod;
import org.zstack.header.log.NoLogging;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vm.metadata.MetadataImpact;
import org.zstack.header.vo.ResourceVO;

import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

@RestRequest(
        path = "/system-tags/{resourceUuid}/tags",
        method = HttpMethod.POST,
        responseClass = APICreateSystemTagsEvent.class,
        parameterName = "params"
)
@MetadataImpact(value = MetadataImpact.Impact.CONFIG, resolver = "ResourceUuidToVmUuidResolver", field = "resourceUuid")
public class APICreateSystemTagsMsg extends APIMessage {
    @APIParam
    private String resourceType;
    @APIParam(resourceType = ResourceVO.class, scope = APIParam.SCOPE_ALLOWED_SHARING)
    private String resourceUuid;
    @APIParam(nonempty = true)
    @NoLogging(type = NoLogging.Type.Tag)
    private List<String> tags;

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getResourceUuid() {
        return resourceUuid;
    }

    public void setResourceUuid(String resourceUuid) {
        this.resourceUuid = resourceUuid;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public static APICreateSystemTagsMsg __example__() {
        APICreateSystemTagsMsg msg = new APICreateSystemTagsMsg();
        msg.setResourceUuid(uuid(ResourceVO.class));
        msg.setResourceType("VmInstanceVO");
        msg.setTags(list("tag1", "tag2"));
        return msg;
    }
}
