package org.zstack.header.tag;

/**
 * Created by frank on 8/21/2015.
 */
public class AbstractSystemTagLifeCycleListener implements SystemTagLifeCycleListener {
    @Override
    public void tagCreated(SystemTagInventory tag) {
    }

    @Override
    public void tagDeleted(SystemTagInventory tag) {
    }

    @Override
    public void tagUpdated(SystemTagInventory old, SystemTagInventory newTag) {
    }
}
