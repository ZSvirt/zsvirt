package org.zstack.header.managementnode;


public interface ManagementNodeChangeListener {
    void nodeJoin(ManagementNodeInventory inv);

    void nodeLeft(ManagementNodeInventory inv);

    void iAmDead(ManagementNodeInventory inv);

    void iJoin(ManagementNodeInventory inv);
}
