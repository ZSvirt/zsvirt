package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.AttachDataVolumeToVmMsg;
import org.zstack.header.vm.VmInstanceConstant;

import java.util.Collection;
import java.util.Map;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class AttachDataVolumeInOrderFlow extends NoRollbackFlow {
    @Autowired
    private CloudBus bus;

    @Override
    public boolean skip(Map data) {
        return ((Collection) data.get(PremiumVmInstanceConstant.DATA_VOLUME_SNAPSHOT)).isEmpty();
    }

    @Override
    public void run(FlowTrigger trigger, Map data) {
        CreateVmFromVolumeResourceSpec spec = (CreateVmFromVolumeResourceSpec) data.get(PremiumVmInstanceConstant.VM_INSTANCE_FROM_VOLUME_SPEC);

        new While<>(spec.getDataVolumes()).each((vol, compl) -> {
            AttachDataVolumeToVmMsg amsg = new AttachDataVolumeToVmMsg();
            amsg.setVmInstanceUuid(spec.getVmInstance().getUuid());
            amsg.setVolume(vol);
            bus.makeTargetServiceIdByResourceUuid(amsg, VmInstanceConstant.SERVICE_ID, amsg.getVmInstanceUuid());
            bus.send(amsg, new CloudBusCallBack(compl) {
                @Override
                public void run(MessageReply reply) {
                    if (reply.isSuccess()) {
                        compl.done();
                    } else {
                        compl.addError(reply.getError());
                        compl.allDone();
                    }
                }
            });
        }).run(new WhileDoneCompletion(trigger) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (!errorCodeList.getCauses().isEmpty()) {
                    trigger.fail(errorCodeList.getCauses().get(0));
                } else {
                    trigger.next();
                }
            }
        });
    }
}