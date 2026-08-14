package org.zstack.sns.platform.system;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.sns.*;

import java.util.List;

public class SNSSystemPlatformFactory implements SNSApplicationPlatformFactory {
    public SNSApplicationPlatformType type = new SNSApplicationPlatformType(SNSConstants.SYSTEM_PLATFORM);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private PluginRegistry pluginRgty;

    @Override
    public SNSApplicationPlatformVO createApplicationPlatform(SNSApplicationPlatformVO vo, APICreateSNSApplicationPlatformMsg msg) {
        return vo;
    }

    @Override
    public String getApplicationPlatformType() {
        return SNSConstants.SYSTEM_PLATFORM;
    }

    @Override
    public SNSApplicationPlatformInventory getSNSApplicationPlatformInventory(SNSApplicationPlatformVO vo) {
        return SNSApplicationPlatformInventory.valueOf(vo);
    }

    @Override
    public SNSApplicationPlatform getSNSApplicationPlatform(String uuid) {
        return new SNSApplicationPlatformBase(dbf.findByUuid(uuid, SNSApplicationPlatformVO.class));
    }

    public List<SNSApplicationSystemEndpoint> getSNSApplicationSystemEndpoints() {
        return pluginRgty.getExtensionList(SNSApplicationSystemEndpoint.class);
    }
}
