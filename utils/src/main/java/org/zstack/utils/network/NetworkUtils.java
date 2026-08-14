package org.zstack.utils.network;

import com.googlecode.ipv6.IPv6Address;
import com.googlecode.ipv6.IPv6Network;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.net.util.SubnetUtils;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.ShellResult;
import org.zstack.utils.ShellUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.data.Pair;
import org.zstack.utils.logging.CLogger;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.zstack.utils.network.IPv6NetworkUtils.*;


public class NetworkUtils {
    private static final CLogger logger = Utils.getLogger(NetworkUtils.class);

    private static final Map<String, Integer> validNetmasks = new HashMap<String, Integer>();

    private static final Random random = new Random();

    static {
        validNetmasks.put("255.255.255.255", 32);
        validNetmasks.put("255.255.255.254", 31);
        validNetmasks.put("255.255.255.252", 30);
        validNetmasks.put("255.255.255.248", 29);
        validNetmasks.put("255.255.255.240", 28);
        validNetmasks.put("255.255.255.224", 27);
        validNetmasks.put("255.255.255.192", 26);
        validNetmasks.put("255.255.255.128", 25);
        validNetmasks.put("255.255.255.0", 24);
        validNetmasks.put("255.255.254.0", 23);
        validNetmasks.put("255.255.252.0", 22);
        validNetmasks.put("255.255.248.0", 21);
        validNetmasks.put("255.255.240.0", 20);
        validNetmasks.put("255.255.224.0", 19);
        validNetmasks.put("255.255.192.0", 18);
        validNetmasks.put("255.255.128.0", 17);
        validNetmasks.put("255.255.0.0", 16);
        validNetmasks.put("255.254.0.0", 15);
        validNetmasks.put("255.252.0.0", 14);
        validNetmasks.put("255.248.0.0", 13);
        validNetmasks.put("255.240.0.0", 12);
        validNetmasks.put("255.224.0.0", 11);
        validNetmasks.put("255.192.0.0", 10);
        validNetmasks.put("255.128.0.0", 9);
        validNetmasks.put("255.0.0.0", 8);
        validNetmasks.put("254.0.0.0", 7);
        validNetmasks.put("252.0.0.0", 6);
        validNetmasks.put("248.0.0.0", 5);
        validNetmasks.put("240.0.0.0", 4);
        validNetmasks.put("224.0.0.0", 3);
        validNetmasks.put("192.0.0.0", 2);
        validNetmasks.put("128.0.0.0", 1);
        validNetmasks.put("0.0.0.0", 0);
    }

    public static boolean isHostname(String hostname) {
        if (hostname == null) {
            return false;
        }
        String PATTERN = "^(([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\\-]*[a-zA-Z0-9])\\.)*+([A-Za-z0-9]|[A-Za-z0-9][A-Za-z0-9\\-]*[A-Za-z0-9])$";
        Pattern pattern = Pattern.compile(PATTERN);
        Matcher matcher = pattern.matcher(hostname);
        return matcher.matches();
    }

    public static boolean isIpv4Address(String ip) {
        if (ip == null) {
            return false;
        }

        String PATTERN =
                "^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                        "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                        "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                        "([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";
        Pattern pattern = Pattern.compile(PATTERN);
        Matcher matcher = pattern.matcher(ip);
        return matcher.matches();
    }

    public static boolean isIpAddress(String ip) {
        boolean isIpAddress = false;
        if (NetworkUtils.isIpv4Address(ip)) {
            isIpAddress = true;
        } else {
            if (isIpv6Address(ip)) {
                isIpAddress = true;
            }
        }
        return isIpAddress;
    }

    public static boolean isNotIpAddress(String ip) {
        return !isIpAddress(ip);
    }

    public static boolean isNetmask(String netmask) {
        return validNetmasks.containsKey(netmask);
    }

    public static boolean isNetmaskExcept(String netmask, String except) {
        return validNetmasks.containsKey(netmask) && !netmask.equals(except);
    }

    public static boolean isCidr(String cidr) {
        try {
            IPv6Network.fromString(cidr);
            return true;
        } catch (Exception e) {
            Pattern pattern = Pattern.compile("^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])(\\/(\\d|[1-2]\\d|3[0-2]))$");
            Matcher matcher = pattern.matcher(cidr);
            return matcher.find();
        }
    }

    public static boolean isCidr(String cidr, int ipVersion) {
        if (ipVersion == IPv6Constants.IPv4) {
            Pattern pattern = Pattern.compile("^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])(\\/(\\d|[1-2]\\d|3[0-2]))$");
            Matcher matcher = pattern.matcher(cidr);
            return matcher.find();
        } else if (ipVersion == IPv6Constants.IPv6) {
            try {
                IPv6Network.fromString(cidr);
                return true;
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }

    public static long bytesToLong(byte[] bytes) {
        long value = 0;
        for (byte b : bytes) {
            value = (value << 8) + (b & 0xff);
        }
        return value;
    }

    public static byte[] ipStringToBytes(String ip) {
        if (isNotIpAddress(ip)) {
            throw new IllegalArgumentException(String.format("%s is not a valid ip address", ip));
        }

        try {
            InetAddress inetAddress = InetAddress.getByName(ip);
            return inetAddress.getAddress();
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException(String.format("%s is not a valid ip address", ip), e);
        }
    }

    public static long ipv4StringToLong(String ip) {
        validateIpv4(ip);

        try {
            InetAddress ia = InetAddress.getByName(ip);
            byte[] bytes = ia.getAddress();
            return bytesToLong(bytes);
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException(String.format("%s is not a valid ipv4 address", ip), e);
        }
    }

    public static boolean isIpv4InRange(String ip, String startIp, String endIp) {
        long ipl = ipv4StringToLong(ip);
        long startIpl = ipv4StringToLong(startIp);
        long endIpl = ipv4StringToLong(endIp);
        return (ipl <= endIpl && ipl >= startIpl);
    }

    public static String longToIpv4String(long ip) {
        byte[] bytes = ByteBuffer.allocate(4).putInt((int) ip).array();
        try {
            InetAddress ia = InetAddress.getByAddress(bytes);
            return ia.getHostAddress();
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException(String.format("%s cannot convert to a valid ipv4 address", ip), e);
        }
    }

    public static int ipRangeLength(String startIp, String endIp) {
        validateIpv4(startIp);
        validateIpv4(endIp);
        return (int) (ipv4StringToLong(endIp) - ipv4StringToLong(startIp) + 1);
    }

    private static void validateIpv4(String ip) {
        if (!isIpv4Address(ip)) {
            throw new IllegalArgumentException(String.format("%s is not a valid ipv4 address", ip));
        }
    }

    public static void validateIpRange(String startIp, String endIp) {
        validateIpv4(startIp);
        validateIpv4(endIp);
        long s = ipv4StringToLong(startIp);
        long e = ipv4StringToLong(endIp);
        if (s > e) {
            throw new IllegalArgumentException(String.format("[%s, %s] is not a valid ip range, end ip must be greater than start ip", startIp, endIp));
        }
    }

    public static boolean isValidIpv4Range(String startIp, String endIp) {
        validateIpv4(startIp);
        validateIpv4(endIp);
        long s = ipv4StringToLong(startIp);
        long e = ipv4StringToLong(endIp);
        return e >= s;
    }

    public static boolean isIpv4RangeOverlap(String startIp1, String endIp1, String startIp2, String endIp2) {
        validateIpRange(startIp1, endIp1);
        validateIpRange(startIp2, endIp2);

        long s1 = ipv4StringToLong(startIp1);
        long e1 = ipv4StringToLong(endIp1);
        long s2 = ipv4StringToLong(startIp2);
        long e2 = ipv4StringToLong(endIp2);

        return (s1 >= s2 && s1 <= e2) || (s1 <= s2 && s2 <= e1);
    }

    public static boolean isConsecutiveRange(List<Long> allocatedIps) {
        return allocatedIps.get(allocatedIps.size() - 1) - allocatedIps.get(0) == allocatedIps.size();
    }

    private static boolean isConsecutiveRange(Long[] allocatedIps) {
        return allocatedIps[allocatedIps.length - 1] - allocatedIps[0] + 1 == allocatedIps.length;
    }

    private static String[] longToIpv4String(Long[] ips) {
        String[] ret = new String[ips.length];
        for (int i = 0; i < ips.length; i++) {
            ret[i] = longToIpv4String(ips[i]);
        }
        return ret;
    }

    private static long findFirstHoleByDichotomy(Long[] allocatedIps) {
        if (isConsecutiveRange(allocatedIps)) {
            String[] ips = longToIpv4String(allocatedIps);
            List<String> dips = new ArrayList<String>(allocatedIps.length);
            Collections.addAll(dips, ips);
            String err = "You can not ask me to find a hole in consecutive range!!! " + dips;
            assert false : err;
        }

        if (allocatedIps.length == 2) {
            return allocatedIps[0] + 1;
        }

        int mIndex = allocatedIps.length / 2;
        Long[] part1 = Arrays.copyOfRange(allocatedIps, 0, mIndex);
        Long[] part2 = Arrays.copyOfRange(allocatedIps, mIndex, allocatedIps.length);
        if (part1.length == 1) {
            if (isConsecutiveRange(part2)) {
                /* For special case there are only three items like [1, 3, 4]*/
                return part1[0] + 1;
            } else {
                /* For special case there are only three items like [1, 5, 9] that are all inconsecutive */
                Long[] tmp = new Long[]{part1[0], part2[0]};
                if (!isConsecutiveRange(tmp)) {
                    return part1[0] + 1;
                }
            }
        }

        /*For special case that hole is in the middle of array. for example, [1, 2, 4, 5]*/
        if (isConsecutiveRange(part1) && isConsecutiveRange(part2)) {
            return part1[part1.length - 1] + 1;
        }

        if (!isConsecutiveRange(part1)) {
            return findFirstHoleByDichotomy(part1);
        } else if (part2[0] - part1[part1.length - 1] > 1) {
            return part1[part1.length - 1] + 1;
        } else {
            return findFirstHoleByDichotomy(part2);
        }
    }

    // The allocatedIps must be sorted!
    public static Long findFirstAvailableIpv4Address(Long startIp, Long endIp, Long[] allocatedIps) {
        Long ret = null;
        if (startIp > endIp) {
            throw new IllegalArgumentException(String.format("[%s, %s] is an invalid ip range, end ip must be greater than start ip", longToIpv4String(startIp), longToIpv4String(endIp)));
        }
        if (startIp.equals(endIp) && allocatedIps.length == 0) {
            return startIp;
        }
        if (allocatedIps.length == 0) {
            return startIp;
        }

        long lastAllocatedIp = allocatedIps[allocatedIps.length - 1];
        long firstAllocatedIp = allocatedIps[0];
        if (firstAllocatedIp < startIp || lastAllocatedIp > endIp) {
            throw new IllegalArgumentException(String.format("[%s, %s] is an invalid allocated ip range, it's not a sub range of ip range[%s, %s]", longToIpv4String(firstAllocatedIp), longToIpv4String(lastAllocatedIp), longToIpv4String(startIp), longToIpv4String(endIp)));
        }


        if (allocatedIps.length == endIp - startIp + 1) {
            /* The ip range is fully occupied*/
            return null;
        }

        if (firstAllocatedIp > startIp) {
            /* The allocatedIps doesn't begin with startIp, then startIp is first available one*/
            return startIp;
        }

        if (isConsecutiveRange(allocatedIps)) {
            /* the allocated ip range is consecutive, allocate the first one out of allocated ip range */
            ret = lastAllocatedIp + 1;
            assert ret <= endIp;
            return ret;
        }

        /* Now the allocated ip range is inconsecutive, we are going to find out the first *hole* in it */
        return findFirstHoleByDichotomy(allocatedIps);
    }

    public static String findFirstAvailableIpv4Address(String startIp, String endIp, Long[] allocatedIps) {
        Long ret = findFirstAvailableIpv4Address(ipv4StringToLong(startIp), ipv4StringToLong(endIp), allocatedIps);
        return ret == null ? null : longToIpv4String(ret);
    }

    public static String randomAllocateIpv4Address(Long startIp, Long endIp, List<Long> allocatedIps) {
        int total = (int) (endIp - startIp + 1);
        if (startIp.equals(endIp) && allocatedIps.size() == 0) {
            return longToIpv4String(startIp);
        }
        if (total == allocatedIps.size()) {
            return null;
        }

        BitSet full = new BitSet(total);
        for (long alloc : allocatedIps) {
            full.set((int) (alloc - startIp));
        }

        int next = random.nextInt(total);
        int a = full.nextClearBit(next);

        if (a >= total) {
            a = full.nextClearBit(0);
        }

        return longToIpv4String(a + startIp);
    }

    public static String randomAllocateIpv4Address(String startIp, String endIp, List<Long> allocatedIps) {
        return randomAllocateIpv4Address(ipv4StringToLong(startIp), ipv4StringToLong(endIp), allocatedIps);
    }

    public static int getTotalIpInRange(String startIp, String endIp) {
        if (isIpv4Address(startIp)) {
            validateIpRange(startIp, endIp);
            long s = ipv4StringToLong(startIp);
            long e = ipv4StringToLong(endIp);
            return (int) (e - s + 1);
        } else if (isIpv6Address(startIp)) {
            return (int) getIpv6RangeSize(startIp, endIp);
        } else {
            throw new IllegalArgumentException(String.format("%s is not a valid ipv4 address or valid ipv6 address", startIp));
        }
    }

    public static int getTotalIpInCidr(String cidr) {
        if (!isCidr(cidr)) {
            throw new IllegalArgumentException(String.format("%s is not a valid cidr", cidr));
        }
        SubnetUtils.SubnetInfo range = new SubnetUtils(cidr).getInfo();

        return getTotalIpInRange(range.getLowAddress(), range.getHighAddress());
    }


    public static String getIpAddressByName(String hostName) throws UnknownHostException {
        InetAddress ia = InetAddress.getByName(hostName);
        return ia.getHostAddress();
    }

    public static String generateMacWithDeviceId(short deviceId) {
        int seed = random.nextInt();
        String seedStr = Integer.toHexString(seed);
        if (seedStr.length() < 8) {
            String compensate = StringUtils.repeat("0", 8 - seedStr.length());
            seedStr = compensate + seedStr;
        }
        String octet2 = seedStr.substring(0, 2);
        String octet3 = seedStr.substring(2, 4);
        String octet4 = seedStr.substring(4, 6);
        String octet5 = seedStr.substring(6, 8);
        StringBuilder sb = new StringBuilder("fa").append(":");
        sb.append(octet2).append(":");
        sb.append(octet3).append(":");
        sb.append(octet4).append(":");
        sb.append(octet5).append(":");
        String deviceIdStr = Integer.toHexString(deviceId);
        if (deviceIdStr.length() < 2) {
            deviceIdStr = "0" + deviceIdStr;
        }
        sb.append(deviceIdStr);
        return sb.toString();
    }

    public static List<Pair<String, String>> findConsecutiveIpRange(Collection<String> ips) {
        List<Pair<String, String>> ret = new ArrayList<Pair<String, String>>();
        if (ips.isEmpty()) {
            return ret;
        }

        TreeSet<Long> orderIps = new TreeSet<Long>();
        for (String ip : ips) {
            orderIps.add(ipv4StringToLong(ip));
        }

        Long s = null;
        Long e = null;

        for (Long ip : orderIps) {
            if (s == null || e == null) {
                s = ip;
                e = ip;
            } else if (e.equals(ip - 1)) {
                e = ip;
            } else {
                Pair<String, String> range = new Pair<String, String>();
                range.first(longToIpv4String(s));
                range.second(longToIpv4String(e));
                ret.add(range);
                s = ip;
                e = ip;
            }
        }

        Pair<String, String> range = new Pair<String, String>();
        range.first(longToIpv4String(s));
        range.second(longToIpv4String(e));
        ret.add(range);

        return ret;
    }

    /**
     * @param ip
     * @param port
     * @param timeout in milliseconds
     * @return
     */
    public static boolean isRemotePortOpen(String ip, int port, int timeout) {
        Socket socket = null;

        socket = new Socket();
        try {
            socket.setReuseAddress(true);
            SocketAddress sa = new InetSocketAddress(ip, port);
            socket.connect(sa, timeout);
            return socket.isConnected();
        } catch (SocketException e) {
            logger.debug(String.format("unable to connect remote port[ip:%s, port:%s], %s", ip, port, e.getMessage()));
            return false;
        } catch (IOException e) {
            logger.debug(String.format("unable to connect remote port[ip:%s, port:%s], %s", ip, port, e.getMessage()));
            return false;
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                logger.warn(e.getMessage(), e);
            }
        }
    }

    public static List<String> getFreeIpInRange(String startIp, String endIp, List<String> usedIps, int limit, String start) {
        long s = ipv4StringToLong(startIp);
        long e = ipv4StringToLong(endIp);
        long f = ipv4StringToLong(start);
        List<String> res = new ArrayList<String>();
        for (long i = Math.max(s, f); i <= e; i++) {
            String ip = longToIpv4String(i);
            if (!usedIps.contains(ip)) {
                res.add(ip);
            }

            if (res.size() >= limit) {
                break;
            }
        }

        return res;
    }

    public static List<String> getFreeIpv6InRange(String startIp, String endIp, List<String> usedIps, int limit, String start) {
        IPv6Address s = IPv6Address.fromString(startIp);
        IPv6Address e = IPv6Address.fromString(endIp);
        IPv6Address f = IPv6Address.fromString(start);

        List<String> res = new ArrayList<String>();
        while (s.compareTo(e) <= 0) {
            if (s.compareTo(f) <= 0) {
                s = s.add(1);
                continue;
            }
            if (usedIps.contains(s.toString())) {
                s = s.add(1);
                continue;
            }
            res.add(s.toString());
            s = s.add(1);

            if (res.size() >= limit) {
                break;
            }
        }

        return res;
    }

    public static List<String> getAllMac() {
        ShellResult res = ShellUtils.runAndReturn("ip a | awk '/link\\// && !/loopback/ {print $2}' | sort -u");

        if (!res.isReturnCode(0)) {
            throw new RuntimeException("Fail to get mac address");
        }

        List<String> macs = Arrays.asList(res.getStdout().split("\n"));

        return macs.stream().map(StringUtils::strip).collect(Collectors.toList());
    }

    public static boolean isFullCidr(String cidr) {
        return cidr.equals("0.0.0.0/0");
    }

    public static SubnetUtils.SubnetInfo getSubnetInfo(SubnetUtils subnet) {
        SubnetUtils.SubnetInfo info = subnet.getInfo();
        if (info.getAddressCountLong() > 0) {
            return info;
        }

        subnet.setInclusiveHostCount(true);
        return subnet.getInfo();
    }

    public static boolean isCidrOverlap(String cidr1, String cidr2) {
        DebugUtils.Assert(isCidr(cidr1), String.format("%s is not a cidr", cidr1));
        DebugUtils.Assert(isCidr(cidr2), String.format("%s is not a cidr", cidr2));

        SubnetUtils su1 = new SubnetUtils(cidr1);
        SubnetUtils su2 = new SubnetUtils(cidr2);

        SubnetUtils.SubnetInfo info1 = getSubnetInfo(su1);
        SubnetUtils.SubnetInfo info2 = getSubnetInfo(su2);

        return isIpv4RangeOverlap(info1.getLowAddress(), info1.getHighAddress(), info2.getLowAddress(), info2.getHighAddress());
    }

    public static boolean isSameCidr(String cidr1, String cidr2) {
        DebugUtils.Assert(isCidr(cidr1), String.format("%s is not a cidr", cidr1));
        DebugUtils.Assert(isCidr(cidr2), String.format("%s is not a cidr", cidr2));

        SubnetUtils su1 = new SubnetUtils(cidr1);
        SubnetUtils su2 = new SubnetUtils(cidr2);

        return su1.getInfo().getNetworkAddress().equals(su2.getInfo().getNetworkAddress());
    }

    public static boolean isIpv4InCidr(String ipv4, String cidr) {
        DebugUtils.Assert(isCidr(cidr), String.format("%s is not a cidr", cidr));
        validateIpv4(ipv4);

        SubnetUtils.SubnetInfo info = getSubnetInfo(new SubnetUtils(cidr));
        return isIpv4InRange(ipv4, info.getLowAddress(), info.getHighAddress());
    }

    public static boolean isIpInCidr(String ip, String cidr) {
        if (isIpv4Address(ip) && isCidr(cidr, IPv6Constants.IPv4)) {
            return isIpv4InCidr(ip, cidr);
        } else if (isIpv6Address(ip) && isCidr(cidr, IPv6Constants.IPv6)) {
            return IPv6NetworkUtils.isIpv6InCidrRange(ip, cidr);
        }

        return false;
    }

    public static List<String> filterIpv4sInCidr(List<String> ipv4s, String cidr) {
        DebugUtils.Assert(isCidr(cidr), String.format("%s is not a cidr", cidr));
        SubnetUtils.SubnetInfo info = getSubnetInfo(new SubnetUtils(cidr));
        List<String> results = new ArrayList<>();

        for (String ipv4 : ipv4s) {
            validateIpv4(ipv4);
            if (isIpv4InRange(ipv4, info.getLowAddress(), info.getHighAddress())) {
                results.add(ipv4);
            }
        }
        return results;
    }

    public static boolean isIpRoutedByDefaultGateway(String ip) {
        ShellResult res = ShellUtils.runAndReturn(String.format("ip route get %s | grep -q \"via $(ip route | awk '/default/ {print $3}')\"", ip));
        return res.isReturnCode(0);
    }

    public static boolean isSubCidr(String cidr, String subCidr) {
        DebugUtils.Assert(isCidr(cidr), String.format("%s is not a cidr", cidr));
        DebugUtils.Assert(isCidr(subCidr), String.format("%s is not a cidr", subCidr));

        SubnetUtils.SubnetInfo range = getSubnetInfo(new SubnetUtils(cidr));
        SubnetUtils.SubnetInfo sub = getSubnetInfo(new SubnetUtils(subCidr));
        return range.isInRange(sub.getLowAddress()) && range.isInRange(sub.getHighAddress());
    }

    public static String getNetworkAddressFromCidr(String cidr) {
        DebugUtils.Assert(isCidr(cidr), String.format("%s is not a cidr", cidr));
        SubnetUtils n = new SubnetUtils(cidr);
        return String.format("%s/%s", n.getInfo().getNetworkAddress(), cidr.split("\\/")[1]);
    }


    public static List<String> getIpRangeFromIps(List<String> ips) {
        List<Pair<String, String>> ipRanges = findConsecutiveIpRange(ips);
        List<String> internalIpRanges = new ArrayList<String>(ipRanges.size());
        for (Pair<String, String> p : ipRanges) {
            if (p.first().equals(p.second())) {
                internalIpRanges.add(p.first());
            } else {
                internalIpRanges.add(String.format("%s-%s", p.first(), p.second()));
            }
        }
        return internalIpRanges;
    }

    public static String fmtCidr(final String origin) {
        // format "*.*.1.1/16" to "*.*.0.0/16"
        DebugUtils.Assert(isCidr(origin), String.format("%s is not a cidr", origin));
        return new SubnetUtils(origin).getInfo().getNetworkAddress() + "/" + origin.split("/")[1];
    }

    public static List<String> getCidrsFromIpRange(String startIp, String endIp) {
        return getCidrsFromIpRange(startIp, endIp, true);
    }

    public static List<String> getCidrsFromIpRange(String startIp, String endIp, boolean exact) {
        if (!isIpv4Address(startIp)) {
            throw new IllegalArgumentException(String.format("%s is not a valid ipv4 address", startIp));
        }
        if (!isIpv4Address(endIp)) {
            throw new IllegalArgumentException(String.format("%s is not a valid ipv4 address", endIp));
        }

        long start = exact ? ipToLong(startIp) : ipToLong(startIp, 0L);
        long end = exact ? ipToLong(endIp) : ipToLong(endIp, 255L);

        ArrayList<String> pairs = new ArrayList<String>();
        while (end >= start) {
            byte maxsize = 32;
            while (maxsize > 0) {
                long mask = CIDR2MASK[maxsize - 1];
                long maskedBase = start & mask;

                if (maskedBase != start) {
                    break;
                }

                maxsize--;
            }
            double x = Math.log((double) end - start + 1) / Math.log(2);
            byte maxdiff = (byte) (32 - Math.floor(x));
            if (maxsize < maxdiff) {
                maxsize = maxdiff;
            }
            String ip = longToIP(start);
            pairs.add(ip + "/" + maxsize);
            start += Math.pow(2, (32 - maxsize));
        }
        logger.debug(String.format("get Cidrs from startIp:[%s], endId: [%s]", startIp, endIp));
        logger.debug(String.format("cidrs: %s", pairs.toString()));
        return pairs;
    }

    public static final int[] CIDR2MASK = new int[]{0x00000000, 0x80000000,
            0xC0000000, 0xE0000000, 0xF0000000, 0xF8000000, 0xFC000000,
            0xFE000000, 0xFF000000, 0xFF800000, 0xFFC00000, 0xFFE00000,
            0xFFF00000, 0xFFF80000, 0xFFFC0000, 0xFFFE0000, 0xFFFF0000,
            0xFFFF8000, 0xFFFFC000, 0xFFFFE000, 0xFFFFF000, 0xFFFFF800,
            0xFFFFFC00, 0xFFFFFE00, 0xFFFFFF00, 0xFFFFFF80, 0xFFFFFFC0,
            0xFFFFFFE0, 0xFFFFFFF0, 0xFFFFFFF8, 0xFFFFFFFC, 0xFFFFFFFE,
            0xFFFFFFFF};

    public static String getCidrFromIpMask(String ip, String mask) {
        try {
            InetAddress netmask = InetAddress.getByName(mask);
            return getNetworkAddressFromCidr(String.format("%s/%s", ip, convertNetmaskToCIDR(netmask)));
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException(String.format("%s is not a valid ipv4 netmask", ip));
        }
    }

    public static int convertNetmaskToCIDR(InetAddress netmask) {

        byte[] netmaskBytes = netmask.getAddress();
        int cidr = 0;
        boolean zero = false;
        for (byte b : netmaskBytes) {
            int mask = 0x80;

            for (int i = 0; i < 8; i++) {
                int result = b & mask;
                if (result == 0) {
                    zero = true;
                } else if (zero) {
                    throw new IllegalArgumentException("invalid netmask");
                } else {
                    cidr++;
                }
                mask >>>= 1;
            }
        }
        return cidr;
    }

    private static long ipToLong(String strIP) {
        return ipToLong(strIP, null);
    }

    private static long ipToLong(String strIP, Long last) {
        long[] ip = new long[4];
        String[] ipSec = strIP.split("\\.");
        for (int k = 0; k < 4; k++) {
            ip[k] = Long.parseLong(ipSec[k]);
        }

        return (ip[0] << 24) + (ip[1] << 16) + (ip[2] << 8) + (last == null ? ip[3] : last);
    }

    private static String longToIP(long longIP) {
        StringBuilder sb = new StringBuilder();
        sb.append((longIP >>> 24));
        sb.append(".");
        sb.append(((longIP & 0x00FFFFFF) >>> 16));
        sb.append(".");
        sb.append(((longIP & 0x0000FFFF) >>> 8));
        sb.append(".");
        sb.append((longIP & 0x000000FF));

        return sb.toString();
    }

    public static boolean isInRange(String ip, String startIp, String endIp) {
        if (isIpv4Address(ip)) {
            return isIpv4InRange(ip, startIp, endIp);
        } else if (isIpv6Address(ip)) {
            return isIpv6InRange(ip, startIp, endIp);
        } else {
            throw new IllegalArgumentException(String.format("%s is not a valid ipv4 address or valid ipv6 address", ip));
        }
    }

    public static String getSmallestIp(List<String> ips) {
        if (isIpv4Address(ips.get(0))) {
            List<Long> addresses = ips.stream().map(NetworkUtils::ipToLong).sorted(Long::compareTo).collect(Collectors.toList());
            return longToIP(addresses.get(0));
        } else {
            List<IPv6Address> addresses = ips.stream().map(IPv6Address::fromString).sorted(IPv6Address::compareTo).collect(Collectors.toList());
            return addresses.get(0).toString();
        }
    }

    public static String getBiggesttIp(List<String> ips) {
        int length = ips.size();
        if (isIpv4Address(ips.get(0))) {
            List<Long> addresses = ips.stream().map(NetworkUtils::ipToLong).sorted(Long::compareTo).collect(Collectors.toList());
            return longToIP(addresses.get(length - 1));
        } else {
            List<IPv6Address> addresses = ips.stream().map(IPv6Address::fromString).sorted(IPv6Address::compareTo).collect(Collectors.toList());
            return addresses.get(length - 1).toString();
        }
    }

    public static Integer getIpversion(String ip) {
        if (isIpv4Address(ip)) {
            return IPv6Constants.IPv4;
        } else {
            IPv6Address.fromString(ip);
            return IPv6Constants.IPv6;
        }
    }

    public static Integer getPrefixLengthFromNetmask(String mask) {
        if (isIpv4Address(mask)) {
            return validNetmasks.get(mask);
        } else if (isIpv6Address(mask)) {
            IPv6Address netmask = IPv6Address.fromString(mask);
            return netmask.numberOfLeadingOnes();
        }

        return 0;
    }

    public static String convertNetmask(Integer prefix) {
        int value = 0xffffffff << (32 - prefix);
        byte[] bytes = new byte[]{
                (byte) (value >>> 24),
                (byte) (value >> 16 & 0xff),
                (byte) (value >> 8 & 0xff),
                (byte) (value & 0xff)
        };
        try {
            return InetAddress.getByAddress(bytes).getHostAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean isValidMacAddress(String macAddress) {
        final String MAC_REGEX = "^([A-Fa-f0-9]{2}[-,:]){5}[A-Fa-f0-9]{2}$";
        return macAddress != null && !macAddress.isEmpty() && (macAddress.matches(MAC_REGEX));
    }

    public static boolean isBelongToSameSubnet(String addr1, String addr2, String mask) {
        byte[] addr1Byte = new byte[0];
        byte[] addr2Byte = new byte[0];
        byte[] maskByte = new byte[0];
        try {
            addr1Byte = InetAddress.getByName(addr1).getAddress();
            addr2Byte = InetAddress.getByName(addr2).getAddress();
            maskByte = InetAddress.getByName(mask).getAddress();
        } catch (UnknownHostException e) {
            return false;
        }

        for (int i = 0; i < addr1Byte.length; i++)
            if ((addr1Byte[i] & maskByte[i]) != (addr2Byte[i] & maskByte[i]))
                return false;
        return true;
    }

    public static boolean isValidIPAddress(String ip) {
        if (isIpv4Address(ip)) {
            return true;
        } else {
            return isIpv6Address(ip);
        }
    }

    public static String getCanonicalNetworkCidr(String gw, String netmask) {
        Long gateway = ipv4StringToLong(gw);
        Long mask = ipv4StringToLong(netmask);

        String cidr = longToIP(gateway & mask);
        return String.format("%s/%d", cidr, validNetmasks.get(netmask));
    }

    static boolean isIpv4MulticastAddress(String ip) {
        return isInRange(ip, "224.0.0.0", "239.255.255.255");
    }

    static boolean isIpv4ClassEAddress(String ip) {
        return isInRange(ip, "240.0.0.0", "254.255.255.254");
    }

    static boolean isIpv4LocalAddress(String ip) {
        return isInRange(ip, "127.0.0.0", "127.255.255.255");
    }

    static boolean isIpv4LinkLocalAddress(String ip) {
        return isInRange(ip, "169.254.0.0", "169.254.255.255");
    }

    public static boolean isUnicastIPAddress(String ip) {
        if (isIpv4Address(ip)) {
            return !(isIpv4MulticastAddress(ip) || isIpv4LocalAddress(ip) || isIpv4LinkLocalAddress(ip) || isIpv4ClassEAddress(ip));
        } else {
            return isIpv6UnicastAddress(ip);
        }
    }

    public static boolean isMulticastCidr(String cidr) {
        try {
            IPv6Network network6 = IPv6Network.fromString(cidr);
            String formatCidr = network6.toString();
            String formatAddress = formatCidr.split("/")[0];
            IPv6Address address6 = IPv6Address.fromString(formatAddress);
            return address6.isMulticast();
        } catch (Exception e) {
            Pattern pattern = Pattern.compile("^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])(\\/(\\d|[1-2]\\d|3[0-2]))$");
            Matcher matcher = pattern.matcher(cidr);
            if (!matcher.find()) {
                return false;
            }

            SubnetUtils sub = new SubnetUtils(cidr);
            String address = sub.getInfo().getAddress();
            return isIpv4MulticastAddress(address);
        }
    }

    public static boolean isReachable(String host, int timeout) {
        try {
            InetAddress address = InetAddress.getByName(host);
            return address.isReachable(timeout);
        } catch (IOException ignored) {
            return false;
        }
    }

    public static boolean isHostReachable(String address, int port, int timeout) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(address, port), timeout);
            return true;
        } catch (IOException exception) {
            return false;
        }
    }

    /**
     * Generate vlan device name like eth0.100
     *
     * @param ifName interface name
     * @param vlanId vlan id
     * @return vlan device name
     */
    public static String generateVlanDeviceName(String ifName, Integer vlanId) {
        return String.format("%s.%s", ifName, vlanId.toString());
    }

    public static boolean isInIpv6Range(String startIp, String endIp, String ip) {
        IPv6Address startIpv6Addresses = IPv6Address.fromString(startIp);
        IPv6Address endIpv6Addresses = IPv6Address.fromString(endIp);
        IPv6Address iPv6Addresses = IPv6Address.fromString(ip);

        return startIpv6Addresses.toBigInteger().compareTo(iPv6Addresses.toBigInteger()) <= 0 && endIpv6Addresses.toBigInteger().compareTo(iPv6Addresses.toBigInteger()) >= 0;
    }

    public static final String NETWORK_CFG_EMPTY = "none";

    public static final String PROTO_DHCP = "dhcp";
    public static final String PROTO_STATIC = "static";

    public static final String DEFAULT_IPV4_PREFIX = "32";
    public static final String DEFAULT_IPV6_PREFIX = "128";
    public static final String DEFAULT_IPV4_PREFIX_SPLIT = "/";
    public static final String DEFAULT_IPV6_PREFIX_SPLIT = "/";

    public static String ipv4PrefixToNetmask(String prefix) {
        return ipv4PrefixToNetmask(Integer.parseInt(prefix));
    }

    public static String ipv4PrefixToNetmask(Integer prefixLength) {
        if (prefixLength < 1 || prefixLength > 32) {
            return String.format("255.255.255.255");
        }

        int mask = 0xFFFFFFFF << (32 - prefixLength);
        int netmask1 = (mask >> 24) & 0xFF;
        int netmask2 = (mask >> 16) & 0xFF;
        int netmask3 = (mask >> 8) & 0xFF;
        int netmask4 = mask & 0xFF;

        return String.format("%d.%d.%d.%d", netmask1, netmask2, netmask3, netmask4);
    }

    /**
     * Checks if the provided IP address is an Automatic Private IP Address (APIPA).
     * Read vm ip to usedIp need avoid it
     */
    public static Boolean isAutomaticPrivateIpAddr(String ipv4) {
        if (ipv4 == null || ipv4.isEmpty()) {
            return false;
        }
        String[] parts = ipv4.split("\\.");
        if (parts.length != 4) {
            return false;
        }
        try {
            return Integer.parseInt(parts[0]) == 169 && Integer.parseInt(parts[1]) == 254;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static Boolean isInternalAddress(String ip) {
        if (isIpv4Address(ip)) {
            return isAutomaticPrivateIpAddr(ip);
        } else if (isIpv6Address(ip)) {
            return isLinkLocalAddress(ip);
        } else {
            return Boolean.FALSE;
        }
    }

    public static Boolean isValidVlan(Integer vlanId) {
        return vlanId != null && vlanId >= 0 && vlanId <= 4094;
    }

    public static Boolean isValidVni(Integer vni) {
        return vni != null && vni >= 1 && vni <= 16777214;
    }

    public static int compareIpv4Address(String ip1, String ip2) {
        long diff = NetworkUtils.ipv4StringToLong(ip1) - NetworkUtils.ipv4StringToLong(ip2);
        return diff > 0 ? 1 : diff == 0 ? 0 : -1;
    }

    public static String getIp(String ipAddress) {
        return ipAddress == null ? null : ipAddress.split(NetworkUtils.DEFAULT_IPV4_PREFIX_SPLIT)[0];
    }

    public static String getPrefix(String ipAddress) {
        if (ipAddress == null) {
            return null;
        }

        int splitIndex = ipAddress.indexOf(NetworkUtils.DEFAULT_IPV4_PREFIX_SPLIT);
        if (splitIndex == -1) {
            return isIpv4Address(ipAddress) ? NetworkUtils.DEFAULT_IPV4_PREFIX
                    : isIpv6Address(ipAddress) ? NetworkUtils.DEFAULT_IPV6_PREFIX
                    : null;
        }
        return ipAddress.substring(splitIndex + 1);
    }

    public static String getNetmask(String ipAddress) {
        String prefix = getPrefix(ipAddress);

        return prefix == null ? null
                : isValidIpv4(ipAddress) ? ipv4PrefixToNetmask(prefix)
                : isValidIpv6(ipAddress) ? getFormalNetmaskOfNetworkCidr(ipAddress)
                : null;
    }
}

