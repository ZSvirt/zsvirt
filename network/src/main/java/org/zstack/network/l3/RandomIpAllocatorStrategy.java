package org.zstack.network.l3;

import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.header.network.l3.*;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.IPv6Constants;
import org.zstack.utils.network.NetworkUtils;

import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class RandomIpAllocatorStrategy extends AbstractIpAllocatorStrategy {
    private static final CLogger logger = Utils.getLogger(RandomIpAllocatorStrategy.class);
    public static final IpAllocatorType type = new IpAllocatorType(L3NetworkConstant.RANDOM_IP_ALLOCATOR_STRATEGY);
    private static final Random random = new Random();

    @Override
    public IpAllocatorType getType() {
        return type;
    }

    @Override
    public UsedIpInventory allocateIp(IpAllocateMessage msg) {
        if (msg.getRequiredIp() != null) {
            return allocateRequiredIp(msg);
        }

        List<IpRangeVO> ranges = getReqIpRanges(msg, IPv6Constants.IPv4);

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
	                /* No available ip in ranges */
                return null;
            }

            UsedIpInventory inv = l3NwMgr.reserveIp(tr, ip, msg.isDuplicatedIpAllowed());
            if (inv != null) {
                return inv;
            }
        } while (true);
    }

    private String steppingAllocate(long s, long e, Long ex, int total, String rangeUuid) {
        int step = 254;
        int failureCount = 0;
        int failureCheckPoint = 5;

        while (s <= e) {
            // if failing failureCheckPoint times, the range is probably full,
            // we check the range.
            // why don't we check before steppingAllocate()? because in that case we
            // have to count the used IP every time allocating a IP, and count operation
            // is a full scan in DB, which is very costly
            if (failureCheckPoint == failureCount++) {
                List<BigInteger> used = IpRangeHelper.getUsedIpInRange(rangeUuid, IPv6Constants.IPv4);
                long count = used.size();
                if (count == total) {
                    logger.debug(String.format("ip range[uuid:%s] has no ip available, try next one", rangeUuid));
                    return null;
                } else {
                    failureCount = 0;
                }
            }

            long te = s + step;
            te = te > e ? e : te;
            SimpleQuery<UsedIpVO> q = dbf.createQuery(UsedIpVO.class);
            q.select(UsedIpVO_.ipInLong);
            q.add(UsedIpVO_.ipInLong, Op.GTE, s);
            q.add(UsedIpVO_.ipInLong, Op.LTE, te);
            q.add(UsedIpVO_.ipRangeUuid, Op.EQ, rangeUuid);
            List<Long> used = q.listValue();
            used = used.stream().distinct().collect(Collectors.toList());
            if (te - s + 1 == used.size()) {
                s += step;
                continue;
            }

            if (ex != null) {
                used.add(ex);
            }

            Collections.sort(used);

            return NetworkUtils.randomAllocateIpv4Address(s, te, used);
        }

        return null;
    }

    private String allocateIp(IpRangeAO vo, String excludeIp) {
        int total = vo.size();
        long s = random.nextInt(total) + NetworkUtils.ipv4StringToLong(vo.getStartIp());
        long e = NetworkUtils.ipv4StringToLong(vo.getEndIp());

        Long ex = null;
        if (excludeIp != null) {
            ex = NetworkUtils.ipv4StringToLong(excludeIp);
        }

        String ret = steppingAllocate(s ,e, ex, total, vo.getUuid());
        if (ret != null) {
            return ret;
        }

        e = s;
        s = NetworkUtils.ipv4StringToLong(vo.getStartIp());
        return steppingAllocate(s ,e, ex, total, vo.getUuid());
    }
}
