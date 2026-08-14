package org.zstack.network.service.flat;

import org.zstack.header.core.Completion;
import org.zstack.header.message.Message;
import org.zstack.network.service.vip.*;

/**
 * Created by xing5 on 2016/12/5.
 */
public class FlatVipFactory implements VipFactory {
    @Override
    public String getNetworkServiceProviderType() {
        return FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE_STRING;
    }

    @Override
    public VipBaseBackend getVip(VipVO self) {
        return new VipBaseBackend(self) {
            @Override
            protected void releaseVipOnBackend(Completion completion) {
                // fake, the VIP is deleted when the EIP is deleted
                completion.success();
            }

            @Override
            protected void acquireVipOnBackend(Completion completion) {
                // fake, the VIP is created when the EIP is created
                completion.success();
            }

            @Override
            protected void handleBackendSpecificMessage(Message msg) {
                bus.dealWithUnknownMessage(msg);
            }

        };
    }

}
