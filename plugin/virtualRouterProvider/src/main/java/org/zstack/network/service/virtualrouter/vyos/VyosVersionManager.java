package org.zstack.network.service.virtualrouter.vyos;

import org.zstack.header.core.ReturnValueCompletion;

/**
 * Created by shixin on 2018/05/22.
 */
public interface VyosVersionManager {
    void vyosRouterVersionCheck(String vrUuid, ReturnValueCompletion<VyosVersionCheckResult> completion);
}
