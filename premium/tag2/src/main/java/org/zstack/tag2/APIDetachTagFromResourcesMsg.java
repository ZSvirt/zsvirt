package org.zstack.tag2;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.tag.TagPatternVO;
import org.zstack.header.vo.ResourceVO;

import java.util.Collections;
import java.util.List;

@RestRequest(
        path = "/tags/{tagUuid}/resources",
        method = HttpMethod.DELETE,
        responseClass = APIDetachTagFromResourcesEvent.class
)
public class APIDetachTagFromResourcesMsg extends APIMessage implements TagPatternMessage {
    @APIParam(resourceType = TagPatternVO.class)
    private String tagUuid;
    @APIParam(nonempty = true)
    private List<String> resourceUuids;

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

    public static APIDetachTagFromResourcesMsg __example__() {
        APIDetachTagFromResourcesMsg msg = new APIDetachTagFromResourcesMsg();
        msg.tagUuid = uuid(TagPatternVO.class);
        msg.resourceUuids = Collections.singletonList(uuid(ResourceVO.class));
        return msg;
    }
}
