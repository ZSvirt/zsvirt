package org.zstack.network.service.lb;

/**
 * Created by camile on 2017/5/19.
 */
public interface LoadBalancerListenerMsg {
    String getLoadBalancerListenerUuid();

    void setLoadBalancerUuid(String l3Uuid);
}
