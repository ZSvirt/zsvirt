package org.zstack.compute.vm;

import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.Q;
import org.zstack.core.gc.*;
import org.zstack.header.host.*;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.*;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.operr;

/**
 * Created by xing5 on 2017/3/3.
 */
public class DeleteVmGC extends EventBasedGarbageCollector {
    @GC
    public String hostUuid;
    @GC
    public VmInstanceInventory inventory;

    @Override
    protected void setup() {
        onEvent(HostCanonicalEvents.HOST_DELETED_PATH, (tokens, data) -> {
            HostCanonicalEvents.HostDeletedData d = (HostCanonicalEvents.HostDeletedData) data;
            return hostUuid.equals(d.getHostUuid());
        });

        onEvent(HostCanonicalEvents.HOST_STATUS_CHANGED_PATH, (tokens, data) -> {
            HostCanonicalEvents.HostStatusChangedData d = (HostCanonicalEvents.HostStatusChangedData) data;
            return d.getHostUuid().equals(hostUuid) && d.getNewStatus().equals(HostStatus.Connected.toString());
        });
    }

    @Override
    protected void triggerNow(GCCompletion completion) {
        HostStatus status = Q.New(HostVO.class).select(HostVO_.status)
                .eq(HostVO_.uuid, hostUuid).findValue();
        if (status == null) {
            // the host has been deleted
            completion.cancel();
            return;
        }

        if (status != HostStatus.Connected) {
            completion.fail(operr("the host[uuid:%s] is not connected", hostUuid));
            return;
        }

        VmInstanceState vmstate = Q.New(VmInstanceVO.class).select(VmInstanceVO_.state)
                .eq(VmInstanceVO_.uuid, inventory.getUuid()).findValue();
        //zhanyong.miao, GC will be executed with the case in ZSTAC-17639.
        if (vmstate != null && (vmstate != VmInstanceState.Destroyed && vmstate != VmInstanceState.Destroying
                &&vmstate != VmInstanceState.Stopped)) {
            // the vm has been recovered
            completion.cancel();
            return;
        }

        VmDirectlyDestroyOnHypervisorMsg msg = new VmDirectlyDestroyOnHypervisorMsg();
        msg.setHostUuid(hostUuid);
        msg.setVmUuid(inventory.getUuid());
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    completion.success();
                } else {
                    completion.fail(reply.getError());
                }
            }
        });
    }

    public static Collection<String> queryVmInGC(final String hostUuid, final Collection<String> vmUuids) {
        Collection<String> vmUuidsInGC = new HashSet<>();
        List<String> gcNames = Q.New(GarbageCollectorVO.class).select(GarbageCollectorVO_.name)
                                        .eq(GarbageCollectorVO_.runnerClass, DeleteVmGC.class.getName())
                                        .like(GarbageCollectorVO_.name, String.format("%%on-host-%s%%", hostUuid))
                                        .notEq(GarbageCollectorVO_.status, GCStatus.Done).listValues();
        if (gcNames != null && !gcNames.isEmpty()) {
            vmUuidsInGC = vmUuids.stream().filter(uuid ->
                    gcNames.contains(String.format("gc-vm-%s-on-host-%s", uuid, hostUuid))).collect(Collectors.toSet());
        }
        return vmUuidsInGC;
    }
}
