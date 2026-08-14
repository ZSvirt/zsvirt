package org.zstack.header.host;

import org.zstack.compute.host.MevocoKVMAgentCommands;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.kvm.KVMHostVO;

/**
 * Created by kayo on 2018/4/2.
 */
public interface KVMChangeHostPasswordExtensionPoint {
    void afterChangeHostPassword(KVMHostVO kvmHostVO);
}
