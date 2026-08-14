package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.*;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.network.l3.*;
import org.zstack.header.vm.*;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Map;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VmPreAllocateNicIpFlow extends NoRollbackFlow {
    private static final CLogger logger = Utils.getLogger(VmPreAllocateNicIpFlow.class);
    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected PluginRegistry pluginRgty;

    @Override
    public void run(final FlowTrigger trigger, final Map data) {
        final L3NetworkInventory l3 = (L3NetworkInventory) data.get(VmInstanceConstant.Params.L3NetworkInventory.toString());
        final VmInstanceInventory vm = (VmInstanceInventory) data.get(VmInstanceConstant.Params.vmInventory.toString());

        /* vmnic is not bound to vm instance */
        if (vm == null) {
            trigger.next();
            return;
        }

        for (VmPreAttachL3NetworkExtensionPoint ext : pluginRgty.getExtensionList(VmPreAttachL3NetworkExtensionPoint.class)) {
            ext.vmPreAttachL3Network(vm, l3);
        }

        trigger.next();
    }

}
