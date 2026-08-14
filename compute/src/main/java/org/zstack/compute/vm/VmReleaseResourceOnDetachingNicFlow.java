package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.vm.*;

import java.util.Iterator;
import java.util.Map;

/**
 * Created by frank on 7/18/2015.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VmReleaseResourceOnDetachingNicFlow extends NoRollbackFlow {
    @Autowired
    private PluginRegistry pluginRgty;

    @Override
    public void run(FlowTrigger trigger, Map data) {
        final VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
        VmNicInventory nic = spec.getDestNics().get(0);

        Iterator<ReleaseNetworkServiceOnDetachingNicExtensionPoint> it = pluginRgty.getExtensionList(ReleaseNetworkServiceOnDetachingNicExtensionPoint.class).iterator();

        releaseResource(it, spec, nic, trigger);
    }

    private void releaseResource(final Iterator<ReleaseNetworkServiceOnDetachingNicExtensionPoint> it, final VmInstanceSpec spec, final VmNicInventory nic, final FlowTrigger trigger) {
        if (!it.hasNext()) {
            trigger.next();
            return;
        }

        ReleaseNetworkServiceOnDetachingNicExtensionPoint ext = it.next();
        ext.releaseResourceOnDetachingNic(spec, nic, new NoErrorCompletion(trigger) {
            @Override
            public void done() {
                releaseResource(it, spec, nic, trigger);
            }
        });
    }
}
