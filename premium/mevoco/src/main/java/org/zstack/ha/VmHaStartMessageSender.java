package org.zstack.ha;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.header.core.Completion;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.HaStartVmInstanceMsg;
import org.zstack.header.vm.HaStartVmInstanceReply;
import org.zstack.header.vm.VmInstanceConstant;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VmHaStartMessageSender {
    @Autowired
    private CloudBus bus;
    @Autowired
    private EventFacade evtf;

    private String vmInstanceUuid;
    private List<String> avoidHostUuids = Collections.emptyList();
    private String judgerClassName;
    private String reasonForHA;

    public VmHaStartMessageSender withVmInstance(String uuid) {
        this.vmInstanceUuid = uuid;
        return this;
    }
    public VmHaStartMessageSender withAvoidHost(String... uuids) {
        this.avoidHostUuids = new ArrayList<>(Arrays.asList(uuids));
        return this;
    }
    public VmHaStartMessageSender withJudgerClass(String className) {
        this.judgerClassName = className;
        return this;
    }
    public VmHaStartMessageSender withReasonForHA(String reasonForHA) {
        this.reasonForHA = reasonForHA;
        return this;
    }

    public void send(Completion completion) {
        HaStartVmInstanceMsg msg = new HaStartVmInstanceMsg();
        msg.setVmInstanceUuid(vmInstanceUuid);
        msg.setSoftAvoidHostUuids(avoidHostUuids);
        msg.setJudgerClassName(judgerClassName);
        msg.setHaReason(reasonForHA);
        bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, vmInstanceUuid);
        bus.send(msg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                VmHaCanonicalEvents.VMHaStartedData data = new VmHaCanonicalEvents.VMHaStartedData();
                data.setVmUuid(vmInstanceUuid);
                if (!reply.isSuccess()) {
                    data.setError(reply.getError());
                    evtf.fire(VmHaCanonicalEvents.VM_HA_STARTED_PATH, data);
                    completion.fail(reply.getError());
                    return;
                }

                HaStartVmInstanceReply r = reply.castReply();

                if (r.getInventory() != null) {
                    data.setHostUuid(r.getInventory().getHostUuid());
                    evtf.fire(VmHaCanonicalEvents.VM_HA_STARTED_PATH, data);
                }
                completion.success();
            }
        });
    }
}
