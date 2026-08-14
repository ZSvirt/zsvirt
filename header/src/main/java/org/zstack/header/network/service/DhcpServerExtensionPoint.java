package org.zstack.header.network.service;

import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.network.l3.UsedIpInventory;
import org.zstack.header.vm.VmInstanceSpec;

import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: shixin.ruan
 * Time: 2018/04/10
 * To change this template use File | Settings | File Templates.
 */
public interface DhcpServerExtensionPoint {
    void afterAllocateDhcpServerIP(String L3NetworkUuid, String dhcpSererIp);
    void afterRemoveDhcpServerIP(String L3NetworkUuid, String dhcpSererIp);
}
