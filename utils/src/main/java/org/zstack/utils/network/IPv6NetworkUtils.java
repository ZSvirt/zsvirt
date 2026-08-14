package org.zstack.utils.network;

import com.googlecode.ipv6.IPv6Address;
import com.googlecode.ipv6.IPv6AddressRange;
import com.googlecode.ipv6.IPv6Network;
import com.googlecode.ipv6.IPv6NetworkMask;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.math.BigInteger;
import java.util.Arrays;

public class IPv6NetworkUtils {
    private final static CLogger logger = Utils.getLogger(IPv6NetworkUtils.class);

    // IPv4 地址正则表达式
    private static String ipv4Regex = "^(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";
    // MAC 地址正则表达式
    private static String macRegex = "^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$";

    private static boolean isConsecutiveRange(BigInteger[] allocatedIps) {
        BigInteger first = allocatedIps[0];
        BigInteger last = allocatedIps[allocatedIps.length - 1];
        return first.add(new BigInteger(String.valueOf(allocatedIps.length - 1))).compareTo(last) == 0;
        //return allocatedIps[allocatedIps.length - 1] - allocatedIps[0] + 1 == allocatedIps.length;
    }

    private static BigInteger findFirstHoleByDichotomy(BigInteger[] allocatedIps) {
        BigInteger first = allocatedIps[0];
        if (isConsecutiveRange(allocatedIps)) {
            String err = "You can not ask me to find a hole in consecutive range!!! ";
            assert false : err;
        }

        if (allocatedIps.length == 2) {
            return first.add(BigInteger.ONE);
        }

        int mIndex = allocatedIps.length / 2;
        BigInteger[] part1 = Arrays.copyOfRange(allocatedIps, 0, mIndex);
        BigInteger[] part2 = Arrays.copyOfRange(allocatedIps, mIndex, allocatedIps.length);
        if (part1.length == 1) {
            if (isConsecutiveRange(part2)) {
                /* For special case there are only three items like [1, 3, 4]*/
                return part1[0].add(BigInteger.ONE);
            } else {
                /* For special case there are only three items like [1, 5, 9] that are all inconsecutive */
                BigInteger[] tmp = new BigInteger[] { part1[0], part2[0] };
                if (!isConsecutiveRange(tmp)) {
                    return part1[0].add(BigInteger.ONE);
                }
            }
        }

        /*For special case that hole is in the middle of array. for example, [1, 2, 4, 5]*/
        if (isConsecutiveRange(part1) && isConsecutiveRange(part2)) {
            return part1[part1.length-1].add(BigInteger.ONE);
        }

        if (!isConsecutiveRange(part1)) {
            return findFirstHoleByDichotomy(part1);
        } else if (part2[0].compareTo(part1[part1.length-1].add(BigInteger.ONE)) > 0) {
            return part1[part1.length-1].add(BigInteger.ONE);
        } else {
            return findFirstHoleByDichotomy(part2);
        }
    }

    public static String IPv6AddressToString(BigInteger ip) {
        return IPv6Address.fromBigInteger(ip).toString();
    }

    // The allocatedIps must be sorted!
    public static BigInteger findFirstAvailableIpv6Address(BigInteger startIp, BigInteger endIp, BigInteger[] allocatedIps) {
        BigInteger ret = null;
        if (startIp.compareTo(endIp) > 0) {
            throw new IllegalArgumentException(String.format("[%s, %s] is an invalid ip range, end ip must be greater than start ip", IPv6AddressToString(startIp), IPv6AddressToString(endIp)));
        }
        if (startIp.equals(endIp) && allocatedIps.length == 0 ) {
            return startIp;
        }
        if (allocatedIps.length == 0) {
            return startIp;
        }

        BigInteger lastAllocatedIp = allocatedIps[allocatedIps.length-1];
        BigInteger firstAllocatedIp = allocatedIps[0];
        if (firstAllocatedIp.compareTo(startIp) < 0 || lastAllocatedIp.compareTo(endIp) > 0) {
            throw new IllegalArgumentException(String.format("[%s, %s] is an invalid allocated ip range, it's not a sub range of ip range[%s, %s]", IPv6AddressToString(firstAllocatedIp), IPv6AddressToString(lastAllocatedIp), IPv6AddressToString(startIp), IPv6AddressToString(endIp)));
        }

        /* ipv4 version: allocatedIps.length == endIp - startIp + 1 */
        if (startIp.add(new BigInteger(String.valueOf(allocatedIps.length))).compareTo(endIp) > 0) {
            /* The ip range is fully occupied*/
            return null;
        }

        if (firstAllocatedIp.compareTo(startIp) > 0) {
            /* The allocatedIps doesn't begin with startIp, then startIp is first available one*/
            return startIp;
        }

        if (isConsecutiveRange(allocatedIps)) {
            /* the allocated ip range is consecutive, allocate the first one out of allocated ip range */
            ret = lastAllocatedIp.add(BigInteger.ONE);
            assert ret.compareTo(endIp) <= 0;
            return ret;
        }

        /* Now the allocated ip range is inconsecutive, we are going to find out the first *hole* in it */
        return findFirstHoleByDichotomy(allocatedIps);
    }

    /* convert 48 bit mac to ipv6 address
     * 1. invert the universal/local bit of mac, universal/local bit is the 6 bit(0 ~7)
     * 2. insert ff:fe after 3rd byte
     * 3. attach converted to mac address to network cidr
     * */
    public static String getIPv6AddresFromMac(String networkCidr, String mac) {
        IPv6Network network = IPv6Network.fromString(networkCidr);
        if (network.getNetmask().asPrefixLength() > 64) {
            return null;
        }

        int idx = networkCidr.indexOf("::");
        String[] macs = mac.split(":");
        String ip = networkCidr.substring(0, idx) + "::" + Integer.toHexString(Integer.parseInt(macs[0], 16) ^ 2) +
                macs[1] + ":" + macs[2] + "ff:fe" + macs[3] +":" + macs[4] + macs[5];
        return getIpv6AddressCanonicalString(ip);
    }

    public static boolean isIpv6Address(String ip) {
        try {
            IPv6Address.fromString(ip);
            return true;
        } catch (Exception e){
            return false;
        }
    }

    public static boolean isIpv6UnicastAddress(String ip) {
        try {
            IPv6Address address = IPv6Address.fromString(ip);
            if (address.isMulticast() || address.isLinkLocal() || address.isSiteLocal()) {
                return false;
            } else {
                return true;
            }
        } catch (Exception e){
            return false;
        }
    }

    public static boolean isIpv6MulticastAddress(String ip) {
        try {
            IPv6Address address = IPv6Address.fromString(ip);
            if (address.isMulticast()) {
                return true;
            } else {
                return false;
            }
        } catch (Exception e){
            return false;
        }
    }

    public static boolean isValidUnicastIpv6Range(String startIp, String endIp, String gatewayIp, int prefixLen) {
        try {
            IPv6Address start = IPv6Address.fromString(startIp);
            IPv6Address end = IPv6Address.fromString(endIp);
            if (end.compareTo(start) < 0) {
                return false;
            }

            IPv6Address gateway = IPv6Address.fromString(gatewayIp);
            /* gateway can not equals to the first or last */
            if (gateway.compareTo(start) == 0 || gateway.compareTo(end) == 0) {
                return false;
            }

            IPv6Network network = IPv6Network.fromAddressAndMask(start, IPv6NetworkMask.fromPrefixLength(prefixLen));
            if (!network.contains(end)) {
                return false;
            }
            if (!network.contains(gateway)) {
                return false;
            }
            /* start can not be first of the cidr */
            if (network.getFirst().compareTo(start) == 0) {
                return false;
            }
            /* end can not be last of the cidr */
            if (network.getLast().compareTo(end) < 0) {
                return false;
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isIpv6RangeOverlap(String startIp1, String endIp1, String startIp2, String endIp2) {
        try {
            IPv6Address s1 = IPv6Address.fromString(startIp1);
            IPv6Address e1 = IPv6Address.fromString(endIp1);
            IPv6Address s2 = IPv6Address.fromString(startIp2);
            IPv6Address e2 = IPv6Address.fromString(endIp2);

            IPv6AddressRange range1 = IPv6AddressRange.fromFirstAndLast(s1, e1);
            IPv6AddressRange range2 = IPv6AddressRange.fromFirstAndLast(s2, e2);
            return range1.overlaps(range2);
        } catch (Exception e) {
            return true;
        }
    }

    public static boolean isValidUnicastNetworkCidr(String networkCidr) {
        try {
            IPv6Network network = IPv6Network.fromString(networkCidr);
            if ((network.getNetmask().asPrefixLength() > IPv6Constants.IPV6_PREFIX_LEN_MAX)
                    || (network.getNetmask().asPrefixLength() < IPv6Constants.IPV6_PREFIX_LEN_MIN)) {
                return false;
            }

            return !(network.getFirst().isSiteLocal() || network.getFirst().isLinkLocal() || network.getFirst().isMulticast());
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isIpv6InRange(String ip, String startIp, String endIp) {
        IPv6Address start = IPv6Address.fromString(startIp);
        IPv6Address end = IPv6Address.fromString(endIp);
        IPv6Address address = IPv6Address.fromString(ip);

        IPv6AddressRange range = IPv6AddressRange.fromFirstAndLast(start, end);
        return range.contains(address);
    }

    public static boolean isIpv6InCidrRange(String ip, String networkCidr) {
        IPv6Network network = IPv6Network.fromString(networkCidr);
        IPv6Address address = IPv6Address.fromString(ip);
        return network.contains(address);
    }

    public static boolean isIpv6CidrEqual( String cidr1, String cidr2) {
        IPv6Network network = IPv6Network.fromString(cidr1);
        return network.equals(IPv6Network.fromString(cidr2));
    }

    public static long getIpv6RangeSize(String startIp, String endIp) {
        IPv6Address start = IPv6Address.fromString(startIp);
        IPv6Address end = IPv6Address.fromString(endIp);
        IPv6AddressRange range = IPv6AddressRange.fromFirstAndLast(start, end);
        if (range.size().compareTo(IPv6Constants.IntegerMax) >= 0) {
            return Integer.MAX_VALUE;
        } else {
            return range.size().longValue();
        }
    }

    public static boolean isIpv6RangeFull(String startIp, String endIp, long used) {
        IPv6Address start = IPv6Address.fromString(startIp);
        IPv6Address end = IPv6Address.fromString(endIp);
        IPv6AddressRange range = IPv6AddressRange.fromFirstAndLast(start, end);
        
        return range.size().compareTo(new BigInteger(String.valueOf(used))) <= 0;
    }

    public static BigInteger getBigIntegerFromString(String ip) {
        return IPv6Address.fromString(ip).toBigInteger();
    }

    public static String getFormalCidrOfNetworkCidr(String cidr) {
        IPv6Network network = IPv6Network.fromString(cidr);
        return network.toString();
    }

    private static final String[] IP6MASK = new String[] {
            "8000", "c000", "e000", "f000",
            "f800", "fc00", "fe00", "ff00",
            "ff80", "ffc0", "ffe0", "fff0",
            "fff8", "fffc", "fffe", "ffff"
    };

    public static String getFormalNetmaskOfNetworkCidr(String cidr) {
        Integer prefix = IPv6Network.fromString(cidr).getNetmask().asPrefixLength();
        return getFormalNetmaskOfNetworkCidr(prefix);
    }

    public static String getFormalNetmaskOfNetworkCidr(Integer prefix) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            int left = prefix - i * 16;
            if (left > 16) {
                sb.append(IP6MASK[15]);
            } else if (left <= 0) {
                sb.append("0");
            } else {
                sb.append(IP6MASK[left - 1]);
            }
            if (i != 7) {
                sb.append(":");
            }
        }

        return sb.toString();
    }

    public static String getStartIpOfNetworkCidr(String cidr) {
        IPv6Network network = IPv6Network.fromString(cidr);
        if (network.getNetmask().asPrefixLength() < 127) {
            return network.getFirst().add(2).toString();
        } else {
            return network.getFirst().toString();
        }
    }

    public static String getEndIpOfNetworkCidr(String cidr) {
        IPv6Network network = IPv6Network.fromString(cidr);
        return network.getLast().toString();
    }

    public static String getGatewayOfNetworkCidr(String cidr) {
        IPv6Network network = IPv6Network.fromString(cidr);
        if (network.getNetmask().asPrefixLength() <= 127) {
            return network.getFirst().add(1).toString();
        } else {
            return network.getFirst().toString();
        }
    }

    public static int getPrefixLenOfNetworkCidr(String cidr) {
        IPv6Network network = IPv6Network.fromString(cidr);
        return network.getNetmask().asPrefixLength();
    }

    public static String getNetworkCidrOfIpRange(String startIp, int prefixLen) {
        try {
            IPv6Address start = IPv6Address.fromString(startIp);
            IPv6Network network = IPv6Network.fromAddressAndMask(start, IPv6NetworkMask.fromPrefixLength(prefixLen));
            return network.toString();
        } catch (Exception e) {
            return "";
        }
    }

    public static String getNetworkMaskOfIpRange(String startIp, int prefixLen) {
        try {
            IPv6Address start = IPv6Address.fromString(startIp);
            IPv6Network network = IPv6Network.fromAddressAndMask(start, IPv6NetworkMask.fromPrefixLength(prefixLen));
            return network.getNetmask().toString();
        } catch (Exception e) {
            return "";
        }
    }

    public static String getIpv6AddressCanonicalString(String ip) {
        IPv6Address ip6 = IPv6Address.fromString(ip);
        return ip6.toString();
    }

    public static BigInteger ipv6AddressToBigInteger(String ip) {
        return IPv6Address.fromString(ip).toBigInteger();
    }

    public static String ipv6AddressToString(BigInteger ip) {
        return IPv6Address.fromBigInteger(ip).toString();
    }


    public static String ipv6AddessToTagValue(String ip) {
        return ip.replace("::", "--");
    }

    public static String ipv6AddessToHostname(String ip) {
        return ip.replace("::", "--").replace(":", "-");
    }

    public static String ipv6TagValueToAddress(String tag) {
        if (tag == null){
            return null;
        }
        return tag.replace("--", "::");
    }

    public static String ipv6AddressToTagValue(String address) {
        if (address == null){
            return null;
        }
        return address.replace("::", "--");
    }


    public static boolean isValidGlobalIpv6(String ipv6) {
        if (ipv6 == null) {
            return false;
        }
        // Link-local addresses start with fe80
        if (ipv6.toLowerCase().startsWith("fe80")) {
            return false;
        }
        // Unique-local addresses start with fd
        if (ipv6.toLowerCase().startsWith("fd")) {
            return false;
        }
        return isValidIpv6(ipv6);
    }

    public static boolean isValidIpv6(String ipv6) {
        if (ipv6.split(NetworkUtils.DEFAULT_IPV6_PREFIX_SPLIT).length == 2) {
            return isIpv6Address(ipv6.split(NetworkUtils.DEFAULT_IPV6_PREFIX_SPLIT)[0]);
        } else {
            return isIpv6Address(ipv6);
        }
    }

    public static boolean isValidIpv4(String ipv4) {
        if (ipv4.split(NetworkUtils.DEFAULT_IPV4_PREFIX_SPLIT).length == 2) {
            return ipv4.split(NetworkUtils.DEFAULT_IPV4_PREFIX_SPLIT)[0].matches(IPv6NetworkUtils.ipv4Regex) &&
                    Integer.parseInt(ipv4.split(NetworkUtils.DEFAULT_IPV4_PREFIX_SPLIT)[1]) >= 1 &&
                    Integer.parseInt(ipv4.split(NetworkUtils.DEFAULT_IPV4_PREFIX_SPLIT)[1]) <= 32;
        } else {
            return ipv4.matches(IPv6NetworkUtils.ipv4Regex);
        }
    }

    public static boolean isValidMac(String mac) {
        return mac.matches(IPv6NetworkUtils.macRegex);
    }

    /**
     * Check if the IP starts with the Link-Local prefix "fe80".
     * Read vm ip to usedIp need avoid it
     */
    public static Boolean isLinkLocalAddress(String ipv6) {
        if (ipv6 == null || ipv6.isEmpty()) {
            return false;
        }
        return ipv6.toLowerCase().startsWith("fe80");
    }

    public static boolean isValidIpRange(String startIp, String endIp) {
        if (NetworkUtils.isIpv4Address(startIp) && NetworkUtils.isIpv4Address(endIp)) {
            long s = NetworkUtils.ipv4StringToLong(startIp);
            long e = NetworkUtils.ipv4StringToLong(endIp);
            return e >= s;
        }

        if (isValidIpv6(startIp) && isValidIpv6(endIp)) {
            BigInteger s = IPv6Address.fromString(startIp).toBigInteger();
            BigInteger e = IPv6Address.fromString(endIp).toBigInteger();
            return e.compareTo(s) >= 0;
        }

        return false;
    }

    public static int compareIpv6Address(String ip1, String ip2) {
        BigInteger s = IPv6Address.fromString(ip1).toBigInteger();
        BigInteger e = IPv6Address.fromString(ip2).toBigInteger();

        return e.compareTo(s);
    }

    public static boolean isFullCidr(String cidr) {
        return cidr.equals("::/0");
    }
}
