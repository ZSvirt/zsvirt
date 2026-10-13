package org.zstack.ha;

import org.zstack.core.db.Q;
import org.zstack.ha.header.HaErrors;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.apimediator.InterceptorForService;
import org.zstack.header.host.HostStatus;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.HostVO_;
import org.zstack.header.message.APIMessage;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;

import java.util.List;

import static org.zstack.core.Platform.err;

@InterceptorForService("ha")
public class HaInterceptor implements ApiMessageInterceptor {
    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APIUpdateHaStrategyConditionMsg) {
            validate((APIUpdateHaStrategyConditionMsg) msg);
        }

        return msg;
    }

    private void validate(APIUpdateHaStrategyConditionMsg msg) {
        List<String> hostUuids = Q.New(VmInstanceVO.class, HostVO.class)
            .table0()
                .eq(VmInstanceVO_.hostUuid).table1(HostVO_.uuid)
                .eq(VmInstanceVO_.state, VmInstanceState.Unknown)
            .table1()
                .select(HostVO_.uuid)
                .notEq(HostVO_.status, HostStatus.Connected)
                .eq(HostVO_.hypervisorType, VmInstanceConstant.KVM_HYPERVISOR_TYPE)
            .limit(10)
            .list();
        if (!hostUuids.isEmpty()) {
            throw new ApiMessageInterceptionException(err(HaErrors.DISCONNECTED_HOST_HAS_UNKNOWN_VM,
                    "host[uuid:%s] is not %s, but still have vm on it," +
                    " please resolve hosts' problems before update fencer strategy", hostUuids, HostStatus.Connected));
        }
    }
}
