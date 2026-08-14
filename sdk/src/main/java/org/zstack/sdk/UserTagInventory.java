package org.zstack.sdk;

import org.zstack.sdk.TagPatternInventory;

public class UserTagInventory extends org.zstack.sdk.TagInventory {

    public java.lang.String tagPatternUuid;
    public void setTagPatternUuid(java.lang.String tagPatternUuid) {
        this.tagPatternUuid = tagPatternUuid;
    }
    public java.lang.String getTagPatternUuid() {
        return this.tagPatternUuid;
    }

    public TagPatternInventory tagPattern;
    public void setTagPattern(TagPatternInventory tagPattern) {
        this.tagPattern = tagPattern;
    }
    public TagPatternInventory getTagPattern() {
        return this.tagPattern;
    }

}
