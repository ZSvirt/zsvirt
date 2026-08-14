package org.zstack.network.l3;

import org.zstack.core.db.Q;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.network.l3.*;
import org.zstack.header.tag.SystemTagVO;
import org.zstack.header.tag.SystemTagVO_;
import org.zstack.tag.PatternedSystemTag;
import org.zstack.tag.SystemTagCreator;
import org.zstack.utils.TagUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.IPv6Constants;
import org.zstack.utils.network.NetworkUtils;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

public class AscDelayRecycleIpAllocatorStrategy extends AbstractIpAllocatorStrategy implements IpRangeDeletionExtensionPoint {
    private static final CLogger logger = Utils.getLogger(AscDelayRecycleIpAllocatorStrategy.class);
    private static final IpAllocatorType type = new IpAllocatorType(L3NetworkConstant.ASC_DELAY_RECYCLE_IP_ALLOCATOR_STRATEGY);

    private static class IpCursorStruct {
        private String ipRangeUuid;

        private String nextIp;

        public static final String NULL_VALUE = "null";

        public IpCursorStruct() {
            this.ipRangeUuid = NULL_VALUE;
            this.nextIp = NULL_VALUE;
        }

        public String getIpRangeUuid() {
            return this.ipRangeUuid;
        }

        public String getNextIp() {
            return this.nextIp;
        }

        public void setIpRangeUuid(String ipRangeUuid) {
            this.ipRangeUuid = ipRangeUuid;
        }

        public void setNextIp(String nextIp) {
            this.nextIp = nextIp;
        }
    }

    private IpCursorStruct getIpCursorStruct(String l3NetworkUuid, String reqRangeType) {
        IpCursorStruct ipCursorStruct = new IpCursorStruct();

        PatternedSystemTag pst;
        String rangeUuidToken;
        String nextIpToken;

        if (reqRangeType.equals(IpRangeType.Normal.toString())) {
            pst = L3NetworkSystemTags.NETWORK_ASC_DELAY_NORMAL_NEXT_IP;
            rangeUuidToken = L3NetworkSystemTags.NETWORK_ASC_DELAY_NORMAL_NEXT_IPRANGE_UUID_TOKEN;
            nextIpToken = L3NetworkSystemTags.NETWORK_ASC_DELAY_NORMAL_NEXT_IP_TOKEN;
        } else {
            pst = L3NetworkSystemTags.NETWORK_ASC_DELAY_ADDRESS_POOL_NEXT_IP;
            rangeUuidToken = L3NetworkSystemTags.NETWORK_ASC_DELAY_ADDRESS_POOL_NEXT_IPRANGE_UUID_TOKEN;
            nextIpToken = L3NetworkSystemTags.NETWORK_ASC_DELAY_ADDRESS_POOL_NEXT_IP_TOKEN;
        }

        String tag = pst.getTag(l3NetworkUuid);
        if (tag == null) {
            SystemTagCreator creator = pst.newSystemTagCreator(l3NetworkUuid);
            creator.setTagByTokens(map(
                    e(rangeUuidToken, IpCursorStruct.NULL_VALUE),
                    e(nextIpToken, IpCursorStruct.NULL_VALUE)
            ));
            creator.create();
        } else {
            ipCursorStruct.setIpRangeUuid(pst.getTokenByTag(tag, rangeUuidToken));
            ipCursorStruct.setNextIp(pst.getTokenByTag(tag, nextIpToken));
        }

        return ipCursorStruct;
    }

    private void updateIpCursorStruct(String l3NetworkUuid, String ipRangeType, String ipRangeUuid, String ip) {
        PatternedSystemTag pst;
        String rangeUuidToken;
        String nextIpToken;

        if (ipRangeType.equals(IpRangeType.Normal.toString())) {
            pst = L3NetworkSystemTags.NETWORK_ASC_DELAY_NORMAL_NEXT_IP;
            rangeUuidToken = L3NetworkSystemTags.NETWORK_ASC_DELAY_NORMAL_NEXT_IPRANGE_UUID_TOKEN;
            nextIpToken = L3NetworkSystemTags.NETWORK_ASC_DELAY_NORMAL_NEXT_IP_TOKEN;
        } else {
            pst = L3NetworkSystemTags.NETWORK_ASC_DELAY_ADDRESS_POOL_NEXT_IP;
            rangeUuidToken = L3NetworkSystemTags.NETWORK_ASC_DELAY_ADDRESS_POOL_NEXT_IPRANGE_UUID_TOKEN;
            nextIpToken = L3NetworkSystemTags.NETWORK_ASC_DELAY_ADDRESS_POOL_NEXT_IP_TOKEN;
        }

        String tag = pst.getTag(l3NetworkUuid);

        final String tagUuid = Q.New(SystemTagVO.class)
                .select(SystemTagVO_.uuid)
                .eq(SystemTagVO_.resourceUuid, l3NetworkUuid)
                .eq(SystemTagVO_.resourceType, L3NetworkVO.class.getSimpleName())
                .like(SystemTagVO_.tag, TagUtils.tagPatternToSqlPattern(tag))
                .findValue();

        pst.updateByTagUuid(tagUuid, pst.instantiateTag(map(
                e(rangeUuidToken, ipRangeUuid),
                e(nextIpToken, ip)
        )));
    }

    @Override
    public IpAllocatorType getType() {
        return type;
    }

    private String allocateIpByAsc(IpRangeVO vo, String excludeIp) {
        List<BigInteger> used = l3NwMgr.getUsedIpInRange(vo);
        if (excludeIp != null) {
            used.add(new BigInteger(String.valueOf(NetworkUtils.ipv4StringToLong(excludeIp))));
            Collections.sort(used);
        }
        List<Long> usedIP = used.stream().map(BigInteger::longValue).filter(ip -> ip.longValue() >= NetworkUtils.ipv4StringToLong(vo.getStartIp())).collect(Collectors.toList());
        return NetworkUtils.findFirstAvailableIpv4Address(vo.getStartIp(), vo.getEndIp(), usedIP.toArray(new Long[usedIP.size()]));
    }

    private UsedIpInventory allocateIpByAsc(IpAllocateMessage msg, List<IpRangeVO> ranges) {
        String excludeIp = msg.getExcludedIp();
        do {
            String ip = null;
            IpRangeVO tr;

            for (IpRangeVO r : ranges) {
                if (l3NwMgr.isIpRangeFull(r)) {
                    logger.debug(String.format("Ip range[uuid:%s, name: %s] is exhausted, try next one", r.getUuid(), r.getName()));
                    continue;
                }

                ip = allocateIpByAsc(r, excludeIp);
                tr = r;
                if (ip != null) {
                    UsedIpInventory inv = l3NwMgr.reserveIp(tr, ip, msg.isDuplicatedIpAllowed());
                    if (inv != null) {
                        return inv;
                    }
                }
            }

            if (ip == null) {
                /* No available ip in ranges */
                return null;
            }
        } while (true);
    }

    private boolean findNextAndUpdateByAscDelayRecycle(IpAllocateMessage msg, List<IpRangeVO> ranges, String curUsedIpRangeUuid, String curUsedIp) {
        String ip;
        String excludeIp = msg.getExcludedIp();
        for (IpRangeVO r : ranges) {
            if (l3NwMgr.isIpRangeFull(r)) {
                logger.debug(String.format("Ip range[uuid:%s, name: %s] is exhausted, try next one", r.getUuid(), r.getName()));
                continue;
            }
            if (r.getUuid().equals(curUsedIpRangeUuid)) {
                IpRangeVO cloneRange = new IpRangeVO();
                cloneRange.setUuid(r.getUuid());
                long startIp = NetworkUtils.ipv4StringToLong(curUsedIp)+1;
                if (startIp > NetworkUtils.ipv4StringToLong(r.getEndIp())) {
                    continue;
                }
                cloneRange.setStartIp(NetworkUtils.longToIpv4String(startIp));
                cloneRange.setEndIp(r.getEndIp());
                cloneRange.setIpVersion(r.getIpVersion());
                ip = allocateIpByAsc(cloneRange, excludeIp);
            } else {
                ip = allocateIpByAsc(r, excludeIp);
            }
            if (ip != null) {
                updateIpCursorStruct(msg.getL3NetworkUuid(), getReqIpRangeType(msg), r.getUuid(), ip);
                return true;
            }
        }
        return false;
    }

    private boolean findNextAndUpdateByFirstAvailable(List<IpRangeVO> ranges, String l3NetworkUuid, String rangeType, String excludeIp) {
        String ip;
        for (IpRangeVO r : ranges) {
            if (l3NwMgr.isIpRangeFull(r)) {
                logger.debug(String.format("Ip range[uuid:%s, name: %s] is exhausted, try next one", r.getUuid(), r.getName()));
                continue;
            }

            ip = allocateIpByAsc(r, excludeIp);
            if (ip != null) {
                updateIpCursorStruct(l3NetworkUuid, rangeType, r.getUuid(), ip);
                return true;
            }
        }
        return false;
    }

    private void updateNextIp(IpAllocateMessage msg, String curUsedIpRangeUuid, String curUsedIp, List<IpRangeVO> ranges) {
        String excludeIp = msg.getExcludedIp();
        String rangeType = getReqIpRangeType(msg);

        long curIp = NetworkUtils.ipv4StringToLong(curUsedIp);
        List<IpRangeVO> rightRanges = new ArrayList<>();

        for (IpRangeVO ipr : ranges) {
            if (NetworkUtils.ipv4StringToLong(ipr.getStartIp()) <= curIp && curIp <= NetworkUtils.ipv4StringToLong(ipr.getEndIp())) {
                rightRanges.add(ipr);
            } else if (curIp < NetworkUtils.ipv4StringToLong(ipr.getStartIp())) {
                rightRanges.add(ipr);
            }
        }

        if (findNextAndUpdateByAscDelayRecycle(msg, rightRanges, curUsedIpRangeUuid, curUsedIp)) {
            return;
        }
        if (findNextAndUpdateByFirstAvailable(ranges, msg.getL3NetworkUuid(), rangeType, excludeIp)) {
            return;
        }

        updateIpCursorStruct(msg.getL3NetworkUuid(), rangeType, IpCursorStruct.NULL_VALUE, IpCursorStruct.NULL_VALUE);
    }

    @Override
    public UsedIpInventory allocateIp(IpAllocateMessage msg) {
        UsedIpInventory ret;

        IpCursorStruct ipCursorStruct = getIpCursorStruct(msg.getL3NetworkUuid(), getReqIpRangeType(msg));
        List<IpRangeVO> ranges;
        boolean needToUpdateIpCursor = true;

        if (msg.getRequiredIp() != null) {
            ret = allocateRequiredIp(msg);
            if (ret == null || !ret.getIp().equals(ipCursorStruct.getNextIp())) {
                return ret;
            }
            ranges = getReqIpRanges(msg, IPv6Constants.IPv4);
        } else if ( msg.getIpRangeUuid() != null) {
            ranges = getReqIpRanges(msg, IPv6Constants.IPv4);
            if (ipCursorStruct.getIpRangeUuid().equals(msg.getIpRangeUuid())) {
                msg.setRequiredIp(ipCursorStruct.getNextIp());
                ret = allocateRequiredIp(msg);
            } else {
                ret = allocateIpByAsc(msg, ranges);
                if (!ipCursorStruct.getIpRangeUuid().equals(IpCursorStruct.NULL_VALUE)) {
                    needToUpdateIpCursor = false;
                }
            }
        } else {
            ranges = getReqIpRanges(msg, IPv6Constants.IPv4);
            if (!ipCursorStruct.getNextIp().equals(IpCursorStruct.NULL_VALUE)) {
                msg.setIpRangeUuid(ipCursorStruct.getIpRangeUuid());
                msg.setRequiredIp(ipCursorStruct.getNextIp());
                ret = allocateRequiredIp(msg);
            } else {
                ret = allocateIpByAsc(msg, ranges);
            }
        }

        if (ret != null && needToUpdateIpCursor) {
            updateNextIp(msg, ret.getIpRangeUuid(), ret.getIp(), ranges);
        }
        return ret;
    }

    @Override
    public void preDeleteIpRange(IpRangeInventory ipRange) {
    }

    @Override
    public void beforeDeleteIpRange(IpRangeInventory ipRange) {
    }
    @Override
    public void afterDeleteIpRange(IpRangeInventory ipRange) {
        String l3NetworkUuid = ipRange.getL3NetworkUuid();
        String rangeType;

        IpCursorStruct normalIpCursorStruct = getIpCursorStruct(l3NetworkUuid, IpRangeType.Normal.toString());
        IpCursorStruct addressPoolIpCursorStruct = getIpCursorStruct(l3NetworkUuid, IpRangeType.AddressPool.toString());

        if (normalIpCursorStruct.getIpRangeUuid().equals(ipRange.getUuid())) {
            rangeType = IpRangeType.Normal.toString();
        } else if (addressPoolIpCursorStruct.getIpRangeUuid().equals(ipRange.getUuid())) {
            rangeType = IpRangeType.AddressPool.toString();
        } else {
            return;
        }

        List<IpRangeVO> ranges = getIpRanges(rangeType, l3NetworkUuid, IPv6Constants.IPv4, true);
        List<IpRangeVO> rightRanges = new ArrayList<>();

        for (IpRangeVO ipr : ranges) {
            if (NetworkUtils.ipv4StringToLong(ipr.getStartIp()) > NetworkUtils.ipv4StringToLong(ipRange.getStartIp())) {
                rightRanges.add(ipr);
            }
        }

        if (findNextAndUpdateByFirstAvailable(rightRanges, l3NetworkUuid, rangeType, null)) {
            return;
        }
        if (findNextAndUpdateByFirstAvailable(ranges, l3NetworkUuid, rangeType, null)) {
            return;
        }

        updateIpCursorStruct(l3NetworkUuid, rangeType, IpCursorStruct.NULL_VALUE, IpCursorStruct.NULL_VALUE);
    }

    public void failedToDeleteIpRange(IpRangeInventory ipRange, ErrorCode errorCode) {
    }
}
