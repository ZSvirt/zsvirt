package org.zstack.network.plugin;

import org.zstack.header.core.Completion;
import org.zstack.header.network.service.NetworkServiceProviderType;
import org.zstack.network.service.userdata.UserdataStruct;

import java.util.List;

/**
 * Created by shixin.ruan on 2021/08/09.
 */
public interface GratuitousARPBackend {
    NetworkServiceProviderType getProviderType();

    void applyGratuitousARP(GratuitousARPStruct struct, Completion completion);

    void releaseGratuitousARP(GratuitousARPStruct struct, Completion completion);

    void updateGratuitousARPSettings(String newValue, Boolean state, List<String> hostUuids);
}
