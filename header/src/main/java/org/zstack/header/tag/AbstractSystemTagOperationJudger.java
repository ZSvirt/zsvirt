package org.zstack.header.tag;

/**
 * Created by frank on 8/18/2015.
 */
public class AbstractSystemTagOperationJudger implements SystemTagOperationJudger {
    @Override
    public void tagPreCreated(SystemTagInventory tag) {
    }

    @Override
    public void tagPreDeleted(SystemTagInventory tag) {
    }

    @Override
    public void tagPreUpdated(SystemTagInventory old, SystemTagInventory newTag) {
    }
}
