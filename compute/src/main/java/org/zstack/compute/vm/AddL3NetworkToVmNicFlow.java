package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.vm.*;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Map;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class AddL3NetworkToVmNicFlow extends NoRollbackFlow {
    private static final CLogger logger = Utils.getLogger(AddL3NetworkToVmNicFlow.class);
    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected PluginRegistry pluginRgty;
    @Autowired
    protected CloudBus bus;

    @Override
    public void run(final FlowTrigger trigger, final Map data) {
        final VmInstanceInventory vm = (VmInstanceInventory) data.get(VmInstanceConstant.Params.vmInventory.toString());
        final VmNicInventory nic = (VmNicInventory) data.get(VmInstanceConstant.Params.VmNicInventory.toString());
        final L3NetworkInventory l3Inv = (L3NetworkInventory)data.get(VmInstanceConstant.Params.L3NetworkInventory.toString());

        if (!vm.getState().equals(VmInstanceState.Running.toString())) {
            trigger.next();
            return;
        }

        AddL3NetworkToVmNicMsg msg = new AddL3NetworkToVmNicMsg();
        msg.setVmInstanceUuid(vm.getUuid());
        msg.setVmNicUuid(nic.getUuid());
        msg.setNewL3Uuid(l3Inv.getUuid());

        bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, vm.getUuid());

        bus.send(msg, new CloudBusCallBack(trigger) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    trigger.next();
                } else {
                    trigger.fail(reply.getError());
                }
            }
        });
    }

}
