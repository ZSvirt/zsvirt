package org.zstack.header.tag;

/**
 */
public interface SystemTagLifeCycleListener {
    void tagCreated(SystemTagInventory tag);

    void tagDeleted(SystemTagInventory tag);

    void tagUpdated(SystemTagInventory old, SystemTagInventory newTag);
}
