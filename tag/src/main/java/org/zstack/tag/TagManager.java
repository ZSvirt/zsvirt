package org.zstack.tag;

import org.zstack.header.message.APICreateMessage;
import org.zstack.header.tag.SystemTagCreateMessageValidator;
import org.zstack.header.tag.SystemTagInventory;
import org.zstack.header.tag.SystemTagResourceDeletionOperator;
import org.zstack.header.tag.TagInventory;

import java.util.Collection;
import java.util.List;

/**
 */
public interface TagManager {
    SystemTagInventory createNonInherentSystemTag(String resourceUuid, String tag, String resourceType);

    SystemTagInventory createInherentSystemTag(String resourceUuid, String tag, String resourceType);

    void createInherentSystemTags(List<String> sysTags, String resourceUuid, String resourceType);

    void createNonInherentSystemTags(List<String> sysTags, String resourceUuid, String resourceType);

    void createTagsFromAPICreateMessage(APICreateMessage msg, String resourceUuid, String resourceType);

    void createTagsFromAPICreateMessage(APICreateMessage msg, String resourceUuid, String resourceType, boolean forceMatch);

    TagInventory createSysTag(String resourceUuid, String tag, String resourceType);

    TagInventory createUserTag(String resourceUuid, String tag, String resourceType);

    TagInventory createSysTag(String resourceUuid, Enum tag, String resourceType);

    TagInventory createUserTag(String resourceUuid, Enum tag, String resourceType);

    void copySystemTag(String srcResourceUuid, String srcResourceType, String dstResourceUuid, String dstResourceType, boolean inherent);

    void copySystemTag(String srcResourceUuid, String srcResourceType, String dstResourceUuid, String dstResourceType);

    SystemTagInventory updateSystemTag(String tagUuid, String newTag);

    List<String> findSystemTags(String resourceUuid);

    List<String> findUserTags(String resourceUuid);

    boolean hasSystemTag(String resourceUuid, String tag);

    boolean hasSystemTag(String resourceUuid, Enum tag);

    SystemTag findMatchingSystemTag(String tag);

    void deleteSystemTag(String uuid);

    /**
     * @return true if any tags have been deleted
     */
    boolean deleteSystemTag(String tag, String resourceUuid, String resourceType, Boolean inherit);

    /**
     * @return true if any tags have been deleted
     */
    boolean deleteSystemTagUseLike(String tag, String resourceUuid, String resourceType, Boolean inherit);

    Collection<String> getManagedEntityNames();

    void validateSystemTag(String resourceUuid, String resourceType, String tag);

    boolean isResourceConfigSystemTag(String tag);

    void installCreateMessageValidator(String resourceType, SystemTagCreateMessageValidator validator);

    void installAfterResourceDeletionOperator(String resourceType, SystemTagResourceDeletionOperator operator);

    void createTags(List<String> systemTags, List<String> userTags, String resourceUuid, String resourceType);

    List<String> filterSystemTags(List<String> systemTags, String resourceType);

    boolean isCloneable(String tag, String resourceType);

    String applyBeforePersist(String resourceUuid, String resourceType, String tag);

    String decryptTokenValue(String resourceType, String tagHead, String tokenName, String tokenValue);

    String transformTagForCopy(String srcResourceUuid, String srcResourceType,
                               String dstResourceUuid, String dstResourceType, String srcTag);
}
