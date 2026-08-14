package org.zstack.header.network.service;

import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceSpec;

import java.util.List;

/**
 * Created by AlanJager on 2017/7/8.
 */
public interface NetworkServiceCentralizedDnsBackend {
    NetworkServiceProviderType getProviderType();

    void applyForwardDnsService(List<ForwardDnsStruct> forwardDnsStructs, VmInstanceSpec spec, Completion completion);

    void releaseForwardDnsService(List<ForwardDnsStruct> forwardDnsStructs, VmInstanceSpec spec, NoErrorCompletion completion);
}
