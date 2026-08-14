package org.zstack.network.l3;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.network.l3.*;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.IPv6Constants;
import org.zstack.utils.network.IPv6NetworkUtils;
import org.zstack.utils.network.NetworkUtils;

import java.util.Comparator;
import java.util.List;

import static org.zstack.core.Platform.err;
import static org.zstack.utils.CollectionUtils.*;

/**
 */
public abstract class AbstractIpAllocatorStrategy implements IpAllocatorStrategy  {
    private static final CLogger logger = Utils.getLogger(AbstractIpAllocatorStrategy.class);

    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected L3NetworkManager l3NwMgr;
    @Autowired
    protected ErrorFacade errf;
    @Autowired
    private PluginRegistry pluginRgty;

    protected String getReqIpRangeType(IpAllocateMessage msg) {
        if (msg.getIpRangeUuid() != null) {
            return IpRangeHelper.getIpRangeType(msg.getIpRangeUuid());
        } else {
            return IpRangeType.Normal.toString();
        }
    }

    protected List<IpRangeVO> getIpRanges(String ipRangeType, String l3NetworkUuid, int ipVersion, boolean needStrip) {
        List<IpRangeVO> iprs;
        if (ipRangeType.equals(IpRangeType.Normal.toString())) {
            iprs = Q.New(NormalIpRangeVO.class).eq(NormalIpRangeVO_.l3NetworkUuid, l3NetworkUuid)
                    .eq(NormalIpRangeVO_.ipVersion, ipVersion).list();
        } else {
            iprs = Q.New(AddressPoolVO.class).eq(AddressPoolVO_.l3NetworkUuid, l3NetworkUuid)
                    .eq(AddressPoolVO_.ipVersion, ipVersion).list();
        }
        if (ipVersion == IPv6Constants.IPv4) {
            if (needStrip) {
                iprs = filter(iprs, IpRangeHelper::stripNetworkAndBroadcastAddress);
            }
            iprs.sort(AbstractIpAllocatorStrategy::compareIpv4Range);
        } else {
            iprs.sort(Comparator.comparing(r -> IPv6NetworkUtils.ipv6AddressToBigInteger(r.getStartIp())));
        }
        return iprs;
    }

    protected List<IpRangeVO> getReqIpRanges(IpAllocateMessage msg, int ipVersion) {
        List<IpRangeVO> iprs;

        if (msg.getIpRangeUuid() != null) {
            iprs = Q.New(IpRangeVO.class).eq(IpRangeVO_.uuid, msg.getIpRangeUuid()).list();
        } else {
            iprs = getIpRanges(getReqIpRangeType(msg), msg.getL3NetworkUuid(), ipVersion, false);
        }
        if (ipVersion == IPv6Constants.IPv4) {
            iprs = filter(iprs, IpRangeHelper::stripNetworkAndBroadcastAddress);
        }
        return iprs;
    }

    protected UsedIpInventory allocateRequiredIp(IpAllocateMessage msg) {
        List<IpRangeVO> iprs = getReqIpRanges(msg, IPv6Constants.IPv4);
        final long rip = NetworkUtils.ipv4StringToLong(msg.getRequiredIp());

        IpRangeVO ipr = findOneOrNull(iprs, arg -> {
            long s = NetworkUtils.ipv4StringToLong(arg.getStartIp());
            long e = NetworkUtils.ipv4StringToLong(arg.getEndIp());
            return s <= rip && rip <= e;
        });

        if (ipr == null) {
            L3NetworkVO l3NetworkVO = Q.New(L3NetworkVO.class).eq(L3NetworkVO_.uuid, msg.getL3NetworkUuid()).find();
            if (!l3NetworkVO.getType().equals(L3NetworkConstant.L3_BASIC_NETWORK_TYPE)) {
                for (AfterAllocateRequiredIpExtensionPoint extp : pluginRgty.getExtensionList(AfterAllocateRequiredIpExtensionPoint.class)) {
                    ipr = extp.afterAllocateRequiredIp(msg, ipr, iprs);
                }
            }
        }

        if (ipr == null) {
            throw new OperationFailureException(err(L3Errors.ALLOCATE_IP_ERROR,
                    "cannot find ip range that has ip[%s] in l3Network[uuid:%s]", msg.getRequiredIp(), msg.getL3NetworkUuid()
            ));
        }

        return l3NwMgr.reserveIp(ipr, msg.getRequiredIp(), msg.isDuplicatedIpAllowed());
    }

    protected UsedIpInventory allocateRequiredIpv6(IpAllocateMessage msg) {
        List<IpRangeVO> iprs = getReqIpRanges(msg, IPv6Constants.IPv6);
        IpRangeVO ipr = findOneOrNull(iprs,
                arg -> IPv6NetworkUtils.isIpv6InRange(msg.getRequiredIp(), arg.getStartIp(), arg.getEndIp()));

        if (ipr == null) {
            L3NetworkVO l3NetworkVO = Q.New(L3NetworkVO.class).eq(L3NetworkVO_.uuid, msg.getL3NetworkUuid()).find();
            if (!l3NetworkVO.getType().equals(L3NetworkConstant.L3_BASIC_NETWORK_TYPE)) {
                for (AfterAllocateRequiredIpExtensionPoint extp : pluginRgty.getExtensionList(AfterAllocateRequiredIpExtensionPoint.class)) {
                    ipr = extp.afterAllocateRequiredIp(msg, ipr, iprs);
                }
            }
        }

        if (ipr == null) {
            throw new OperationFailureException(err(L3Errors.ALLOCATE_IP_ERROR,
                    "cannot find ip range that has ip[%s] in l3Network[uuid:%s]", msg.getRequiredIp(), msg.getL3NetworkUuid()
            ));
        }

        return l3NwMgr.reserveIp(ipr, msg.getRequiredIp(), msg.isDuplicatedIpAllowed());
    }

    public static int compareIpv4Range(IpRangeVO r1, IpRangeVO r2) {
        long diff = NetworkUtils.ipv4StringToLong(r1.getStartIp()) - NetworkUtils.ipv4StringToLong(r2.getStartIp());
        return diff > 0 ? 1 : diff == 0 ? 0 : -1;
    }
}
