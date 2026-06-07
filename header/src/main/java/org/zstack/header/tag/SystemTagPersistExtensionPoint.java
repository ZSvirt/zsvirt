package org.zstack.header.tag;

/**
 * Hook before a system tag is persisted to the database.
 */
public interface SystemTagPersistExtensionPoint {
    String beforePersist(String resourceUuid, String resourceType, String tag);
}
