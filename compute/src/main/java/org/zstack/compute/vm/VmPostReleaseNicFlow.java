package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.vm.VmDetachNicExtensionPoint;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.List;
import java.util.Map;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VmPostReleaseNicFlow extends NoRollbackFlow {
    private static final CLogger logger = Utils.getLogger(VmPostReleaseNicFlow.class);
    @Autowired
    protected PluginRegistry pluginRgty;

    @Override
    public void run(final FlowTrigger trigger, final Map data) {
        final VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
        List<VmNicInventory> nics = spec.getVmInventory().getVmNics();
        if (spec.getCurrentVmOperation() == VmInstanceConstant.VmOperation.Expunge) {
            nics = (List<VmNicInventory>) data.get(VmReturnReleaseNicFlow.EXPUNGE_EXTERNAL_NICS);
            if (nics == null) {
                trigger.next();
                return;
            }
        }
        for (VmNicInventory vmNic : nics) {
            for (VmDetachNicExtensionPoint ext : pluginRgty.getExtensionList(VmDetachNicExtensionPoint.class)) {
                ext.afterDetachNic(vmNic);
            }
        }
        trigger.next();
    }
}
