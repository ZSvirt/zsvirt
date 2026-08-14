package org.zstack.network.service.eip;

import org.zstack.header.core.Completion;

/**
 */
public interface EipBackend {
    void applyEip(EipStruct struct, Completion completion);

    void revokeEip(EipStruct struct, Completion completion);

    String getNetworkServiceProviderType();
}
