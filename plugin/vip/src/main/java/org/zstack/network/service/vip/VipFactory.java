package org.zstack.network.service.vip;

/**
 * Created by xing5 on 2016/11/20.
 */
public interface VipFactory {
    String getNetworkServiceProviderType();

    VipBaseBackend getVip(VipVO self);
}
