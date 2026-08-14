package org.zstack.header.vm;

/**
 * Only for UserVM.
 *
 * Appliance VM use {@link ApplianceVmInstanceCreateExtensionPoint}
 */
public interface VmInstanceCreateExtensionPoint {
    void preCreateVmInstance(CreateVmInstanceMsg msg);

    default void afterPersistVmInstanceVO(VmInstanceVO vo, CreateVmInstanceMsg msg) {}

    /**
     * Invoked when VM creation rolls back after
     * {`@link` `#afterPersistVmInstanceVO`(VmInstanceVO, CreateVmInstanceMsg)} so extensions can
     * clean up any state created in that hook. Implementations should be idempotent.
     */
    default void afterRollbackPersistVmInstanceVO(VmInstanceVO vo, CreateVmInstanceMsg msg) {}
}
