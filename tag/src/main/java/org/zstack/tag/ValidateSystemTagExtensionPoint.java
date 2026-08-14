package org.zstack.tag;

public interface ValidateSystemTagExtensionPoint {
    boolean validateSystemTag(String resourceUuid, String resourceType, String tag);
}
