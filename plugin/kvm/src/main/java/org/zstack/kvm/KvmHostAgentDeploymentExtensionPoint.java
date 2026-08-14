package org.zstack.kvm;

import org.zstack.header.host.HostInventory;

import java.util.List;

public interface KvmHostAgentDeploymentExtensionPoint {
    List<String> appendExtraPackages(HostInventory host);

    void modifyDeploymentArguments(HostInventory host, KVMHostDeployArguments args);
}