package org.zstack.kvm;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.header.Component;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.vm.HypervisorBasedVmConfigurationFactory;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.HashMap;
import java.util.Map;

public class KVMVmConfigurationFactory implements HypervisorBasedVmConfigurationFactory, Component {
    private static final CLogger logger = Utils.getLogger(KVMVmConfigurationFactory.class);

    @Autowired
    private PluginRegistry pluginRgty;

    private final Map<String, KVMSubTypeVmConfigurationFactory> kvmSubTypeVmConfigurationFactoryMap = new HashMap<>();

    @Override
    public String getHypervisorType() {
        return KVMHostFactory.hypervisorType.toString();
    }

    @Override
    public void createVmConfigurations(VmInstanceSpec spec) {
        KVMSubTypeVmConfigurationFactory factory = kvmSubTypeVmConfigurationFactoryMap.get(spec.getVmInventory().getType());
        if (factory == null) {
            logger.debug(String.format("no available KVMVmConfigurationFactory found for vm[uuid: %s, type: %s, hypervisor: %s]," +
                    " skip vm configuration", spec.getVmInventory().getUuid(), spec.getVmInventory().getType(),
                    spec.getDestHost().getHypervisorType()));
            return;
        }
        factory.createConfigurations(spec);
    }

    private void populateExtensions() {
        for (KVMSubTypeVmConfigurationFactory ext : pluginRgty.getExtensionList(KVMSubTypeVmConfigurationFactory.class)) {
            KVMSubTypeVmConfigurationFactory old = kvmSubTypeVmConfigurationFactoryMap.get(ext.getVmInstanceType());
            if (old != null) {
                throw new CloudRuntimeException(String.format("duplicate VmInstanceFactory[%s, %s] for vm of type[%s]",
                        old.getClass().getName(), ext.getClass().getName(), ext.getVmInstanceType()));
            }
            kvmSubTypeVmConfigurationFactoryMap.put(ext.getVmInstanceType(), ext);
        }
    }

    @Override
    public boolean start() {
        populateExtensions();
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }
}
