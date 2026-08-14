package org.zstack.mevoco;

import org.zstack.header.host.HostInventory;
import org.zstack.kvm.KVMHostDeployArguments;

/**
 * @author Xingwei Yu
 * @date 2024/7/4 17:13
 */
public interface QemuConfigItemOperator {
    String getStatus(HostInventory host);
    boolean isEnabled();
    void applyConfig(KVMHostDeployArguments args);
    void createOrUpdateTag(String uuid);
    void rollbackTag(String uuid);
}
