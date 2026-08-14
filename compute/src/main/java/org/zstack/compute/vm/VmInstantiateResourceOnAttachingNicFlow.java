package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowRollback;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.vm.*;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * Created by frank on 7/18/2015.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VmInstantiateResourceOnAttachingNicFlow implements Flow {
    @Autowired
    private PluginRegistry pluginRgty;

    @Override
    public void run(FlowTrigger trigger, Map data) {
        Iterator<InstantiateResourceOnAttachingNicExtensionPoint> it = pluginRgty.getExtensionList(InstantiateResourceOnAttachingNicExtensionPoint.class).iterator();
        VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
        L3NetworkInventory l3 = VmNicSpec.getL3NetworkInventoryOfSpec(spec.getL3Networks()).get(0);
        instantiateResource(it, spec, l3, trigger);
    }

    private void instantiateResource(final Iterator<InstantiateResourceOnAttachingNicExtensionPoint> it, final VmInstanceSpec spec, final L3NetworkInventory l3, final FlowTrigger trigger) {
        if (!it.hasNext()) {
            trigger.next();
            return;
        }

        InstantiateResourceOnAttachingNicExtensionPoint ext = it.next();
        ext.instantiateResourceOnAttachingNic(spec, l3, new Completion(trigger) {
            @Override
            public void success() {
                instantiateResource(it, spec, l3, trigger);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                trigger.fail(errorCode);
            }
        });
    }

    @Override
    public void rollback(FlowRollback trigger, Map data) {
        Iterator<InstantiateResourceOnAttachingNicExtensionPoint> it = pluginRgty.getExtensionList(InstantiateResourceOnAttachingNicExtensionPoint.class).iterator();
        VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
        L3NetworkInventory l3 = VmNicSpec.getL3NetworkInventoryOfSpec(spec.getL3Networks()).get(0);
        releaseResource(it, spec, l3, trigger);
    }

    private void releaseResource(final Iterator<InstantiateResourceOnAttachingNicExtensionPoint> it, final VmInstanceSpec spec, final L3NetworkInventory l3, final FlowRollback trigger) {
        if (!it.hasNext()) {
            trigger.rollback();
            return;
        }

        InstantiateResourceOnAttachingNicExtensionPoint ext = it.next();
        ext.releaseResourceOnAttachingNic(spec, l3, new NoErrorCompletion(trigger) {
            @Override
            public void done() {
                releaseResource(it, spec, l3, trigger);
            }
        });
    }
}
