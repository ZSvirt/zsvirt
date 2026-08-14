package org.zstack.storage.memorySnapshot;

import org.zstack.core.Platform;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.header.vm.devices.VmInstanceResourceMetadataVO;
import org.zstack.utils.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

public class ResourceMetadataBuilderFactory {
    public static List<VmInstanceResourceMetadataVO> getCurrentVmInstanceResourceMetadataVOs(String vmInstanceUuid) {
        if (vmInstanceUuid == null) {
            throw new IllegalArgumentException("vmInstanceUuid cannot be null");
        }

        List<VmInstanceResourceMetadataVO> vos = new ArrayList<>();
        PluginRegistry pluginRgty = Platform.getComponentLoader().getComponent(PluginRegistry.class);
        pluginRgty.getExtensionList(ResourceMetadataBuilder.class).forEach(e -> {
            List<VmInstanceResourceMetadataVO> voList = e.buildResourceMetadataVOs(vmInstanceUuid);
            if (!CollectionUtils.isEmpty(voList)) {
                vos.addAll(voList);
            }
        });

        return vos;
    }
}
