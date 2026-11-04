package org.zstack.header.tag;

/**
 * Created by frank on 8/18/2015.
 */
public interface SystemTagOperationJudger {
    void tagPreCreated(SystemTagInventoryDefinition tag);

    void tagPreDeleted(SystemTagInventoryDefinition tag);

    void tagPreUpdated(SystemTagInventoryDefinition old, SystemTagInventoryDefinition newTag);
}
