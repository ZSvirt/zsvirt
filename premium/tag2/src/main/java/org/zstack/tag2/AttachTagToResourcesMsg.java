package org.zstack.tag2;

import org.zstack.header.message.NeedReplyMessage;
import java.util.List;
import java.util.Map;

/**
 * Created by MaJin on 2019/2/11.
 */
public class AttachTagToResourcesMsg extends NeedReplyMessage implements TagPatternMessage {
    private String tagUuid;

    private List<String> resourceUuids;

    private Map<String, String> tokens;

    @Override
    public String getTagPatternUuid() {
        return tagUuid;
    }

    public List<String> getResourceUuids() {
        return resourceUuids;
    }

    public Map<String, String> getTokens() {
        return tokens;
    }

    public String getTagUuid() {
        return tagUuid;
    }

    public void setTagUuid(String tagUuid) {
        this.tagUuid = tagUuid;
    }

    public void setResourceUuids(List<String> resourceUuids) {
        this.resourceUuids = resourceUuids;
    }

    public void setTokens(Map<String, String> tokens) {
        this.tokens = tokens;
    }
}
