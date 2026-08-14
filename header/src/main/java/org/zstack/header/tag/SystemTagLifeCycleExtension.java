package org.zstack.header.tag;

import java.util.List;

/**
 */
public interface SystemTagLifeCycleExtension {
    List<String> getResourceTypeOfSystemTags();

    void tagCreated(SystemTagInventory tag);

    void tagDeleted(SystemTagInventory tag);

    void tagUpdated(SystemTagInventory old, SystemTagInventory newTag);
}
