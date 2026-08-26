package org.zstack.header.tag;

/**
 * Hook to transform system tags when copied/cloned between resources.
 * Return null to skip copying the tag.
 */
public interface SystemTagCopyExtensionPoint {
    String transformTagForCopy(String srcResourceUuid, String srcResourceType,
                               String dstResourceUuid, String dstResourceType, String srcTag);
}
