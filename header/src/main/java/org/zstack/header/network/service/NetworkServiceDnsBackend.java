package org.zstack.header.network.service;

import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.vm.VmInstanceSpec;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 1:45 PM
 * To change this template use File | Settings | File Templates.
 */
public interface NetworkServiceDnsBackend {
    NetworkServiceProviderType getProviderType();

    void addDns(L3NetworkInventory l3, List<String> dns, Completion completion);

    void removeDns(L3NetworkInventory l3, List<String> dns, Completion completion);

    void applyDnsService(List<DnsStruct> dnsStructList, VmInstanceSpec spec, Completion completion);

    void releaseDnsService(List<DnsStruct> dnsStructsList, VmInstanceSpec spec, NoErrorCompletion completion);
}
