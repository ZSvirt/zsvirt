package org.zstack.network.service.flat;

import org.zstack.core.gc.EventBasedGarbageCollector;
import org.zstack.core.gc.GC;
import org.zstack.core.gc.GCCompletion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.host.HostCanonicalEvents;
import org.zstack.header.host.HostStatus;
import org.zstack.header.host.HostVO;
import org.zstack.kvm.KvmCommandSender;
import org.zstack.kvm.KvmResponseWrapper;
import static org.zstack.core.Platform.operr;

/**
 * Created by xing5 on 2017/3/6.
 */
public class FlatDHCPDeleteNamespaceGC extends EventBasedGarbageCollector {
    @GC
    public FlatDhcpBackend.DeleteNamespaceCmd command;
    @GC
    public String path;
    @GC
    public String hostUuid;

    @Override
    protected void triggerNow(GCCompletion completion) {
        if (!dbf.isExist(hostUuid, HostVO.class)) {
            // the host is deleted;
            completion.cancel();
            return;
        }

        new KvmCommandSender(hostUuid).send(command, path,
                wrapper -> {
                    FlatDhcpBackend.DeleteNamespaceRsp rsp = wrapper.getResponse(FlatDhcpBackend.DeleteNamespaceRsp.class);
                    return rsp.isSuccess() ? null : operr("operation error, because:%s", rsp.getError());
                },

                new ReturnValueCompletion<KvmResponseWrapper>(completion) {
                    @Override
                    public void success(KvmResponseWrapper w) {
                        completion.success();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        completion.fail(errorCode);
                    }
                });
    }

    @Override
    protected void setup() {
        onEvent(HostCanonicalEvents.HOST_STATUS_CHANGED_PATH, ((tokens, data) -> {
            HostCanonicalEvents.HostStatusChangedData d = (HostCanonicalEvents.HostStatusChangedData) data;
            return hostUuid.equals(d.getHostUuid()) && d.getNewStatus().equals(HostStatus.Connected.toString());
        }));

        onEvent(HostCanonicalEvents.HOST_DELETED_PATH, ((tokens, data) -> {
            HostCanonicalEvents.HostDeletedData d = (HostCanonicalEvents.HostDeletedData) data;
            return hostUuid.equals(d.getHostUuid());
        }));
    }
}
