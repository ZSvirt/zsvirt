package org.zstack.header.tag;

/**
 */
public interface SystemTagValidator {
    void validateSystemTag(String resourceUuid, Class resourceType, String systemTag);
}
