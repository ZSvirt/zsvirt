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
import org.zstack.header.vm.PostVmInstantiateResourceExtensionPoint;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VmInstantiateResourcePostFlow implements Flow {
    private static final CLogger logger = Utils.getLogger(VmInstantiateResourcePostFlow.class);

    @Autowired
    private PluginRegistry pluginRgty;

    private final List<PostVmInstantiateResourceExtensionPoint> extensions = pluginRgty.getExtensionList(PostVmInstantiateResourceExtensionPoint.class);


    public void run(FlowTrigger trigger, Map data) {
        VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
        for (PostVmInstantiateResourceExtensionPoint ext : extensions) {
            ext.postBeforeInstantiateVmResource(spec);
        }

        runExtensions(extensions.iterator(), spec, trigger);
    }

    private void runExtensions(final Iterator<PostVmInstantiateResourceExtensionPoint> iterator, final VmInstanceSpec spec, final FlowTrigger trigger) {
        if (!iterator.hasNext()) {
            trigger.next();
            return;
        }

        PostVmInstantiateResourceExtensionPoint ext = iterator.next();
        logger.debug(String.format("run PostVmInstantiateResourceExtensionPoint[%s]", ext.getClass()));
        ext.postInstantiateVmResource(spec, new Completion(trigger) {
            @Override
            public void success() {
                runExtensions(iterator, spec, trigger);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                trigger.fail(errorCode);
            }
        });
    }

    @Override
    public void rollback(FlowRollback trigger, Map data) {
        VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
        rollbackExtensions(extensions.iterator(), spec, trigger);
    }

    private void rollbackExtensions(final Iterator<PostVmInstantiateResourceExtensionPoint> iterator, final VmInstanceSpec spec, final FlowRollback trigger) {
        if (!iterator.hasNext()) {
            trigger.rollback();
            return;
        }

        PostVmInstantiateResourceExtensionPoint ext = iterator.next();
        logger.debug(String.format("rollback PostVmInstantiateResourceExtensionPoint[%s]", ext.getClass()));
        ext.postReleaseVmResource(spec, new Completion(trigger) {
            @Override
            public void success() {
                rollbackExtensions(iterator, spec, trigger);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                logger.warn(errorCode.getReadableDetails());
                trigger.rollback();
            }
        });
    }
}
