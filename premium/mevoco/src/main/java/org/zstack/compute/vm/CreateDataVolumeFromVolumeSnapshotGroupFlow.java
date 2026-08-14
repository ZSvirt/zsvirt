package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowRollback;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.image.*;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO;
import org.zstack.header.vm.APICreateVmInstanceFromVolumeSnapshotGroupMsg;
import org.zstack.header.volume.*;

import java.util.*;

/**
 * Created by MaJin on 2021/3/16.
 */

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class CreateDataVolumeFromVolumeSnapshotGroupFlow implements Flow {
    @Autowired
    private CloudBus bus;

    @Override
    public boolean skip(Map data) {
        return ((Collection) data.get(PremiumVmInstanceConstant.DATA_VOLUME_SNAPSHOT)).isEmpty();
    }

    @Override
    @SuppressWarnings("unchecked")
    public void run(FlowTrigger trigger, Map data) {
        CreateVmFromVolumeResourceSpec spec = (CreateVmFromVolumeResourceSpec) data.get(PremiumVmInstanceConstant.VM_INSTANCE_FROM_VOLUME_SPEC);
        APICreateVmInstanceFromVolumeSnapshotGroupMsg msg = (APICreateVmInstanceFromVolumeSnapshotGroupMsg) spec.getApiMsg();

        List<VolumeSnapshotVO> snaps = (List) data.get(PremiumVmInstanceConstant.DATA_VOLUME_SNAPSHOT);
        Map<String, Integer> dataVolDeviceIds = new HashMap<>();

        new While<>(snaps).step((snap, compl) -> {
            CreateDataVolumeFromVolumeSnapshotMsg cmsg = new CreateDataVolumeFromVolumeSnapshotMsg();
            cmsg.setName("data-volume-from-snapshot-" + snap.getUuid());
            cmsg.setSession(msg.getSession());
            cmsg.setVolumeSnapshotUuid(snap.getUuid());
            cmsg.setSystemTags(msg.getDataVolumeSystemTags(snap.getUuid()));
            bus.makeLocalServiceId(cmsg, VolumeConstant.SERVICE_ID);
            bus.send(cmsg, new CloudBusCallBack(compl) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        compl.addError(reply.getError());
                        compl.allDone();
                        return;
                    }

                    CreateDataVolumeFromVolumeSnapshotReply r = reply.castReply();
                    spec.addDataVolume(r.getInventory());
                    dataVolDeviceIds.put(r.getInventory().getUuid(), snap.getGroupRef().getDeviceId());
                    compl.done();
                }
            });

        }, 5).run(new WhileDoneCompletion(trigger) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (!errorCodeList.getCauses().isEmpty()) {
                    trigger.fail(errorCodeList.getCauses().get(0));
                    return;
                }

                spec.getDataVolumes().sort(Comparator.comparing(vol -> dataVolDeviceIds.get(vol.getUuid())));
                trigger.next();
            }
        });
    }


    @Override
    public void rollback(FlowRollback trigger, Map data) {
        CreateVmFromVolumeResourceSpec spec = (CreateVmFromVolumeResourceSpec) data.get(PremiumVmInstanceConstant.VM_INSTANCE_FROM_VOLUME_SPEC);
        new While<>(spec.getDataVolumes()).all((vol, compl) -> {
            VolumeDeletionMsg msg = new VolumeDeletionMsg();
            msg.setDeletionPolicy(ImageDeletionPolicyManager.ImageDeletionPolicy.Direct.toString());
            msg.setVolumeUuid(vol.getUuid());
            bus.makeTargetServiceIdByResourceUuid(msg, VolumeConstant.SERVICE_ID, msg.getVolumeUuid());
            bus.send(msg, new CloudBusCallBack(trigger) {
                @Override
                public void run(MessageReply reply) {
                    compl.done();
                }
            });
        }).run(new WhileDoneCompletion(trigger) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                trigger.rollback();
            }
        });
    }
}
