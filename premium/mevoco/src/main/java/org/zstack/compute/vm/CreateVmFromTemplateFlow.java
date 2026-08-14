package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.Q;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.message.MessageReply;
import org.zstack.header.tag.SystemTagVO;
import org.zstack.header.tag.SystemTagVO_;
import org.zstack.header.vm.*;
import org.zstack.kvm.tpm.SnapshotGroupRevertTpmHelper;

import java.util.List;
import java.util.Map;

/**
 * Created by MaJin on 2021/3/16.
 */

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class CreateVmFromTemplateFlow extends NoRollbackFlow {
    @Autowired
    CloudBus bus;

    @Override
    public void run(FlowTrigger trigger, Map data) {
        CreateVmFromVolumeResourceSpec spec = (CreateVmFromVolumeResourceSpec) data.get(PremiumVmInstanceConstant.VM_INSTANCE_FROM_VOLUME_SPEC);
        CreateVmInstanceMsg cmsg = NewVmInstanceMsgBuilder.fromAPINewVmInstanceMsg(spec.getApiMsg());
        cmsg.setImageUuid(spec.getRootVolumeImage().getUuid());
        if (cmsg.getHostUuid() == null) {
            cmsg.setHostUuid(spec.getHostUuid());
        }

        String systemAllocateClusterUuid = spec.getClusterUuid();
        if (!spec.getDataVolumes().isEmpty()) {
            cmsg.setStrategy(VmCreationStrategy.CreateStopped.toString());
            systemAllocateClusterUuid = spec.getPreAllocateClusterUuids().iterator().next();
        }

        if (cmsg.getClusterUuid() == null && cmsg.getHostUuid() == null) {
            cmsg.setClusterUuid(systemAllocateClusterUuid);
        }

        if (cmsg.getCandidatePrimaryStorageUuidsForRootVolume().isEmpty()) {
            cmsg.setPrimaryStorageUuidForRootVolume(spec.getPrimaryStorageUuidForRootVolume());
        }

        if (spec.getRootVolumeSystemTags() != null && !spec.getRootVolumeSystemTags().isEmpty()) {
            cmsg.setRootVolumeSystemTags(spec.getRootVolumeSystemTags());
        } else {
            List<String> systemTags = Q.New(SystemTagVO.class).select(SystemTagVO_.tag)
                    .eq(SystemTagVO_.resourceUuid, spec.getOriginVolumeUuidForRootVolume())
                    .listValues();
            cmsg.setRootVolumeSystemTags(systemTags);
        }

        if (spec.getApiMsg() instanceof APICreateVmInstanceFromVolumeSnapshotGroupMsg) {
            new SnapshotGroupRevertTpmHelper().setupFromApi(
                    (APICreateVmInstanceFromVolumeSnapshotGroupMsg) spec.getApiMsg(), cmsg);
        }

        bus.makeLocalServiceId(cmsg, VmInstanceConstant.SERVICE_ID);
        bus.send(cmsg, new CloudBusCallBack(trigger) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    trigger.fail(reply.getError());
                    return;
                }

                CreateVmInstanceReply r = reply.castReply();
                spec.setVmInstance(r.getInventory());
                trigger.next();
            }
        });
    }
}
