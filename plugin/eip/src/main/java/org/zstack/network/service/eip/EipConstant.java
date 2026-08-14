package org.zstack.network.service.eip;

import org.zstack.header.network.service.NetworkServiceType;
import org.zstack.header.vm.VmInstance;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceState;

import java.util.List;

import static java.util.Arrays.asList;

/**
 */
public interface EipConstant {
    public static final String SERVICE_ID = "eip";
    public static String EIP_NETWORK_SERVICE_TYPE = "Eip";

    public static final String ACTION_CATEGORY = "eip";

    public static final NetworkServiceType EIP_TYPE = new NetworkServiceType(EIP_NETWORK_SERVICE_TYPE);

    public static enum Params {
        NETWORK_SERVICE_PROVIDER_TYPE,
        EIP_STRUCT,
        NEED_LOCK_VIP,
        NEED_UNLOCK_VIP
    }

    public final List<VmInstanceState> noNeedApplyOnBackendVmStates = asList(
            VmInstanceState.Stopped,
            VmInstanceState.VolumeMigrating
    );

    public final List<VmInstanceState> attachableVmStates = asList(
            VmInstanceState.Running,
            VmInstanceState.Paused,
            VmInstanceState.Pausing,
            VmInstanceState.Resuming,
            VmInstanceState.Stopped,
            VmInstanceState.VolumeMigrating
    );

    public final List<VmInstanceConstant.VmOperation> vmOperationForDetachEip = asList(
            VmInstanceConstant.VmOperation.Destroy,
            VmInstanceConstant.VmOperation.DetachNic,
            VmInstanceConstant.VmOperation.ChangeNicIp
    );
}
