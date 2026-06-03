package org.zstack.pki;

import org.apache.commons.lang.StringUtils;
import org.zstack.core.db.Q;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.HostVO_;
import org.zstack.compute.host.HostSystemTags;
import org.zstack.header.migration.MigrationTlsSanCollector;
import org.zstack.mevoco.MevocoSystemTags;
import org.zstack.utils.network.NetworkUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class MigrationTlsSanCollectorImpl implements MigrationTlsSanCollector {
    @Override
    public List<String> collectSanForHost(String hostUuid) {
        Set<String> san = new LinkedHashSet<>();

        HostVO host = Q.New(HostVO.class).eq(HostVO_.uuid, hostUuid).find();
        if (host == null) {
            return new ArrayList<>();
        }

        String managementIp = host.getManagementIp();
        if (StringUtils.isNotBlank(managementIp)) {
            san.add(managementIp);
        }

        // add migration network IP if cluster has a dedicated migration CIDR
        String migrateCidr = MevocoSystemTags.CLUSTER_MIGRATE_NETWORK_CIDR.getTokenByResourceUuid(
                host.getClusterUuid(), MevocoSystemTags.CLUSTER_MIGRATE_NETWORK_CIDR_TOKEN);
        if (StringUtils.isNotBlank(migrateCidr)) {
            List<String> ips = new ArrayList<>();
            ips.add(managementIp);
            String extraIps = HostSystemTags.EXTRA_IPS.getTokenByResourceUuid(hostUuid, HostSystemTags.EXTRA_IPS_TOKEN);
            if (StringUtils.isNotBlank(extraIps)) {
                ips.addAll(Arrays.asList(extraIps.split(",")));
            }
            List<String> cidrIps = NetworkUtils.filterIpv4sInCidr(ips, migrateCidr);
            if (!cidrIps.isEmpty()) {
                san.add(cidrIps.get(0));
            }
        }

        san.add(hostUuid);
        return new ArrayList<>(san);
    }
}
