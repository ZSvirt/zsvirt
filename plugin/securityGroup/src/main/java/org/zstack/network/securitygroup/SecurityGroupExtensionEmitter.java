package org.zstack.network.securitygroup;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.componentloader.PluginExtension;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.header.Component;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.ForEachFunction;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.List;

public class SecurityGroupExtensionEmitter implements Component {
    private static CLogger logger = Utils.getLogger(SecurityGroupExtensionEmitter.class);
    
    @Autowired
    private PluginRegistry pluginRgty;
    
    private List<AddVmNicToSecurityGroupExtensionPoint> addNicExts = new ArrayList<AddVmNicToSecurityGroupExtensionPoint>();

    public boolean start() {
        addNicExts = pluginRgty.getExtensionList(AddVmNicToSecurityGroupExtensionPoint.class);
        return true;
    }

    public boolean stop() {
        return true;
    }
    
    public void preAddVmNicToSecurityGroup(SecurityGroupInventory sg, VmInstanceInventory vm, List<VmNicInventory> nics) throws SecurityGroupException {
        for (AddVmNicToSecurityGroupExtensionPoint extp : addNicExts) {
            extp.preAddVmNic(sg, vm, nics);
        }
    }
    
    public void beforeAddVmNicToSecurityGroup(final SecurityGroupInventory sg, final VmInstanceInventory vm, final List<VmNicInventory> nics) {
        CollectionUtils.safeForEach(addNicExts, new ForEachFunction<AddVmNicToSecurityGroupExtensionPoint>() {
            @Override
            public void run(AddVmNicToSecurityGroupExtensionPoint arg) {
                arg.beforeAddVmNic(sg, vm, nics);
            }
        });
    }
    
    public void afterAddVmNicToSecurityGroup(final SecurityGroupInventory sg, final VmInstanceInventory vm, final List<VmNicInventory> nics) {
        CollectionUtils.safeForEach(addNicExts, new ForEachFunction<AddVmNicToSecurityGroupExtensionPoint>() {
            @Override
            public void run(AddVmNicToSecurityGroupExtensionPoint arg) {
                arg.afterAddVmNic(sg, vm, nics);
            }
        });
    }
}
