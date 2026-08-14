package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.config.GlobalConfigException;
import org.zstack.header.core.Completion;
import org.zstack.header.vm.*;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import static org.zstack.core.Platform.operr;

public class InitializeResourceConfigExtensionPoint implements PreVmInstantiateResourceExtensionPoint {
    private static final CLogger logger = Utils.getLogger(InitializeResourceConfigExtensionPoint.class);

    @Autowired
    private VmFactoryManager vmFactoryManager;

    @Override
    public void preBeforeInstantiateVmResource(VmInstanceSpec spec) throws VmInstantiateResourceException {
        // pass
    }

    /**
     * vm is almost allocated all resources including host so
     * use vm's type and hypervisor to initialize its resource
     * configs
     * @param spec allocated vm spec before creation
     * @param completion callback
     */
    @Override
    public void preInstantiateVmResource(VmInstanceSpec spec, Completion completion) {
        // factory only requested by new create vm
        if (spec.getCurrentVmOperation() != VmInstanceConstant.VmOperation.NewCreate) {
            completion.success();
            return;
        }

        // if no dest host specified, means this vm do not have available
        // hypervisor, record and complete with success
        if (spec.getDestHost() == null) {
            logger.debug(String.format("create vm[uuid: %s] but no host found, skip create configuration", spec.getVmInventory().getUuid()));
            completion.success();
            return;
        }

        HypervisorBasedVmConfigurationFactory vicf = vmFactoryManager.getVmInstanceConfigurationFactory(spec.getDestHost().getHypervisorType());
        if (vicf == null) {
            logger.debug(String.format("no available VmInstanceConfigurationFactory found for vm[uuid: %s, hypervisor: %s]," +
                    " skip vm configuration", spec.getVmInventory().getUuid(), spec.getDestHost().getHypervisorType()));
            completion.success();
            return;
        }

        try {
            vicf.createVmConfigurations(spec);
        } catch (GlobalConfigException e) {
            logger.warn(String.format("create vm[uuid: %s] configuration failed, %s", spec.getVmInventory().getUuid(), e.getMessage()), e);
            completion.fail(operr(e.getMessage()));
            return;
        }

        completion.success();
    }

    @Override
    public void preReleaseVmResource(VmInstanceSpec spec, Completion completion) {
        completion.success();
    }
}
