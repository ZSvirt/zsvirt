package org.zstack.header.tag;

/**
 * Created by frank on 8/21/2015.
 */
public class AbstractSystemTagLifeCycleListener implements SystemTagLifeCycleListener {
    @Override
    public void tagCreated(SystemTagInventoryDefinition tag) {
    }

    @Override
    public void tagDeleted(SystemTagInventoryDefinition tag) {
    }

    @Override
    public void tagUpdated(SystemTagInventoryDefinition old, SystemTagInventoryDefinition newTag) {
    }
}
