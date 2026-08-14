package org.zstack.kvm;

import org.zstack.compute.host.HostReconnectTask;
import org.zstack.compute.host.HostReconnectTaskFactory;
import org.zstack.core.Platform;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.host.HostErrors;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

public class KVMHostReconnectTaskFactory implements HostReconnectTaskFactory {
    protected static final CLogger logger = Utils.getLogger(KVMHostReconnectTaskFactory.class);

    @Override
    public HostReconnectTask createTask(String uuid, NoErrorCompletion completion) {
        return Platform.New(() -> new KVMReconnectHostTask(uuid, completion));
    }

    @Override
    public HostReconnectTask createTaskWithLastConnectError(String hostUuid, ErrorCode errorCode, NoErrorCompletion completion) {
        if (errorCode.rootCause().isError(HostErrors.HOST_PASSWORD_HAS_BEEN_CHANGED)) {
            logger.warn(String.format(
                    "stop tracking host[uuid:%s] until its password is updated correctly", hostUuid));

            return Platform.New(() -> new KVMReconnectHostTask(hostUuid, completion) {
                @Override
                protected CanDoAnswer canDoReconnect() {
                    return CanDoAnswer.NoReconnect;
                }
            });
        }
        return HostReconnectTaskFactory.super.createTaskWithLastConnectError(hostUuid, errorCode, completion);
    }

    @Override
    public String getHypervisorType() {
        return KVMConstant.KVM_HYPERVISOR_TYPE;
    }
}
