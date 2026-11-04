package org.zstack.storage.cbt;

import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.Q;
import org.zstack.core.gc.GC;
import org.zstack.core.gc.GCCompletion;
import org.zstack.core.gc.TimeBasedGarbageCollector;
import org.zstack.header.host.HostConstant;
import org.zstack.header.host.HostStatus;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.HostVO_;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeVO;
import org.zstack.kvm.KVMAgentCommands;
import org.zstack.kvm.KVMHostAsyncHttpCallMsg;
import org.zstack.kvm.KVMHostAsyncHttpCallReply;
import org.zstack.kvm.KVMHostInventory;
import org.zstack.kvm.KVMHostVO;
import org.zstack.kvm.VolumeTO;

import java.util.ArrayList;
import java.util.List;

import static org.zstack.core.Platform.operr;

public class UnexportNbdVolumesGC extends TimeBasedGarbageCollector {
    @GC
    public String hostUuid;
    @GC
    public String vmUuid;
    @GC
    public List<VolumeTO> volumes;

    public void prepare(String vmUuid, List<String> volumeUuids) {
        this.vmUuid = vmUuid;
        this.hostUuid = findVmHostUuid(vmUuid);
        KVMHostVO host = dbf.findByUuid(hostUuid, KVMHostVO.class);
        KVMHostInventory hostInventory = host == null ? null : KVMHostInventory.valueOf(host);
        this.volumes = new ArrayList<>();
        for (String volumeUuid : volumeUuids) {
            VolumeVO volume = dbf.findByUuid(volumeUuid, VolumeVO.class);
            if (volume != null && hostInventory != null) {
                volumes.add(VolumeTO.valueOf(VolumeInventory.valueOf(volume), hostInventory));
            }
        }
    }

    @Override
    protected void triggerNow(GCCompletion completion) {
        if (!lock()) {
            return;
        }

        HostStatus status = Q.New(HostVO.class).eq(HostVO_.uuid, hostUuid).select(HostVO_.status).findValue();
        if (status == null || volumes == null || volumes.isEmpty()) {
            completion.success();
            return;
        }
        if (status != HostStatus.Connected) {
            completion.fail(operr("the host[uuid:%s] is not connected", hostUuid));
            return;
        }

        CbtBackupKvmCommands.UnexportNbdVolumesCmd cmd = new CbtBackupKvmCommands.UnexportNbdVolumesCmd();
        cmd.volumes = volumes;
        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setCommand(cmd);
        msg.setHostUuid(hostUuid);
        msg.setPath(CbtBackupKvmCommands.REST_API_UNEXPORT_NDB_VOLUMES_PATH);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }
                KVMHostAsyncHttpCallReply r = reply.castReply();
                KVMAgentCommands.AgentResponse rsp = r.toResponse(KVMAgentCommands.AgentResponse.class);
                if (rsp.isSuccess()) {
                    completion.success();
                } else {
                    completion.fail(operr("%s", rsp.getError()));
                }
            }
        });
    }

    private String findVmHostUuid(String vmUuid) {
        String uuid = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, vmUuid)
                .select(VmInstanceVO_.hostUuid).findValue();
        return uuid != null ? uuid : Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, vmUuid)
                .select(VmInstanceVO_.lastHostUuid).findValue();
    }
}
