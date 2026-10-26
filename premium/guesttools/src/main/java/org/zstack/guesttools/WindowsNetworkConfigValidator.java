package org.zstack.guesttools;

import org.zstack.utils.network.IPv6NetworkUtils;
import org.zstack.utils.network.NetworkUtils;

/** Guards configuration intent without suppressing legitimate empty Guest reports. */
final class WindowsNetworkConfigValidator {
    private WindowsNetworkConfigValidator() {
    }

    static boolean isMissingStaticIpv4(String recordedIp, String proto, String targetIp, String netmask) {
        // DHCP/APIPA, IPv6-only and genuinely unaddressed NICs retain their behavior.
        if (!NetworkUtils.PROTO_STATIC.equals(proto) || recordedIp == null ||
                !IPv6NetworkUtils.isValidIpv4(recordedIp) || NetworkUtils.isInternalAddress(recordedIp)) {
            return false;
        }
        return isEmpty(targetIp) || isEmpty(netmask);
    }

    private static boolean isEmpty(String value) {
        return value == null || value.isEmpty() || NetworkUtils.NETWORK_CFG_EMPTY.equals(value);
    }
}
