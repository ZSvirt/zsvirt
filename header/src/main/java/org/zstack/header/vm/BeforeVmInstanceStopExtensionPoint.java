package org.zstack.header.vm;

import org.zstack.header.core.Completion;

/**
 * Created by GuoYi on 2019-09-24.
 *
 * Unlike VmInstanceStopExtensionPoint, this is run strictly before vm is stopped.
 * Operations like getting vm guest tools info must be performed while the vm is still running.
 */
public interface BeforeVmInstanceStopExtensionPoint {
    void beforeVmInstanceStop(VmInstanceInventory inv, Completion completion);
}
