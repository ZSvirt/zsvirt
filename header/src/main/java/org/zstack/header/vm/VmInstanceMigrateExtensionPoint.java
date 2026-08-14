package org.zstack.header.vm;

import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.errorcode.ErrorCode;

public interface VmInstanceMigrateExtensionPoint {
    default void preMigrateVm(VmInstanceInventory inv, String destHostUuid) {};

    default void preMigrateVm(VmInstanceInventory inv, String destHostUuid, Completion completion) {
        preMigrateVm(inv, destHostUuid);
        completion.success();
    }

    default void beforeMigrateVm(VmInstanceInventory inv, String destHostUuid) {}

    default void beforeMigrateVm(VmInstanceInventory inv, String destHostUuid, NoErrorCompletion completion) {
        beforeMigrateVm(inv, destHostUuid);
        completion.done();
    }

    default void postMigrateVm(VmInstanceInventory inv, String destHostUuid) {}

    default void postMigrateVm(VmInstanceInventory inv, String destHostUuid, Completion completion) {
        postMigrateVm(inv, destHostUuid);
        completion.success();
    }

    default void afterMigrateVm(VmInstanceInventory inv, String srcHostUuid) {};

    default void afterMigrateVm(VmInstanceInventory inv, String srcHostUuid, NoErrorCompletion completion) {
        afterMigrateVm(inv, srcHostUuid);
        completion.done();
    }

    default void failedToMigrateVm(VmInstanceInventory inv, String destHostUuid, ErrorCode reason) {};

    default void failedToMigrateVm(VmInstanceInventory inv, String destHostUuid, ErrorCode reason, NoErrorCompletion completion) {
        failedToMigrateVm(inv, destHostUuid, reason);
        completion.done();
    }
}
