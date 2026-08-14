package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.header.Component;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.vm.VmInstanceBaseExtensionFactory;
import org.zstack.header.vm.HypervisorBasedVmConfigurationFactory;
import org.zstack.header.vm.VmInstanceFactory;
import org.zstack.header.vm.VmInstanceNicFactory;

import java.util.HashMap;
import java.util.Map;

public class VmFactoryManagerImpl implements VmFactoryManager, Component {
    @Autowired
    private PluginRegistry pluginRgty;

    private final Map<String, VmInstanceFactory> vmInstanceFactories = new HashMap<>();
    private final Map<Class, VmInstanceBaseExtensionFactory> vmInstanceBaseExtensionFactories = new HashMap<>();
    private final Map<String, VmInstanceNicFactory> vmInstanceNicFactories = new HashMap<>();
    private final Map<String, VmNicQosConfigBackend> vmNicQosConfigMap = new HashMap<>();
    private final Map<String, VmDnsBackend> vmDnsBackendMap = new HashMap<>();
    private final Map<String, HypervisorBasedVmConfigurationFactory> vmInstanceConfigurationFactoryMap = new HashMap<>();

    @Override
    public VmInstanceFactory getVmInstanceFactory(String vmInstanceType) {
        return vmInstanceFactories.get(vmInstanceType);
    }

    @Override
    public VmInstanceBaseExtensionFactory getVmInstanceBaseExtensionFactory(Class vmInstanceClass) {
        return vmInstanceBaseExtensionFactories.get(vmInstanceClass);
    }

    @Override
    public VmInstanceNicFactory getVmInstanceNicFactory(String vmNicType) {
        return vmInstanceNicFactories.get(vmNicType);
    }

    @Override
    public VmNicQosConfigBackend getVmNicQosConfigBackend(String vmInstanceType) {
        return vmNicQosConfigMap.get(vmInstanceType);
    }

    @Override
    public VmDnsBackend getVmDnsBackend(String vmInstanceType) {
        return vmDnsBackendMap.get(vmInstanceType);
    }

    @Override
    public HypervisorBasedVmConfigurationFactory getVmInstanceConfigurationFactory(String hypervisorType) {
        return vmInstanceConfigurationFactoryMap.get(hypervisorType);
    }

    private void populateExtensions() {
        for (VmInstanceFactory ext : pluginRgty.getExtensionList(VmInstanceFactory.class)) {
            VmInstanceFactory old = vmInstanceFactories.get(ext.getType().toString());
            if (old != null) {
                throw new CloudRuntimeException(String.format("duplicate VmInstanceFactory[%s, %s] for type[%s]",
                        old.getClass().getName(), ext.getClass().getName(), ext.getType()));
            }
            vmInstanceFactories.put(ext.getType().toString(), ext);
        }

        for (VmInstanceBaseExtensionFactory ext : pluginRgty.getExtensionList(VmInstanceBaseExtensionFactory.class)) {
            for (Class clz : ext.getMessageClasses()) {
                VmInstanceBaseExtensionFactory old = vmInstanceBaseExtensionFactories.get(clz);
                if (old != null) {
                    throw new CloudRuntimeException(String.format("duplicate VmInstanceBaseExtensionFactory[%s, %s] for the" +
                            " message[%s]", old.getClass(), ext.getClass(), clz));
                }

                vmInstanceBaseExtensionFactories.put(clz, ext);
            }
        }

        for (VmInstanceNicFactory ext : pluginRgty.getExtensionList(VmInstanceNicFactory.class)) {
            VmInstanceNicFactory old = vmInstanceNicFactories.get(ext.getType().toString());
            if (old != null) {
                throw new CloudRuntimeException(String.format("duplicate VmInstanceNicFactory[%s, %s] for type[%s]",
                        old.getClass().getName(), ext.getClass().getName(), ext.getType()));
            }
            vmInstanceNicFactories.put(ext.getType().toString(), ext);
        }

        for (VmNicQosConfigBackend ext : pluginRgty.getExtensionList(VmNicQosConfigBackend.class)) {
            VmNicQosConfigBackend old = vmNicQosConfigMap.get(ext.getVmInstanceType());
            if (old != null) {
                throw new CloudRuntimeException(String.format("can not add VmNicQosConfigBackend, because duplicate VmNicQosConfigBackend [%s, %s] for type[%s]",
                        old.getClass().getName(), ext.getClass().getName(), ext.getVmInstanceType()));
            }
            vmNicQosConfigMap.put(ext.getVmInstanceType(), ext);
        }

        for (VmDnsBackend ext : pluginRgty.getExtensionList(VmDnsBackend.class)) {
            VmDnsBackend old = vmDnsBackendMap.get(ext.getVmInstanceType());
            if (old != null) {
                throw new CloudRuntimeException(String.format("can not add VmDnsBackend, because duplicate VmDnsBackend [%s, %s] for type[%s]",
                        old.getClass().getName(), ext.getClass().getName(), ext.getVmInstanceType()));
            }
            vmDnsBackendMap.put(ext.getVmInstanceType(), ext);
        }

        for (HypervisorBasedVmConfigurationFactory ext : pluginRgty.getExtensionList(HypervisorBasedVmConfigurationFactory.class)) {
            HypervisorBasedVmConfigurationFactory old = vmInstanceConfigurationFactoryMap.get(ext.getHypervisorType());
            if (old != null) {
                throw new CloudRuntimeException(String.format("duplicate VmInstanceFactory[%s, %s] for vm on hypervisor[%s]",
                        old.getClass().getName(), ext.getClass().getName(), ext.getHypervisorType()));
            }
            vmInstanceConfigurationFactoryMap.put(ext.getHypervisorType(), ext);
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
