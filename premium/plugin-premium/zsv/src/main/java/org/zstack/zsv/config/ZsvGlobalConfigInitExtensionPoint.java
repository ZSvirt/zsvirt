package org.zstack.zsv.config;

import org.zstack.compute.vm.VmGlobalConfig;
import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigInitExtensionPoint;
import org.zstack.core.config.GlobalConfigVO;

import java.util.ArrayList;
import java.util.List;

public class ZsvGlobalConfigInitExtensionPoint implements GlobalConfigInitExtensionPoint {
    @Override
    public List<GlobalConfig> getGenerationGlobalConfig() {
        List<GlobalConfig> configs = new ArrayList<>();

        GlobalConfigVO vo = new GlobalConfigVO();
        vo.setCategory(VmGlobalConfig.VM_VIDEO_TYPE.getCategory());
        vo.setName(VmGlobalConfig.VM_VIDEO_TYPE.getName());
        vo.setDescription(VmGlobalConfig.VM_VIDEO_TYPE.getDescription());
        vo.setDefaultValue("vga");
        vo.setValue("vga");

        configs.add(GlobalConfig.valueOf(vo));
        return configs;
    }
}
