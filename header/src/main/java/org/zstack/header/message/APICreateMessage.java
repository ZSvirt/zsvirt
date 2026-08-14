package org.zstack.header.message;

import org.zstack.header.tag.TagPatternVO;

import java.util.ArrayList;
import java.util.List;

public class APICreateMessage extends APIMessage {
    /**
     * @desc resource uuid which must be of version 4(random) with dash stripped. For example,
     * '5d94103e-1925-4d86-96c0-f05489c259ab' is stripped as '5d94103e19254d8696c0f05489c259ab'.
     * When the field is provided, it's used as uuid for resource to be created. An internal error
     * is raised if the uuid conflicted with any existing resource uuid
     */
    private String resourceUuid;

    @APIParam(required = false, resourceType = TagPatternVO.class)
    private List<String> tagUuids;

    public void addSystemTag(String tag) {
        if (systemTags == null) {
            systemTags = new ArrayList<>();
        }
        systemTags.add(tag);
    }

    public String getResourceUuid() {
        return resourceUuid;
    }

    public void setResourceUuid(String resourceUuid) {
        this.resourceUuid = resourceUuid;
    }

    public List<String> getTagUuids() {
        return tagUuids;
    }

    public void setTagUuids(List<String> tagUuids) {
        this.tagUuids = tagUuids;
    }
}
