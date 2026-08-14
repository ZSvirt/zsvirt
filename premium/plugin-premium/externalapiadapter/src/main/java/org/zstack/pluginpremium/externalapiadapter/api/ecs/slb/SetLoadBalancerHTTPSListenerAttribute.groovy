package org.zstack.pluginpremium.externalapiadapter.api.ecs.slb

import org.zstack.sdk.LoadBalancerListenerInventory
import org.zstack.sdk.RemoveCertificateFromLoadBalancerListenerAction

/**
 * Created by Qi Le on 2019-07-29
 */
class SetLoadBalancerHTTPSListenerAttribute extends SetLoadBalancerListenerBase {
    private static final String protocol = "https"

    @Override
    void afterCallZStackAction(Object zstackActionResult) {
        if (ecsAPIParamMap.containsKey("ServerCertificateId")) {
            removeCertificate(zstackActionResult as LoadBalancerListenerInventory)
        }
        super.afterCallZStackAction(zstackActionResult)
    }

    private void removeCertificate(LoadBalancerListenerInventory listener) {
        if (listener.certificateRefs.size() == 0) {
            return
        }

        String certificateId = listener.certificateRefs.get(0).certificateUuid
        def action = new RemoveCertificateFromLoadBalancerListenerAction(
                sessionId: sessionId,
                certificateUuid: certificateId,
                listenerUuid: listener.uuid
        )
        RemoveCertificateFromLoadBalancerListenerAction.Result result = action.call()
        result.throwExceptionIfError()
    }
}
