package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.header.Component;

import java.util.HashSet;
import java.util.Set;

public class VmTracerHelperImpl implements VmTracerHelper, Component {
    @Autowired
    private PluginRegistry pluginRgty;

    private Set<String> unsupportedVmInstanceTypeStringSet = new HashSet<>();

    @Override
    public boolean start() {
        for (CollectVmTracerUnsupportedVmTypeExtensionPoint ext : pluginRgty.getExtensionList(CollectVmTracerUnsupportedVmTypeExtensionPoint.class)) {
            unsupportedVmInstanceTypeStringSet.add(ext.getVmTracerUnsupportedVmTypeString());
        }

        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public Set<String> getVmTracerUnsupportedVmInstanceTypeSet() {
        return unsupportedVmInstanceTypeStringSet;
    }
}
