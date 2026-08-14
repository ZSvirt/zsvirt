package org.zstack.appliancevm.kvm;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.appliancevm.ApplianceVmFactory;
import org.zstack.appliancevm.ApplianceVmVO;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.Component;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.kvm.KVMSubTypeVmConfigurationFactory;

import java.util.HashMap;
import java.util.Map;

public class KvmApplianceVmFactory implements KVMSubTypeVmConfigurationFactory, Component {
    private final Map<String, KvmApplianceVmSubTypeFactory> kvmApplianceVmSubTypeFactoryMap = new HashMap<>();

    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private DatabaseFacade dbf;

    @Override
    public String getVmInstanceType() {
        return ApplianceVmFactory.type.toString();
    }

    @Override
    public void createConfigurations(VmInstanceSpec spec) {
        ApplianceVmVO self = dbf.findByUuid(spec.getVmInventory().getUuid(), ApplianceVmVO.class);
        KvmApplianceVmSubTypeFactory kvmApplianceVmSubTypeFactory = kvmApplianceVmSubTypeFactoryMap.get(self.getApplianceVmType().toString());
        kvmApplianceVmSubTypeFactory.createHypervisorBasedConfigurations(spec);
    }

    private void populateExtensions() {
        for (KvmApplianceVmSubTypeFactory ext : pluginRgty.getExtensionList(KvmApplianceVmSubTypeFactory.class)) {
            KvmApplianceVmSubTypeFactory old = kvmApplianceVmSubTypeFactoryMap.get(ext.getApplianceVmType().toString());
            if (old != null) {
                throw new CloudRuntimeException(String.format("duplicate VmInstanceFactory[%s, %s] for vm of type[%s]",
                        old.getClass().getName(), ext.getClass().getName(), ext.getApplianceVmType()));
            }
            kvmApplianceVmSubTypeFactoryMap.put(ext.getApplianceVmType().toString(), ext);
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
