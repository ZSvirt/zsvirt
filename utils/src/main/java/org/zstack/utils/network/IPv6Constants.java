package org.zstack.utils.network;

import java.math.BigInteger;

public class IPv6Constants {
    public static int NONE = 0;
    public static int IPv4 = 4;
    public static int IPv6 = 6;
    public static int DUAL_STACK = 46;

    public static final String SLAAC = "SLAAC";
    public static final String Stateless_DHCP = "Stateless-DHCP";
    public static final String Stateful_DHCP = "Stateful-DHCP";

    public static final int IPV6_STATELESS_PREFIX_LEN = 64;

    public static final int IPV6_PREFIX_LEN_MAX = 126;
    public static final int IPV6_PREFIX_LEN_MIN = 8;
    public static final int IPV6_PREFIX_LEN_MIN_DNSMASQ = 64;

    public static BigInteger IntegerMax = BigInteger.valueOf(Integer.MAX_VALUE);
}
