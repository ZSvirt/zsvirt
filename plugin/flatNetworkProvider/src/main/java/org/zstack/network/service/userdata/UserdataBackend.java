package org.zstack.network.service.userdata;

import org.zstack.header.core.Completion;
import org.zstack.header.network.service.NetworkServiceProviderType;

/**
 * Created by frank on 10/13/2015.
 */
public interface UserdataBackend {
    NetworkServiceProviderType getProviderType();

    void applyUserdata(UserdataStruct struct, Completion completion);

    void releaseUserdata(UserdataStruct struct, Completion completion);
}
