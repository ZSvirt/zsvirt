package org.zstack.compute.host;

import org.zstack.core.db.Q;
import org.zstack.header.backup.ManagementNodeRecoverExtensionPoint;
import org.zstack.header.backup.NonBackupInfo;
import org.zstack.header.host.HostConstant;
import org.zstack.header.host.HostVO;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class HostManagementNodeRecoverExtension implements ManagementNodeRecoverExtensionPoint {

    @Override
    public Map<String, List<NonBackupInfo>> getNonBackupInfos() {
        List<HostVO> hosts = Q.New(HostVO.class).list();
        return Collections.singletonMap(HostVO.class.getSimpleName(), hosts.stream().map(it -> {
            NonBackupInfo info = new NonBackupInfo();
            info.setName(it.getName());
            info.setAttributeName("managementIp");
            info.setOldValue(it.getManagementIp());
            info.setUuid(it.getUuid());
            info.setResourceDescription(String.format("belong to cluster[uuid:%s]", it.getClusterUuid()));
            return info;
        }).collect(Collectors.toList()));
    }

    @Override
    public String getServiceId() {
        return HostConstant.SERVICE_ID;
    }
}
