package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowRollback;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.Completion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceDeletionPolicyManager;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.vm.VmReleaseResourceExtensionPoint;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VmReleaseResourceFlow implements Flow {
    private static final CLogger logger = Utils.getLogger(VmReleaseResourceFlow.class);
    
    @Autowired
    private PluginRegistry pluginRgty;
    
    private final List<VmReleaseResourceExtensionPoint> extensions = pluginRgty.getExtensionList(VmReleaseResourceExtensionPoint.class);


    private void fireExtensions(final Iterator<VmReleaseResourceExtensionPoint> it, final VmInstanceSpec spec, final Map<String, Object> ctx, final FlowTrigger chain) {
        if (!it.hasNext()) {
            chain.next();
            return;
        }

        VmReleaseResourceExtensionPoint ext = it.next();
        ext.releaseVmResource(spec, new Completion(chain) {
            @Override
            public void success() {
                fireExtensions(it, spec, ctx, chain);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                chain.fail(errorCode);
            }
        });
    }

    @Override
    public void run(FlowTrigger chain, Map data) {
        VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
        fireExtensions(extensions.iterator(), spec, data, chain);
    }

    @Override
    public void rollback(FlowRollback chain, Map data) {
        chain.rollback();
    }
}
