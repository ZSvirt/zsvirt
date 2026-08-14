package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.header.core.Completion;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowException;
import org.zstack.header.core.workflow.FlowRollback;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.vm.*;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VmInstantiateResourceForChangeImageFlow implements Flow {
    private static final CLogger logger = Utils.getLogger(VmInstantiateResourceForChangeImageFlow.class);

    @Autowired
    private PluginRegistry pluginRgty;

    private final List<ChangeVmImageExtensionPoint> extensions = pluginRgty.getExtensionList(ChangeVmImageExtensionPoint.class);


    private void runExtensions(final Iterator<ChangeVmImageExtensionPoint> it, final VmInstanceSpec spec, final FlowTrigger chain) {
        if (!it.hasNext()) {
            chain.next();
            return;
        }

        ChangeVmImageExtensionPoint extp = it.next();

        logger.debug(String.format("run VmInstantiateResourceExtensionPoint[%s]", extp.getClass()));
        extp.preInstantiateVmResource(spec, new Completion(chain) {
            @Override
            public void success() {
                runExtensions(it, spec, chain);
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
        for (ChangeVmImageExtensionPoint extp : extensions) {
            try {
                extp.preBeforeInstantiateVmResource(spec);
            } catch (VmInstantiateResourceException vie) {
                throw new FlowException(vie.getErrorCode());
            }
        }

        runExtensions(extensions.iterator(), spec, chain);
    }

    private void rollbackExtensions(final Iterator<ChangeVmImageExtensionPoint> it, final VmInstanceSpec spec, final FlowRollback chain) {
        if (!it.hasNext()) {
            chain.rollback();
            return;
        }

        ChangeVmImageExtensionPoint extp = it.next();
        logger.debug(String.format("rollback VmInstantiateResourceExtensionPoint[%s]", extp.getClass()));
        extp.preReleaseVmResource(spec, new Completion(chain) {
            @Override
            public void success() {
                rollbackExtensions(it, spec, chain);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                logger.warn(errorCode.getReadableDetails());
                rollbackExtensions(it, spec, chain);
            }
        });
    }

    @Override
    public void rollback(FlowRollback chain, Map data) {
        VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
        rollbackExtensions(extensions.iterator(), spec, chain);
    }
}
