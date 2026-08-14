package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowException;
import org.zstack.header.core.workflow.FlowRollback;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.Completion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.network.service.NetworkServiceExtensionPoint;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceConstant.VmOperation;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.vm.VmNicSpec;
import org.zstack.header.vm.VmNicVO;
import org.zstack.network.service.NetworkServiceManager;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import static org.zstack.utils.CollectionDSL.*;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VmApplyNetworkServiceOnChangeIPFlow implements Flow {
    private static final CLogger logger = Utils.getLogger(VmApplyNetworkServiceOnChangeIPFlow.class);
    @Autowired
    private NetworkServiceManager nsMgr;

    @Override
    public void run(FlowTrigger trigger, Map data) {
        VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
        VmNicVO vmNicVO =  (VmNicVO) data.get(VmInstanceConstant.Params.VmNicInventory.toString());

        if (spec.getCurrentVmOperation() == VmOperation.ChangeNicNetwork) {
            L3NetworkInventory l3Inv = (L3NetworkInventory) data.get(VmInstanceConstant.Params.L3NetworkInventory.toString());
            VmInstanceInventory vm = (VmInstanceInventory) data.get(VmInstanceConstant.Params.vmInventory.toString());

            spec.setVmInventory(vm);
            spec.setL3Networks(list(new VmNicSpec(l3Inv)));
        }

        spec.setDestNics(list(VmNicInventory.valueOf(vmNicVO)));

        nsMgr.applyNetworkServiceOnChangeIP(spec, null, new Completion(trigger) {
            @Override
            public void success() {
                trigger.next();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                trigger.fail(errorCode);
            }
        });
    }

    @Override
    public void rollback(FlowRollback trigger, Map data) {
        trigger.rollback();
    }
}
