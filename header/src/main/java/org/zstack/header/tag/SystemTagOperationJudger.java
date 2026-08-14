package org.zstack.header.tag;

/**
 * Created by frank on 8/18/2015.
 */
public interface SystemTagOperationJudger {
    void tagPreCreated(SystemTagInventory tag);

    void tagPreDeleted(SystemTagInventory tag);

    void tagPreUpdated(SystemTagInventory old, SystemTagInventory newTag);
}
