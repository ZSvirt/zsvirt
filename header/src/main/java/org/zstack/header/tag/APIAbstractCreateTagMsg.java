package org.zstack.header.tag;

import org.zstack.header.log.NoLogging;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.vo.ResourceVO;

/**
 */
public abstract class APIAbstractCreateTagMsg extends APIMessage {
    @APIParam
    private String resourceType;
    @APIParam(resourceType = ResourceVO.class, scope = APIParam.SCOPE_MUST_OWNER)
    private String resourceUuid;
    @APIParam
    @NoLogging(type = NoLogging.Type.Tag)
    private String tag;

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

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

}