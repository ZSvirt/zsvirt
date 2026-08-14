package org.zstack.network.l3;

import static org.zstack.core.Platform.err;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.network.l3.*;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.IPv6Constants;
import org.zstack.utils.network.IPv6NetworkUtils;
import org.zstack.utils.network.NetworkUtils;

import java.util.Objects;


public class StaticIpAllocatorStrategy extends AbstractIpAllocatorStrategy {
    private static final CLogger logger = Utils.getLogger(StaticIpAllocatorStrategy.class);

    public static final IpAllocatorType type = new IpAllocatorType(L3NetworkConstant.STATIC_IP_ALLOCATOR_STRATEGY);

    @Autowired
    protected DatabaseFacade dbf;

    @Override
    public IpAllocatorType getType() {
        return type;
    }

    @Override
    public UsedIpInventory allocateIp(IpAllocateMessage msg) {
        if (!(msg instanceof AllocateIpMsg)) {
            throw new OperationFailureException(err(L3Errors.ALLOCATE_IP_ERROR, "message[%s] is not AllocateIpMsg", msg.getClass()));
        }

        AllocateIpMsg amsg = (AllocateIpMsg) msg;

        L3NetworkVO l3 = Q.New(L3NetworkVO.class).eq(L3NetworkVO_.uuid, amsg.getL3NetworkUuid()).find();
        if (l3.enableIpAllocation()) {
            throw new OperationFailureException(err(L3Errors.ALLOCATE_IP_ERROR, "IP allocation of l3Network[uuid:%s] enabled, cannot allocate static ip",
                    amsg.getL3NetworkUuid()));
        }

        if (amsg.getRequiredIp() == null) {
            throw new OperationFailureException(err(L3Errors.ALLOCATE_IP_ERROR, "requiredIp cannot be null"));
        }

        Integer ipVersion;
        try {
            ipVersion = NetworkUtils.getIpversion(amsg.getRequiredIp());
            if (!Objects.equals(ipVersion, amsg.getIpVersion())) {
                throw new OperationFailureException(err(L3Errors.ALLOCATE_IP_ERROR, "ip version mismatch, requiredIp[%s] is %s, but ipVersion in message is %s",
                        amsg.getRequiredIp(), ipVersion, amsg.getIpVersion()));
            }
        } catch (Exception ignore) {
            throw new OperationFailureException(err(L3Errors.ALLOCATE_IP_ERROR, "invalid requiredIp[%s]", amsg.getRequiredIp()));
        }

        NormalIpRangeVO ipr = Q.New(NormalIpRangeVO.class)
                .eq(NormalIpRangeVO_.l3NetworkUuid, amsg.getL3NetworkUuid())
                .eq(NormalIpRangeVO_.ipVersion, ipVersion)
                .limit(1).find();

        if (IPv6Constants.IPv4 == ipVersion) {
            if (ipr == null) {
                if (StringUtils.isEmpty(amsg.getNetmask())) {
                    throw new OperationFailureException(err(L3Errors.ALLOCATE_IP_ERROR, "netmask must be set when no ip range is found for l3Network[uuid:%s]",
                            amsg.getL3NetworkUuid()));
                }
                if (!NetworkUtils.isNetmask(amsg.getNetmask())) {
                    throw new OperationFailureException(err(L3Errors.ALLOCATE_IP_ERROR, "netmask[%s] is not valid", amsg.getNetmask()));
                }
            } else {
                if (StringUtils.isEmpty(amsg.getNetmask())) {
                    amsg.setNetmask(ipr.getNetmask());
                } else if (!Objects.equals(amsg.getNetmask(), ipr.getNetmask())) {
                    throw new OperationFailureException(err(L3Errors.ALLOCATE_IP_ERROR, "netmask error, expect: %s, got: %s",
                            ipr.getNetmask(), amsg.getNetmask()));
                }
            }
        } else {
            if (ipr == null) {
                if (amsg.getPrefixLength() < IPv6Constants.IPV6_PREFIX_LEN_MIN || amsg.getPrefixLength() > IPv6Constants.IPV6_PREFIX_LEN_MAX) {
                    throw new OperationFailureException(err(L3Errors.ALLOCATE_IP_ERROR, "prefix length[%d] is out of range [%d - %d]",
                            amsg.getPrefixLength(), IPv6Constants.IPV6_PREFIX_LEN_MIN, IPv6Constants.IPV6_PREFIX_LEN_MAX));
                }
            } else {
                if (amsg.getPrefixLength() == 0) {
                    amsg.setPrefixLength(ipr.getPrefixLen());
                } else if (!Objects.equals(amsg.getPrefixLength(), ipr.getPrefixLen())) {
                    throw new OperationFailureException(err(L3Errors.ALLOCATE_IP_ERROR, "prefix length error, expect: %s, got: %s",
                            ipr.getPrefixLen(), amsg.getPrefixLength()));
                }
            }
            amsg.setNetmask(IPv6NetworkUtils.getFormalNetmaskOfNetworkCidr(String.format("%s/%d", amsg.getRequiredIp(), amsg.getPrefixLength())));
        }

        if (ipr != null) {
            if (StringUtils.isEmpty(amsg.getGateway())) {
                amsg.setGateway(ipr.getGateway());
            } else if (!Objects.equals(amsg.getGateway(), ipr.getGateway())) {
                throw new OperationFailureException(err(L3Errors.ALLOCATE_IP_ERROR, "gateway error, expect: %s, got: %s",
                        ipr.getGateway(), amsg.getGateway()));
            }
        }

        if (ipr != null && IPv6Constants.IPv4 == ipVersion)  {
            return allocateRequiredIp(amsg);
        } else if (ipr != null && IPv6Constants.IPv6 == ipVersion)  {
            return allocateRequiredIp(amsg);
        }

        String ip = amsg.getRequiredIp();
        UsedIpVO conflictIp = Q.New(UsedIpVO.class)
                .eq(UsedIpVO_.l3NetworkUuid, amsg.getL3NetworkUuid())
                .eq(UsedIpVO_.ipVersion, ipVersion)
                .eq(UsedIpVO_.ip, ip)
                .eq(UsedIpVO_.netmask, amsg.getNetmask())
                .find();
        if (conflictIp != null) {
            throw new OperationFailureException(err(L3Errors.ALLOCATE_IP_ERROR, "ip[%s] has been occupied by other usedIp[uuid:%s]", ip, conflictIp.getUuid()));
        }

        UsedIpVO vo = new UsedIpVO();
        vo.setUuid(Platform.getUuid());
        vo.setIp(ip);
        vo.setIpInLong(ipVersion == IPv6Constants.IPv4 ? NetworkUtils.ipv4StringToLong(ip) : 0);
        vo.setIpInBinary(NetworkUtils.ipStringToBytes(ip));
        vo.setIpVersion(ipVersion);
        vo.setL3NetworkUuid(amsg.getL3NetworkUuid());
        vo.setNetmask(amsg.getNetmask());

        vo = dbf.persistAndRefresh(vo);

        return UsedIpInventory.valueOf(vo);
    }
}
