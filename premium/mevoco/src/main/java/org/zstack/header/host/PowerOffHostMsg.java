package org.zstack.header.host;

import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.Platform;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.header.cluster.PowerOffHardwareMsg;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by MaJin on 2019/6/28.
 */
public class PowerOffHostMsg extends PowerOffHardwareMsg implements HostMessage {
    private HostInventory host;

    private boolean isPowerOffManagementNode;
    private boolean isPowerOffOurselves;

    @Override
    public List getInventories() {
        return Collections.singletonList(host);
    }

    @Override
    public List<String> getUuids() {
        return host == null ? Collections.emptyList() : Collections.singletonList(host.getUuid());
    }

    public HostInventory getHost() {
        return host;
    }

    public void setHost(HostInventory host) {
        this.host = host;
        if (host == null) {
            isPowerOffManagementNode = false;
            isPowerOffOurselves = false;
        } else {
            isPowerOffManagementNode = (Long) SQL.New("select count(h.uuid) from HostVO h, ManagementNodeVO m" +
                    " where m.hostName = h.managementIp and h.uuid = :huuid", Long.class)
                    .param("huuid", host.getUuid()).find() > 0;
            isPowerOffOurselves = Q.New(HostVO.class).eq(HostVO_.managementIp, Platform.getManagementServerIp())
                    .eq(HostVO_.uuid, host.getUuid()).isExists();
        }
    }

    @Override
    public boolean powerOffManagementNode() {
        return isPowerOffManagementNode;
    }

    @Override
    public boolean powerOffOurself() {
        return isPowerOffOurselves;
    }

    @Override
    public String getHostUuid() {
        return host == null ? null : host.getUuid();
    }
}
