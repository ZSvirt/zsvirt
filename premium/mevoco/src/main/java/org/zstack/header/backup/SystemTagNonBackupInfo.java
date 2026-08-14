package org.zstack.header.backup;

import org.apache.logging.log4j.util.Strings;
import org.zstack.header.tag.SystemTagVO;
import org.zstack.tag.PatternedSystemTag;

import java.util.Collections;

public class SystemTagNonBackupInfo extends NonBackupInfo {
    protected String tagFormat;

    public SystemTagNonBackupInfo() {
        attributeName = "tag";
    }

    public void setTagFormat(String tagFormat) {
        this.tagFormat = tagFormat;
    }

    public String getTagFormat() {
        return tagFormat;
    }

    public void setTokenName(String token) {
        this.name = token;
    }

    public String buildUpdateSql() {
        if (Strings.isEmpty(newValue) || originValue.equals(newValue)) {
            return null;
        }

        String newTag = new PatternedSystemTag(tagFormat, SystemTagVO.class).instantiateTag(Collections.singletonMap(name, newValue));
        return String.format("update %s set %s = '%s' where uuid = '%s'", resourceType, attributeName, newTag, uuid);
    }

    @Override
    public String getResourceType() {
        return SystemTagVO.class.getSimpleName();
    }
}
