package org.zstack.storage.cbt;

import org.zstack.compute.host.HostSystemTags;
import org.zstack.header.host.HostInventory;
import org.zstack.mevoco.MevocoSystemTags;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.NetworkUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

final class CbtBackupNetworkUtils {
    private static final CLogger logger = Utils.getLogger(CbtBackupNetworkUtils.class);

    private CbtBackupNetworkUtils() {
    }

    static String getBackupAddress(HostInventory host) {
        String cidr = MevocoSystemTags.CLUSTER_CBT_BACKUP_NETWORK_CIDR.getTokenByResourceUuid(
                host.getClusterUuid(), MevocoSystemTags.CLUSTER_CBT_BACKUP_NETWORK_CIDR_TOKEN);
        if (cidr == null) {
            return host.getManagementIp();
        }

        String extraIps = HostSystemTags.EXTRA_IPS.getTokenByResourceUuid(host.getUuid(), HostSystemTags.EXTRA_IPS_TOKEN);
        List<String> ips = new ArrayList<>();
        ips.add(host.getManagementIp());
        Optional.ofNullable(extraIps).ifPresent(it -> ips.addAll(Arrays.asList(it.split(","))));
        ips.removeIf(ip -> !NetworkUtils.isIpv4Address(ip));
        List<String> cidrIps = NetworkUtils.filterIpv4sInCidr(ips, cidr);
        if (!cidrIps.isEmpty()) {
            return cidrIps.get(0);
        }

        logger.warn(String.format("host[uuid:%s] has no IP in CBT backup cidr[%s], falling back to management IP[%s]",
                host.getUuid(), cidr, host.getManagementIp()));
        return host.getManagementIp();
    }
}
