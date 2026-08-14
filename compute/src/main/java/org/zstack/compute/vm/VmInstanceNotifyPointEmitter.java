package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.componentloader.PluginExtension;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.thread.AsyncThread;
import org.zstack.header.Component;
import org.zstack.header.host.HypervisorType;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmInstanceStateChangeNotifyPoint;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.ForEachFunction;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VmInstanceNotifyPointEmitter implements Component {
    private static final CLogger logger = Utils.getLogger(VmInstanceNotifyPointEmitter.class);
    
    @Autowired
    private PluginRegistry pluginRgty;
    
    private Map<HypervisorType, List<VmInstanceStateChangeNotifyPoint>> stateChangePoints = new HashMap<HypervisorType, List<VmInstanceStateChangeNotifyPoint>>();
    
    private void populateNotifyPoints() {
        for (VmInstanceStateChangeNotifyPoint ext : pluginRgty.getExtensionList(VmInstanceStateChangeNotifyPoint.class)) {
            List<VmInstanceStateChangeNotifyPoint> exts = stateChangePoints.get(ext.getSupportedHypervisorTypeForVmInstanceStateChangeNotifyPoint());
            if (exts == null) {
                exts = new ArrayList<VmInstanceStateChangeNotifyPoint>(1);
                stateChangePoints.put(ext.getSupportedHypervisorTypeForVmInstanceStateChangeNotifyPoint(), exts);
            }
            exts.add(ext);
        }
    }
    
    @AsyncThread
    public void notifyVmStateChange(final VmInstanceInventory vm, final VmInstanceState pre, final VmInstanceState curr) {
        // when state changing from Created to Starting, there is no hyperviosrType yet as vm has not decided which host to go
        if (vm.getHypervisorType() == null) {
            return;
        }

        List<VmInstanceStateChangeNotifyPoint> exts = stateChangePoints.get(HypervisorType.valueOf(vm.getHypervisorType()));
        if (exts == null) {
            return;
        }

        CollectionUtils.safeForEach(exts, new ForEachFunction<VmInstanceStateChangeNotifyPoint>() {
            @Override
            public void run(VmInstanceStateChangeNotifyPoint arg) {
                arg.notifyVmInstanceStateChange(vm, pre, curr);
            }
        });
    }
    
    @Override
    public boolean start() {
        populateNotifyPoints();
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

}
