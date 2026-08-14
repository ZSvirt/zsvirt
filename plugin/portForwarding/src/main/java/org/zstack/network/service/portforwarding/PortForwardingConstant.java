package org.zstack.network.service.portforwarding;

import org.zstack.header.network.service.NetworkServiceType;
import org.zstack.header.vm.VmInstanceConstant;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;

public interface PortForwardingConstant {
    public static String SERVICE_ID = "portForwarding";
    public static String PORTFORWARDING_NETWORK_SERVICE_TYPE = "PortForwarding";
    public static String ACTION_CATEGORY = "portForwarding";

    public static NetworkServiceType PORTFORWARDING_TYPE = new NetworkServiceType(PORTFORWARDING_NETWORK_SERVICE_TYPE);

    public static enum Params {
        PORTFORWARDING_STRUCT,
        PORTFORWARDING_SERVICE_PROVIDER_TYPE,
        NEED_LOCK_VIP,
        NEED_UNLOCK_VIP
    }

    public final List<VmInstanceConstant.VmOperation> vmOperationForDetachPortfordingRule = asList(
            VmInstanceConstant.VmOperation.Destroy,
            VmInstanceConstant.VmOperation.DetachNic,
            VmInstanceConstant.VmOperation.ChangeNicNetwork,
            VmInstanceConstant.VmOperation.ChangeNicIp
    );
}
