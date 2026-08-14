package org.zstack.network.service.vip;

/**
 * Created by kayo on 2018/7/20.
 */
public interface VipCleanupExtensionPoint {
    void cleanupVip(String uuid);
}
