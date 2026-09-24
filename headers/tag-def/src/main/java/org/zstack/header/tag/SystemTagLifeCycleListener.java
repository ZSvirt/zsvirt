package org.zstack.header.tag;

/**
 */
public interface SystemTagLifeCycleListener {
    void tagCreated(SystemTagInventoryDefinition tag);

    void tagDeleted(SystemTagInventoryDefinition tag);

    void tagUpdated(SystemTagInventoryDefinition old, SystemTagInventoryDefinition newTag);
}
