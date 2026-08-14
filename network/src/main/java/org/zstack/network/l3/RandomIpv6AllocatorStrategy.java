package org.zstack.network.l3;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.Q;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.header.network.l3.*;
import org.zstack.utils.network.IPv6Constants;
import org.zstack.utils.network.IPv6NetworkUtils;

import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

public class RandomIpv6AllocatorStrategy extends AbstractIpAllocatorStrategy {
    public static final IpAllocatorType type = new IpAllocatorType(L3NetworkConstant.RANDOM_IPV6_ALLOCATOR_STRATEGY);
    private static final Random random = new Random();
    @Autowired
    private PluginRegistry pluginRgty;

    @Override
    public IpAllocatorType getType() {
        return type;
    }

    @Override
    public UsedIpInventory allocateIp(IpAllocateMessage msg) {
        if (msg.getRequiredIp() != null) {
            return allocateRequiredIpv6(msg);
        }

        List<IpRangeVO> ranges = getReqIpRanges(msg, IPv6Constants.IPv6);

        Collections.shuffle(ranges);

        do {
            String ip = null;
            IpRangeVO tr = null;

            for (IpRangeVO r : ranges) {
                ip = allocateIp(r, msg.getExcludedIp());
                tr = r;
                if (ip != null) {
                    break;
                }
            }

            if (ip == null) {
                return null;
            }

            UsedIpInventory inv = l3NwMgr.reserveIp(tr, ip, msg.isDuplicatedIpAllowed());
            if (inv != null) {
                return inv;
            }
        } while (true);
    }


    private String allocateIp(IpRangeVO vo, String excludeIp) {
        BigInteger start = IPv6NetworkUtils.ipv6AddressToBigInteger(vo.getStartIp());
        BigInteger end = IPv6NetworkUtils.ipv6AddressToBigInteger(vo.getEndIp());

        BigInteger exclude = null;
        if (excludeIp != null) {
            exclude = IPv6NetworkUtils.ipv6AddressToBigInteger(excludeIp);
        }

        String gateway = Q.New(IpRangeVO.class).select(IpRangeVO_.gateway).eq(IpRangeVO_.uuid, vo.getUuid()).findValue();
        List<String> ips = Q.New(UsedIpVO.class).select(UsedIpVO_.ip).eq(UsedIpVO_.ipRangeUuid, vo.getUuid()).notEq(UsedIpVO_.ip, gateway).listValues();
        ips = ips.stream().distinct().collect(Collectors.toList());
        BigInteger num = start.add(BigInteger.valueOf(ips.size()));
        if (num.compareTo(end) > 0) {
            return null;
        }

        Set<BigInteger> usedIps = new HashSet<>();
        for (String ip : ips) {
            BigInteger address = IPv6NetworkUtils.ipv6AddressToBigInteger(ip);
            usedIps.add(address);
        }

        if (exclude != null) {
            usedIps.add(exclude);
        }

        /* a stateful dhcp range with 2^24 is big enough */
        int total = end.subtract(start).intValue();
        if ((total > (1 << 23)) || (total < 0)) {
            total = (1 << 23);
        }
        String address = null;
        do {
            num = start.add(new BigInteger(String.valueOf(random.nextInt(total + 1))));
            if (!usedIps.contains(num)) {
                address = IPv6NetworkUtils.ipv6AddressToString(num);
                break;
            }
        } while (true);

        return  address;
    }
}
