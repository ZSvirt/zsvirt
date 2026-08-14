package org.zstack.tag2;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.tag.TagPatternVO;
import org.zstack.header.vo.ResourceVO;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestRequest(
        path = "/tags/{tagUuid}/resources",
        method = HttpMethod.POST,
        responseClass = APIAttachTagToResourcesEvent.class,
        parameterName = "params"
)
public class APIAttachTagToResourcesMsg extends APIMessage implements TagPatternMessage {
    @APIParam(resourceType = TagPatternVO.class)
    private String tagUuid;
    @APIParam(nonempty = true, resourceType = ResourceVO.class)
    private List<String> resourceUuids;

    @APIParam(required = false)
    private Map<String, String> tokens;

    @Override
    public String getTagPatternUuid() {
        return tagUuid;
    }

    public String getTagUuid() {
        return tagUuid;
    }

    public void setTagUuid(String tagUuid) {
        this.tagUuid = tagUuid;
    }

    public List<String> getResourceUuids() {
        return resourceUuids;
    }

    public void setResourceUuids(List<String> resourceUuids) {
        this.resourceUuids = resourceUuids;
    }

    public Map<String, String> getTokens() {
        return tokens;
    }

    public void setTokens(Map<String, String> tokens) {
        this.tokens = tokens;
    }

    public static APIAttachTagToResourcesMsg __example__() {
        APIAttachTagToResourcesMsg msg = new APIAttachTagToResourcesMsg();
        msg.tagUuid = uuid(TagPatternVO.class);
        msg.resourceUuids = Collections.singletonList(uuid(ResourceVO.class));
        return msg;
    }
}
