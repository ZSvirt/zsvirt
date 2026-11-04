package org.zstack.header.tag;

/**
 * Created by frank on 8/18/2015.
 */
public class AbstractSystemTagOperationJudger implements SystemTagOperationJudger {
    @Override
    public void tagPreCreated(SystemTagInventoryDefinition tag) {
    }

    @Override
    public void tagPreDeleted(SystemTagInventoryDefinition tag) {
    }

    @Override
    public void tagPreUpdated(SystemTagInventoryDefinition old, SystemTagInventoryDefinition newTag) {
    }
}
