package org.zstack.kvm;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.compute.host.HostIpmiPowerExecutor;
import org.zstack.compute.host.HostSystemTags;
import org.zstack.core.cloudbus.EventCallback;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.Component;
import org.zstack.header.host.*;
import org.zstack.utils.network.NetworkUtils;

import java.util.Map;


/**
 * @Author : jingwang
 * @create 2023/4/13 2:25 PM
 */
public class KvmHostIpmiPowerExecutor extends HostIpmiPowerExecutor implements Component {
    @Autowired
    DatabaseFacade dbf;
    @Autowired
    EventFacade evtf;

    private void onHostStatusChanged() {
        evtf.onLocal(HostCanonicalEvents.HOST_STATUS_CHANGED_PATH, new EventCallback() {
            @Override
            protected void run(Map tokens, Object data) {
                HostCanonicalEvents.HostStatusChangedData d = (HostCanonicalEvents.HostStatusChangedData) data;
                if (!d.getNewStatus().equals(HostStatus.Connected.name())) {
                    return;
                }

                String hostUuid = d.getInventory().getUuid();
                String currentIpmiAddress = HostSystemTags.IPMI_ADDRESS.getTokenByResourceUuid(hostUuid, HostSystemTags.IPMI_ADDRESS_TOKEN);
                HostPowerStatus status = HostPowerStatus.POWER_ON;
                if (!NetworkUtils.isIpv4Address(currentIpmiAddress)) {
                    currentIpmiAddress = null;
                }

                HostVO host = dbf.findByUuid(hostUuid, HostVO.class);
                HostIpmiVO ipmi = host.getIpmi();
                if (isIpmiUnConfigured(ipmi)) {
                    status = HostPowerStatus.UN_CONFIGURED;
                }

                if (ipmi != null) {
                    ipmi.setIpmiAddress(currentIpmiAddress);
                    ipmi.setIpmiPowerStatus(status);
                    dbf.update(ipmi);
                } else {
                    final HostIpmiVO hostIpmiVO = new HostIpmiVO();
                    hostIpmiVO.setUuid(hostUuid);
                    hostIpmiVO.setIpmiAddress(currentIpmiAddress);
                    hostIpmiVO.setIpmiPort(KVMConstant.IPMI_DEFAULT_PORT);
                    hostIpmiVO.setIpmiPowerStatus(status);
                    dbf.persist(hostIpmiVO);
                }
            }
        });
    }

    @Override
    public boolean start() {
        onHostStatusChanged();
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }
}
