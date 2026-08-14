package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.Q;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.volume.*;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VmExpungeCacheVolumeFlow extends NoRollbackFlow {
    private static final CLogger logger = Utils.getLogger(VmExpungeRootVolumeFlow.class);

    @Autowired
    protected CloudBus bus;

    @Override
    public void run(FlowTrigger trigger, Map data) {
        final VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
        if (spec.getVmInventory().getAllVolumes() == null) {
            trigger.next();
        }

        List<String> cacheVolUuids = spec.getVmInventory().getAllVolumes().stream().filter(v -> v.getType().equals(VolumeType.Cache.toString())).map(VolumeInventory::getUuid).collect(Collectors.toList());
        if (cacheVolUuids.isEmpty()) {
            trigger.next();
            return;
        }

        List<VolumeVO> volumes = Q.New(VolumeVO.class).in(VolumeVO_.uuid, cacheVolUuids).list();
        if (volumes.isEmpty()) {
            trigger.next();
            return;
        }

        new While<>(volumes).each((vol, c) -> {
            ExpungeVolumeMsg msg = new ExpungeVolumeMsg();
            msg.setVolumeUuid(vol.getUuid());
            bus.makeTargetServiceIdByResourceUuid(msg, VolumeConstant.SERVICE_ID, vol.getUuid());
            bus.send(msg, new CloudBusCallBack(c) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        logger.warn(String.format("failed to expunge the cache volume[uuid:%s] of the vm[uuid:%s, name:%s], %s",
                                spec.getVmInventory().getRootVolumeUuid(), spec.getVmInventory().getUuid(),
                                spec.getVmInventory().getName(), reply.getError()));

                        c.addError(reply.getError());
                    }

                    c.done();
                }
            });
        }).run(new WhileDoneCompletion(trigger) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (!errorCodeList.getCauses().isEmpty()) {
                    trigger.fail(errorCodeList.getCauses().get(0));
                    return;
                }

                trigger.next();
            }
        });
    }
}
