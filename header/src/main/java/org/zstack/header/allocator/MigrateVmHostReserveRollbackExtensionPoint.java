package org.zstack.header.allocator;

import org.zstack.header.core.Completion;
import org.zstack.header.vm.VmInstanceSpec;

/**
 * Rolls back a host reservation made while allocating a destination host for live migration.
 * The reservation may rewrite {@code VmInstanceVO.hostUuid} before migration succeeds;
 * a later migration failure must restore the original host before VM state is reconciled.
 */
public interface MigrateVmHostReserveRollbackExtensionPoint {
    void rollbackAfterFailedMigration(VmInstanceSpec spec, Completion completion);
}
