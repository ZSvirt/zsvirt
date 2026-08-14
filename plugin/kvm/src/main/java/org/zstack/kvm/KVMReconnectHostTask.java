package org.zstack.kvm;

import org.zstack.compute.host.HostReconnectTask;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.db.Q;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.host.HostErrors;
import org.zstack.utils.network.NetworkUtils;

import javax.persistence.Tuple;
import java.util.concurrent.TimeUnit;

public class KVMReconnectHostTask extends HostReconnectTask {
    public KVMReconnectHostTask(String uuid, NoErrorCompletion completion) {
        super(uuid, completion);
    }

    @Override
    protected CanDoAnswer canDoReconnect() {
        if (CoreGlobalProperty.UNIT_TEST_ON) {
            return CanDoAnswer.Ready;
        }

        Tuple t = Q.New(KVMHostVO.class).select(KVMHostVO_.managementIp, KVMHostVO_.port).eq(KVMHostVO_.uuid, uuid).findTuple();
        if (t == null) {
            return CanDoAnswer.NoReconnect;
        }

        String ip = t.get(0, String.class);
        int port = t.get(1, Integer.class);

        return NetworkUtils.isRemotePortOpen(ip, port, (int) TimeUnit.SECONDS.toMillis(2)) ?
                CanDoAnswer.Ready : CanDoAnswer.NotReady;
    }

    @Override
    protected void whenConnectFail(ErrorCode errorCode) {
        if (errorCode.rootCause().isError(HostErrors.HOST_PASSWORD_HAS_BEEN_CHANGED)) {
            logger.warn(String.format(
                    "stop reconnecting to the host[uuid:%s] until its password is updated correctly", uuid));
            completion.done();
            return;
        }

        super.whenConnectFail(errorCode);
    }
}
