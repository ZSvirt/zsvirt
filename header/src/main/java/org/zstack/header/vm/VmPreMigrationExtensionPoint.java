package org.zstack.header.vm;

import org.zstack.header.core.Completion;

/**
 * Created by frank on 1/2/2016.
 */
public interface VmPreMigrationExtensionPoint {
    void preVmMigration(VmInstanceInventory vm, VmMigrationType type, String dstHostUuid, Completion completion);
}
