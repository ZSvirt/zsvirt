package org.zstack.network.l3;


import com.googlecode.ipv6.IPv6Address;
import org.apache.commons.net.util.SubnetUtils;
import org.apache.commons.net.util.SubnetUtils.SubnetInfo;
import org.apache.commons.validator.routines.DomainValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.apimediator.InterceptorForService;
import org.zstack.header.apimediator.StopRoutingException;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.message.APIMessage;
import org.zstack.header.network.l2.*;
import org.zstack.header.network.l3.*;
import org.zstack.header.storage.snapshot.group.MemorySnapshotValidatorExtensionPoint;
import org.zstack.header.zone.ZoneVO;
import org.zstack.header.zone.ZoneVO_;
import org.zstack.network.service.MtuGetter;
import org.zstack.network.service.NetworkServiceGlobalConfig;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.IPv6Constants;
import org.zstack.utils.network.IPv6NetworkUtils;
import org.zstack.utils.network.NetworkUtils;

import java.util.*;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.argerr;
import static org.zstack.core.Platform.operr;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 9:04 PM
 * To change this template use File | Settings | File Templates.
 */
@InterceptorForService("network.l3")
public class L3NetworkApiInterceptor implements ApiMessageInterceptor {
    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    protected PluginRegistry pluginRgty;

    private final static CLogger logger = Utils.getLogger(L3NetworkApiInterceptor.class);

    private void setServiceId(APIMessage msg) {
        if (msg instanceof IpRangeMessage) {
            IpRangeMessage dmsg = (IpRangeMessage) msg;
            SimpleQuery<IpRangeVO> q = dbf.createQuery(IpRangeVO.class);
            q.select(IpRangeVO_.l3NetworkUuid);
            q.add(IpRangeVO_.uuid, SimpleQuery.Op.EQ, dmsg.getIpRangeUuid());
            String l3NwUuid = q.findValue();
            if (l3NwUuid == null) {
                SimpleQuery<ReservedIpRangeVO> rq = dbf.createQuery(ReservedIpRangeVO.class);
                rq.select(ReservedIpRangeVO_.l3NetworkUuid);
                rq.add(ReservedIpRangeVO_.uuid, SimpleQuery.Op.EQ, dmsg.getIpRangeUuid());
                l3NwUuid = rq.findValue();
            }
            dmsg.setL3NetworkUuid(l3NwUuid);
            bus.makeTargetServiceIdByResourceUuid(msg, L3NetworkConstant.SERVICE_ID, l3NwUuid);
        } else if (msg instanceof L3NetworkMessage) {
            L3NetworkMessage l3msg = (L3NetworkMessage) msg;
            bus.makeTargetServiceIdByResourceUuid(msg, L3NetworkConstant.SERVICE_ID, l3msg.getL3NetworkUuid());
        }
    }

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APIAddDnsToL3NetworkMsg) {
            validate((APIAddDnsToL3NetworkMsg) msg);
        } else if (msg instanceof APIAddIpRangeMsg) {
            validate((APIAddIpRangeMsg) msg);
        } else if (msg instanceof APIDeleteL3NetworkMsg) {
            validate((APIDeleteL3NetworkMsg) msg);
        } else if (msg instanceof APIRemoveDnsFromL3NetworkMsg) {
            validate((APIRemoveDnsFromL3NetworkMsg) msg);
        } else if (msg instanceof APICreateL3NetworkMsg) {
            validate((APICreateL3NetworkMsg) msg);
        } else if (msg instanceof APIGetIpAddressCapacityMsg) {
            validate((APIGetIpAddressCapacityMsg) msg);
        } else if (msg instanceof APIAddIpRangeByNetworkCidrMsg) {
            validate((APIAddIpRangeByNetworkCidrMsg) msg);
        } else if (msg instanceof APIGetFreeIpMsg) {
            validate((APIGetFreeIpMsg) msg);
        } else if (msg instanceof APICheckIpAvailabilityMsg) {
            validate((APICheckIpAvailabilityMsg) msg);
        } else if (msg instanceof APISetL3NetworkRouterInterfaceIpMsg) {
            validate((APISetL3NetworkRouterInterfaceIpMsg) msg);
        } else if (msg instanceof APIUpdateL3NetworkMsg) {
            validate((APIUpdateL3NetworkMsg) msg);
        } else if (msg instanceof APIAddHostRouteToL3NetworkMsg) {
            validate((APIAddHostRouteToL3NetworkMsg) msg);
        } else if (msg instanceof APIRemoveHostRouteFromL3NetworkMsg) {
            validate((APIRemoveHostRouteFromL3NetworkMsg) msg);
        } else if (msg instanceof APIAddIpv6RangeByNetworkCidrMsg) {
            validate((APIAddIpv6RangeByNetworkCidrMsg) msg);
        } else if (msg instanceof APIAddIpv6RangeMsg) {
            validate((APIAddIpv6RangeMsg) msg);
        } else if (msg instanceof APIDeleteIpRangeMsg) {
            validate((APIDeleteIpRangeMsg) msg);
        } else if (msg instanceof APISetL3NetworkMtuMsg) {
            validate((APISetL3NetworkMtuMsg) msg);
        } else if (msg instanceof APIAddReservedIpRangeMsg) {
            validate((APIAddReservedIpRangeMsg) msg);
        } else if (msg instanceof APIDeleteIpAddressMsg) {
            validate((APIDeleteIpAddressMsg) msg);
        }

        setServiceId(msg);

        return msg;
    }

    private void validate(APIDeleteIpAddressMsg msg) {
        for (String uuid : msg.getUsedIpUuids()) {
            UsedIpVO vo = dbf.findByUuid(uuid, UsedIpVO.class);
            if (vo.getVmNicUuid() != null) {
                throw new ApiMessageInterceptionException(argerr("could not delete ip address, " +
                        "because it's used by vmnic[uuid:%s]", vo.getVmNicUuid()));
            }
        }
    }

    private void validate(APIAddReservedIpRangeMsg msg) {
        L3NetworkVO l3NetworkVO = dbf.findByUuid(msg.getL3NetworkUuid(), L3NetworkVO.class);
        if (!NetworkUtils.isValidIPAddress(msg.getStartIp())) {
            throw new ApiMessageInterceptionException(argerr("could not reserve ip range, " +
                    "because start ip[%s] is not valid ip address", msg.getStartIp()));
        }

        if (!NetworkUtils.isValidIPAddress(msg.getEndIp())) {
            throw new ApiMessageInterceptionException(argerr("could not reserve ip range, " +
                    "because end ip[%s] is not valid ip address", msg.getEndIp()));
        }

        if (NetworkUtils.isIpv4Address(msg.getStartIp()) && !NetworkUtils.isIpv4Address(msg.getEndIp())) {
            throw new ApiMessageInterceptionException(argerr("could not reserve ip range, " +
                    "because end ip[%s] is not ipv4 address", msg.getEndIp()));
        }

        if (IPv6NetworkUtils.isIpv6Address(msg.getStartIp()) && !IPv6NetworkUtils.isIpv6Address(msg.getEndIp())) {
            throw new ApiMessageInterceptionException(argerr("could not reserve ip range, " +
                    "because end ip[%s] is not ipv6 address", msg.getEndIp()));
        }

        if (!IPv6NetworkUtils.isValidIpRange(msg.getStartIp(), msg.getEndIp())) {
            throw new ApiMessageInterceptionException(argerr("could not reserve ip range, " +
                    "because end ip[%s] is less than start ip[%s]", msg.getEndIp(), msg.getStartIp()));
        }

        if (NetworkUtils.isIpv4Address(msg.getStartIp())) {
            List<IpRangeVO> ipv4Ranges = l3NetworkVO.getIpRanges().stream()
                    .filter(ipr -> (ipr.getIpVersion() == IPv6Constants.IPv4))
                    .collect(Collectors.toList());
            if (ipv4Ranges.isEmpty()) {
                throw new ApiMessageInterceptionException(argerr("could not reserve ip range, " +
                        "because there is no ipv4 range"));
            }

            if (!NetworkUtils.isIpv4InCidr(msg.getStartIp(), ipv4Ranges.get(0).getNetworkCidr()) ||
                    !NetworkUtils.isIpv4InCidr(msg.getEndIp(), ipv4Ranges.get(0).getNetworkCidr())) {
                throw new ApiMessageInterceptionException(argerr("could not reserve ip range, " +
                        "because reserve ip is not in ip range[%s]", ipv4Ranges.get(0).getNetworkCidr()));
            }

            List<ReservedIpRangeVO> reservedIpv4Ranges = l3NetworkVO.getReservedIpRanges().stream()
                    .filter(ipr -> ipr.getIpVersion() == IPv6Constants.IPv4)
                    .collect(Collectors.toList());
            for (ReservedIpRangeVO reserveRange : reservedIpv4Ranges) {
                if (NetworkUtils.isIpv4RangeOverlap(msg.getStartIp(), msg.getEndIp(),
                        reserveRange.getStartIp(), reserveRange.getEndIp())) {
                    throw new ApiMessageInterceptionException(argerr("could not reserve ip range, " +
                                    "because new range [%s:%s] is overlapped with old range [%s:%s]",
                            msg.getStartIp(), msg.getEndIp(),
                            reserveRange.getStartIp(), reserveRange.getEndIp()));
                }
            }
        }

        if (IPv6NetworkUtils.isIpv6Address(msg.getStartIp())) {
            List<IpRangeVO> ipv6Ranges = l3NetworkVO.getIpRanges().stream()
                    .filter(ipr -> ipr.getIpVersion() == IPv6Constants.IPv6)
                    .collect(Collectors.toList());
            if (ipv6Ranges.isEmpty()) {
                throw new ApiMessageInterceptionException(argerr("could not reserve ip range, " +
                        "because there is no ipv6 range"));
            }

            if (!IPv6NetworkUtils.isIpv6InCidrRange(msg.getStartIp(), ipv6Ranges.get(0).getNetworkCidr()) ||
                    !IPv6NetworkUtils.isIpv6InCidrRange(msg.getEndIp(), ipv6Ranges.get(0).getNetworkCidr())) {
                throw new ApiMessageInterceptionException(argerr("could not reserve ip range, " +
                        "because reserve ip is not in ip range[%s]", ipv6Ranges.get(0).getNetworkCidr()));
            }

            List<ReservedIpRangeVO> reservedIpv6Ranges = l3NetworkVO.getReservedIpRanges().stream()
                    .filter(ipr -> ipr.getIpVersion() == IPv6Constants.IPv6)
                    .collect(Collectors.toList());
            for (ReservedIpRangeVO reserveRange : reservedIpv6Ranges) {
                if (IPv6NetworkUtils.isIpv6RangeOverlap(msg.getStartIp(), msg.getEndIp(),
                        reserveRange.getStartIp(), reserveRange.getEndIp())) {
                    throw new ApiMessageInterceptionException(argerr("could not reserve ip range, " +
                                    "because new range [%s:%s] is overlapped with old range [%s:%s]",
                            msg.getStartIp(), msg.getEndIp(),
                            reserveRange.getStartIp(), reserveRange.getEndIp()));
                }
            }
        }
    }

    private void validate(APISetL3NetworkMtuMsg msg) {
        L3NetworkVO l3Vo = dbf.findByUuid(msg.getL3NetworkUuid(), L3NetworkVO.class);
        L2NetworkVO l2VO = dbf.findByUuid(l3Vo.getL2NetworkUuid(), L2NetworkVO.class);

        if (!l2VO.getType().equals(L2NetworkConstant.L2_VLAN_NETWORK_TYPE)) {
            return;
        }

        /* when set mtu to vlan network, no vlan network mtu must be first */
        List<L2NetworkVO> novlanL2Vos = Q.New(L2NetworkVO.class).eq(L2NetworkVO_.physicalInterface, l2VO.getPhysicalInterface())
                .eq(L2NetworkVO_.type, L2NetworkConstant.L2_NO_VLAN_NETWORK_TYPE).list();
        if (novlanL2Vos.isEmpty()) {
            Integer defaultMtu = NetworkServiceGlobalConfig.DHCP_MTU_NO_VLAN.value(Integer.class);
            if (msg.getMtu() > defaultMtu) {
                throw new ApiMessageInterceptionException(argerr("could not set mtu because l2 network[uuid:%s] of " +
                        "l3 network [uuid:%s] mtu can not be bigger than the novlan network", l2VO.getUuid(), msg.getL3NetworkUuid()));
            }
        }

        /* in a cluster, there should only 1 no vlan network of same physical interface */
        Map<String, L2NetworkVO> novlanMap = new HashMap<>();
        for (L2NetworkVO noVlanNetwork : novlanL2Vos) {
            for (String cluster : noVlanNetwork.getAttachedClusterRefs().stream().map(L2NetworkClusterRefVO::getClusterUuid).collect(Collectors.toList())) {
                novlanMap.put(cluster, noVlanNetwork);
            }
        }

        Integer noVlanMax = null;
        List<String> vlanClusters = l2VO.getAttachedClusterRefs().stream().map(L2NetworkClusterRefVO::getClusterUuid).collect(Collectors.toList());
        for (String vlanCluster : vlanClusters) {
            L2NetworkVO novlanL2 = novlanMap.get(vlanCluster);
            Integer mtu;
            if (novlanL2 == null) {
                mtu = NetworkServiceGlobalConfig.DHCP_MTU_NO_VLAN.value(Integer.class);
            } else {
                mtu = new MtuGetter().getL2Mtu(L2NetworkInventory.valueOf(novlanL2));
            }

            if (noVlanMax == null) {
                noVlanMax = mtu;
            } else if (mtu < noVlanMax) {
                noVlanMax = mtu;
            }
        }

        if (noVlanMax != null && msg.getMtu() > noVlanMax) {
            throw new ApiMessageInterceptionException(argerr("could not set mtu because l2 network[uuid:%s] of " +
                    "l3 network [uuid:%s] mtu can not be bigger than the novlan network", l2VO.getUuid(), msg.getL3NetworkUuid()));
        }
    }

    private void validate(APIDeleteIpRangeMsg msg) {
        NormalIpRangeVO ipr = dbf.findByUuid(msg.getUuid(), NormalIpRangeVO.class);
        if (ipr == null || ipr.getIpVersion() != IPv6Constants.IPv4) {
            return;
        }

        /* address pool only related to Ipv4 */
        long normaCnt = Q.New(NormalIpRangeVO.class).eq(NormalIpRangeVO_.l3NetworkUuid, ipr.getL3NetworkUuid())
                .eq(NormalIpRangeVO_.ipVersion, IPv6Constants.IPv4).count();
        long addressPoolCnt = Q.New(AddressPoolVO.class).eq(AddressPoolVO_.l3NetworkUuid, ipr.getL3NetworkUuid()).count();
        if (addressPoolCnt > 0 && normaCnt == 1) {
            throw new ApiMessageInterceptionException(argerr("can not delete the last normal ip range because there is still has address pool"));
        }
    }


    private void validate(APIUpdateL3NetworkMsg msg) {
        if (msg.getCategory() == null && msg.getSystem() == null) {
            return;
        }

        Boolean currentSystem = Q.New(L3NetworkVO.class)
                .select(L3NetworkVO_.system)
                .eq(L3NetworkVO_.uuid, msg.getL3NetworkUuid())
                .findValue();
        if (msg.getSystem() != null && msg.getCategory() == null && !msg.getSystem().equals(currentSystem)) {
            throw new ApiMessageInterceptionException(argerr("you must update system and category both"));
        }

        List<L3NetworkCategory> validNetworkCategory = Arrays.asList(L3NetworkCategory.values());
        for (L3NetworkCategory category : validNetworkCategory) {
            if (category.toString().equalsIgnoreCase(msg.getCategory())) {
                msg.setCategory(category.toString());
                break;
            }
        }
        if (msg.getSystem() == null) {
            msg.setSystem(currentSystem);
        }
        if (msg.getCategory() != null) {
            if (L3NetworkCategory.checkSystemAndCategory(msg.getSystem(), L3NetworkCategory.valueOf(msg.getCategory()))) {
                return;
            } else {
                throw new ApiMessageInterceptionException(argerr("not valid combination of system and category," +
                        "only %s are valid", L3NetworkCategory.validCombination));
            }
        }
    }

    private void validate(APISetL3NetworkRouterInterfaceIpMsg msg) {
        if (!NetworkUtils.isIpv4Address(msg.getRouterInterfaceIp())) {
            throw new ApiMessageInterceptionException(argerr("invalid IP[%s]", msg.getRouterInterfaceIp()));
        }
        /* this API only related ipv4 */
        List<NormalIpRangeVO> ipRangeVOS = Q.New(NormalIpRangeVO.class).eq(NormalIpRangeVO_.l3NetworkUuid, msg.getL3NetworkUuid())
                .eq(NormalIpRangeVO_.ipVersion, IPv6Constants.IPv4).list();
        if (ipRangeVOS == null || ipRangeVOS.isEmpty()) {
            throw new ApiMessageInterceptionException(argerr("no ip range in l3[%s]", msg.getL3NetworkUuid()));
        }
        for (NormalIpRangeVO ipr : ipRangeVOS) {
            if (!NetworkUtils.isIpv4InCidr(msg.getRouterInterfaceIp(), ipr.getNetworkCidr())) {
                throw new ApiMessageInterceptionException(argerr("ip[%s] is not in the cidr of ip range[uuid:%s, cidr:%s] which l3 network[%s] attached",
                        msg.getRouterInterfaceIp(), ipr.getUuid(), ipr.getNetworkCidr(), msg.getL3NetworkUuid()));
            }
            if (NetworkUtils.isInRange(msg.getRouterInterfaceIp(), ipr.getStartIp(), ipr.getEndIp())) {
                throw new ApiMessageInterceptionException(argerr("ip[%s] in ip range[uuid:%s, startIp:%s, endIp:%s] which l3 network[%s] attached, this is not allowed",
                        msg.getRouterInterfaceIp(), ipr.getUuid(), ipr.getStartIp(), ipr.getEndIp(), msg.getL3NetworkUuid()));
            }
        }
    }

    private void validate(APICheckIpAvailabilityMsg msg) {
        if (!NetworkUtils.isValidIPAddress(msg.getIp())) {
            throw new ApiMessageInterceptionException(argerr("invalid IP[%s]", msg.getIp()));
        }
    }

    private void validate(APIGetFreeIpMsg msg) {
        if (msg.getIpRangeUuid() == null && msg.getL3NetworkUuid() == null) {
            throw new ApiMessageInterceptionException(argerr(
                    "ipRangeUuid and l3NetworkUuid cannot both be null; you must set either one."
            ));
        }

        if (msg.getIpRangeUuid() != null && msg.getL3NetworkUuid() == null) {
            IpRangeVO ipRangeVO = Q.New(IpRangeVO.class)
                    .eq(IpRangeVO_.uuid, msg.getIpRangeUuid())
                    .find();
            msg.setL3NetworkUuid(ipRangeVO.getL3NetworkUuid());
            msg.setIpVersion(ipRangeVO.getIpVersion());
        }

        if (msg.getLimit() < 0) {
            msg.setLimit(Integer.MAX_VALUE);
        }

        if (msg.getIpVersion() == null) {
            if (msg.getL3NetworkUuid() != null) {
                int l3Version = Q.New(L3NetworkVO.class).eq(L3NetworkVO_.uuid, msg.getL3NetworkUuid())
                        .select(L3NetworkVO_.ipVersion).findValue();
                msg.setIpVersion(l3Version);
            } else {
                msg.setIpVersion(IPv6Constants.IPv4);
            }
        }

        if (msg.getStart() != null) {
            if (msg.getIpVersion() == IPv6Constants.DUAL_STACK) {
                throw new ApiMessageInterceptionException(argerr("could not get free ip with start[ip:%s],because l3Network[uuid:%s] is dual stack", msg.getStart(), msg.getL3NetworkUuid()));
            } else if (msg.getIpVersion() == IPv6Constants.IPv4 && !NetworkUtils.isIpv4Address(msg.getStart())) {
                throw new ApiMessageInterceptionException(argerr("could not get free ip with start[ip:%s],because start[ip:%s] is not a correct ipv4 address", msg.getStart(), msg.getStart()));
            } else if (msg.getIpVersion() == IPv6Constants.IPv6 && !IPv6NetworkUtils.isIpv6Address(msg.getStart())) {
                throw new ApiMessageInterceptionException(argerr("could not get free ip with start[ip:%s],because start[ip:%s] is not a correct ipv6 address", msg.getStart(), msg.getStart()));
            }
        }
    }

    private void validate(APIAddIpv6RangeByNetworkCidrMsg msg) {
        if (!IPv6NetworkUtils.isValidUnicastNetworkCidr(msg.getNetworkCidr())) {
            throw new ApiMessageInterceptionException(argerr("%s is not a valid network cidr", msg.getNetworkCidr()));
        }

        if (msg.getIpRangeType() == null) {
            msg.setIpRangeType(IpRangeType.Normal.toString());
        }

        IpRangeInventory ipr = IpRangeInventory.fromMessage(msg);
        validateIpv6Range(ipr);
    }

    private void validate(APIAddIpv6RangeMsg msg) {
        if (!IPv6NetworkUtils.isIpv6UnicastAddress(msg.getStartIp())) {
            throw new ApiMessageInterceptionException(argerr("%s is not a valid ipv6 address", msg.getStartIp()));
        }

        if (!IPv6NetworkUtils.isIpv6UnicastAddress(msg.getEndIp())) {
            throw new ApiMessageInterceptionException(argerr("%s is not a valid ipv6 address", msg.getEndIp()));
        }

        if (!IPv6NetworkUtils.isIpv6UnicastAddress(msg.getGateway())) {
            throw new ApiMessageInterceptionException(argerr("%s is not a valid ipv6 address", msg.getGateway()));
        }

        if (!IPv6NetworkUtils.isValidUnicastIpv6Range(msg.getStartIp(), msg.getEndIp(), msg.getGateway(), msg.getPrefixLen())) {
            throw new ApiMessageInterceptionException(argerr("[startIp %s, endIp %s, prefixLen %d, gateway %s] is not a valid ipv6 range",
                    msg.getStartIp(), msg.getEndIp(), msg.getPrefixLen(), msg.getGateway()));
        }

        if (msg.getIpRangeType() == null) {
            msg.setIpRangeType(IpRangeType.Normal.toString());
        }

        /* normal ip range must has netmask and gateway */
        if (msg.getIpRangeType().equals(IpRangeType.Normal.toString())) {
            if (msg.getGateway() == null) {
                throw new ApiMessageInterceptionException(argerr("adding normal ip range must specify gateway ip address"));
            }
        }

        if (msg.getIpRangeType().equals(IpRangeType.AddressPool.toString())) {
            throw new ApiMessageInterceptionException(argerr("can not add ip range, because ipv6 address pool is not supported"));
            /* fake gateway
            msg.setGateway(msg.getStartIp()); */
        }

        IpRangeInventory ipr = IpRangeInventory.fromMessage(msg);
        validateIpv6Range(ipr);
    }

    private void validateIpv6Range(IpRangeInventory ipr) {
        if (ipr.getPrefixLen() > IPv6Constants.IPV6_PREFIX_LEN_MAX || ipr.getPrefixLen() < IPv6Constants.IPV6_PREFIX_LEN_MIN) {
            throw new ApiMessageInterceptionException(argerr("ip range prefix length is out of range [%d - %d] ",
                    IPv6Constants.IPV6_PREFIX_LEN_MIN, IPv6Constants.IPV6_PREFIX_LEN_MAX));
        }

        L3NetworkVO l3Vo = Q.New(L3NetworkVO.class).eq(L3NetworkVO_.uuid, ipr.getL3NetworkUuid()).find();

        if (l3Vo.getCategory().equals(L3NetworkCategory.System)) {
            throw new ApiMessageInterceptionException(argerr("can not add ip range, because system network doesn't support ipv6 yet"));
        }

        List<NormalIpRangeVO> rangeVOS = Q.New(NormalIpRangeVO.class).eq(NormalIpRangeVO_.l3NetworkUuid, ipr.getL3NetworkUuid()).eq(NormalIpRangeVO_.ipVersion, IPv6Constants.IPv6).list();
        if (rangeVOS != null && !rangeVOS.isEmpty()) {
            if (!rangeVOS.get(0).getAddressMode().equals(ipr.getAddressMode())) {
                throw new ApiMessageInterceptionException(argerr("addressMode[%s] is different from L3Netowork address mode[%s]", ipr.getAddressMode(),
                        rangeVOS.get(0).getAddressMode()));
            }
        }

        if (!ipr.getAddressMode().equals(IPv6Constants.Stateful_DHCP) && ipr.getPrefixLen() != IPv6Constants.IPV6_STATELESS_PREFIX_LEN) {
            throw new ApiMessageInterceptionException(argerr("ipv6 prefix length must be %d for Stateless-DHCP or SLAAC", IPv6Constants.IPV6_STATELESS_PREFIX_LEN));
        }

        List<String> l3Uuids = Q.New(L3NetworkVO.class).eq(L3NetworkVO_.l2NetworkUuid, l3Vo.getL2NetworkUuid()).select(L3NetworkVO_.uuid).listValues();
        SimpleQuery<NormalIpRangeVO> q = dbf.createQuery(NormalIpRangeVO.class);
        q.add(NormalIpRangeVO_.l3NetworkUuid, Op.IN, l3Uuids);
        q.add(NormalIpRangeVO_.ipVersion, Op.EQ, IPv6Constants.IPv6);
        List<NormalIpRangeVO> ranges = q.list();
        for (NormalIpRangeVO r : ranges) {
            if (IPv6NetworkUtils.isIpv6RangeOverlap(ipr.getStartIp(), ipr.getEndIp(), r.getStartIp(), r.getEndIp())) {
                throw new ApiMessageInterceptionException(argerr("new ip range [startip :%s, endip :%s] is overlaped with old ip range[startip :%s, endip :%s]",
                        ipr.getStartIp(), ipr.getEndIp(), r.getStartIp(), r.getEndIp()));
            }

            if (!r.getL3NetworkUuid().equals(ipr.getL3NetworkUuid())) {
                continue;
            }

            /* same l3 network can have only 1 cidr (exclude address pool iprange) */
            if ((ipr.getIpRangeType() != IpRangeType.AddressPool) && (!r.getNetworkCidr().equals(ipr.getNetworkCidr()))) {
                throw new ApiMessageInterceptionException(argerr("new network CIDR [%s] is different from old network cidr [%s]",
                        r.getNetworkCidr(), ipr.getNetworkCidr()));
            }
        }

        /* ipranges of same l3 network must have same gateway */
        List<NormalIpRangeVO> l3IpRanges = Q.New(NormalIpRangeVO.class).eq(NormalIpRangeVO_.l3NetworkUuid, ipr.getL3NetworkUuid())
                .eq(NormalIpRangeVO_.ipVersion, IPv6Constants.IPv6).list();
        for (NormalIpRangeVO r : l3IpRanges) {
            if (!r.getGateway().equals(ipr.getGateway())) {
                throw new ApiMessageInterceptionException(argerr("new add ip range gateway %s is different from old gateway %s", ipr.getGateway(), r.getGateway()));
            }
        }

        if (ipr.getIpRangeType() == IpRangeType.Normal) {
            if (NetworkUtils.isInIpv6Range(ipr.getStartIp(), ipr.getEndIp(), ipr.getGateway())) {
                throw new ApiMessageInterceptionException(argerr("gateway[%s] can not be part of range[%s, %s]", ipr.getGateway(), ipr.getStartIp(), ipr.getEndIp()));
            }
        }
    }

    private void validate(APIAddIpRangeByNetworkCidrMsg msg) {
        try {
            SubnetUtils utils = new SubnetUtils(msg.getNetworkCidr());
            utils.setInclusiveHostCount(false);
            SubnetInfo subnet = utils.getInfo();
            if (subnet.getAddressCount() == 0) {
                throw new ApiMessageInterceptionException(argerr("%s is not an allowed network cidr, because it doesn't have usable ip range", msg.getNetworkCidr()));
            }
            if (msg.getGateway() != null && !subnet.isInRange(msg.getGateway())) {
                throw new ApiMessageInterceptionException(argerr("the gateway[%s] is not in the subnet %s", msg.getGateway(), subnet.getCidrSignature()));
            }
        } catch (IllegalArgumentException e) {
            throw new ApiMessageInterceptionException(argerr("%s is not a valid network cidr", msg.getNetworkCidr()));
        }

        Boolean enableIPAM = Q.New(L3NetworkVO.class)
                .eq(L3NetworkVO_.uuid, msg.getL3NetworkUuid())
                .select(L3NetworkVO_.enableIPAM)
                .findValue();
        if (!enableIPAM) {
            throw new ApiMessageInterceptionException(argerr("IPAM is disabled for L3 network[uuid:%s], cannot add ip range", msg.getL3NetworkUuid()));
        }

        if (msg.getIpRangeType() == null) {
            msg.setIpRangeType(IpRangeType.Normal.toString());
        }

        List<IpRangeInventory> iprs = IpRangeInventory.fromMessage(msg);
        for (IpRangeInventory ipr : iprs) {
            validate(ipr);
        }
    }

    private void validate(APIGetIpAddressCapacityMsg msg) {
        boolean pass = false;
        if (msg.getIpRangeUuids() != null && !msg.getIpRangeUuids().isEmpty()) {
            pass = true;
        }
        if (msg.getL3NetworkUuids() != null && !msg.getL3NetworkUuids().isEmpty()) {
            pass = true;
        }
        if (msg.getZoneUuids() != null && !msg.getZoneUuids().isEmpty()) {
            pass = true;
        }

        if (!pass && !msg.isAll()) {
            throw new ApiMessageInterceptionException(argerr(
                    "ipRangeUuids, L3NetworkUuids, zoneUuids must have at least one be none-empty list, or all is set to true"
            ));
        }

        if (!CollectionUtils.isEmpty(msg.getL3NetworkUuids())) {
            List<String> enableIPAML3Uuids = Q.New(L3NetworkVO.class)
                    .eq(L3NetworkVO_.enableIPAM, true)
                    .in(L3NetworkVO_.uuid, msg.getL3NetworkUuids())
                    .select(L3NetworkVO_.uuid)
                    .listValues();

            if (enableIPAML3Uuids.isEmpty()) {
                throw new ApiMessageInterceptionException(argerr(
                        "all the specified L3 networks are IPAM disabled, cannot get ip address capacity"
                ));
            } else {
                msg.setL3NetworkUuids(enableIPAML3Uuids);
            }
        }

        if (msg.isAll() && (msg.getZoneUuids() == null || msg.getZoneUuids().isEmpty())) {
            SimpleQuery<ZoneVO> q = dbf.createQuery(ZoneVO.class);
            q.select(ZoneVO_.uuid);
            List<String> zuuids = q.listValue();
            msg.setZoneUuids(zuuids);

            if (msg.getZoneUuids().isEmpty()) {
                APIGetIpAddressCapacityReply reply = new APIGetIpAddressCapacityReply();
                bus.reply(msg, reply);
                throw new StopRoutingException();
            }
        }
    }

    private void validate(APICreateL3NetworkMsg msg) {
        if (!L3NetworkType.hasType(msg.getType())) {
            throw new ApiMessageInterceptionException(argerr("unsupported l3network type[%s]", msg.getType()));
        }

        if (msg.getDnsDomain() != null) {
            DomainValidator validator = DomainValidator.getInstance();
            if (!validator.isValid(msg.getDnsDomain())) {
                throw new ApiMessageInterceptionException(argerr("%s is not a valid domain name", msg.getDnsDomain()));
            }
        }

        List<L3NetworkCategory> validNetworkCategory = Arrays.asList(L3NetworkCategory.values());
        for (L3NetworkCategory category : validNetworkCategory) {
            if (category.toString().equalsIgnoreCase(msg.getCategory())) {
                msg.setCategory(category.toString());
                break;
            }
        }

        if (L3NetworkCategory.checkSystemAndCategory(msg.isSystem(), L3NetworkCategory.valueOf(msg.getCategory()))) {
            return;
        } else {
            throw new ApiMessageInterceptionException(argerr("not valid combination of system and category," +
                    "only %s are valid", L3NetworkCategory.validCombination));
        }
    }

    private void validate(APIRemoveDnsFromL3NetworkMsg msg) {
        SimpleQuery<L3NetworkDnsVO> q = dbf.createQuery(L3NetworkDnsVO.class);
        q.add(L3NetworkDnsVO_.dns, Op.EQ, msg.getDns());
        q.add(L3NetworkDnsVO_.l3NetworkUuid, Op.EQ, msg.getL3NetworkUuid());
        if (!q.isExists()) {
            APIRemoveDnsFromL3NetworkEvent evt = new APIRemoveDnsFromL3NetworkEvent(msg.getId());
            bus.publish(evt);
            throw new StopRoutingException();
        }
    }

    private void validate(APIDeleteL3NetworkMsg msg) {
        if (!dbf.isExist(msg.getUuid(), L3NetworkVO.class)) {
            APIDeleteL3NetworkEvent evt = new APIDeleteL3NetworkEvent(msg.getId());
            bus.publish(evt);
            throw new StopRoutingException();
        }

        for (MemorySnapshotValidatorExtensionPoint ext : pluginRgty.getExtensionList(MemorySnapshotValidatorExtensionPoint.class)) {
            ErrorCode errorCode = ext.checkL3IfReferencedByMemorySnapshot(msg.getL3NetworkUuid());
            if (errorCode != null) {
                throw new ApiMessageInterceptionException(errorCode);
            }
        }
    }

    private void validateAddressPool(IpRangeInventory ipr) {
        L3NetworkVO l3Vo = Q.New(L3NetworkVO.class).eq(L3NetworkVO_.uuid, ipr.getL3NetworkUuid()).find();
        List<String> l3Uuids = Q.New(L3NetworkVO.class).eq(L3NetworkVO_.l2NetworkUuid, l3Vo.getL2NetworkUuid()).select(L3NetworkVO_.uuid).listValues();
        SimpleQuery<AddressPoolVO> q = dbf.createQuery(AddressPoolVO.class);
        q.add(AddressPoolVO_.l3NetworkUuid, Op.IN, l3Uuids);
        q.add(AddressPoolVO_.ipVersion, Op.EQ, IPv6Constants.IPv4);
        List<AddressPoolVO> ranges = q.list();
        for (AddressPoolVO r : ranges) {
            if (NetworkUtils.isIpv4RangeOverlap(ipr.getStartIp(), ipr.getEndIp(), r.getStartIp(), r.getEndIp())) {
                throw new ApiMessageInterceptionException(argerr("overlap with ip range[uuid:%s, start ip:%s, end ip: %s]", r.getUuid(), r.getStartIp(), r.getEndIp()));
            }
        }
    }

    private void validate(IpRangeInventory ipr) {
        L3NetworkVO l3Vo = Q.New(L3NetworkVO.class).eq(L3NetworkVO_.uuid, ipr.getL3NetworkUuid()).find();

        if (ipr.getIpRangeType() == IpRangeType.AddressPool && l3Vo.getCategory() != L3NetworkCategory.Public) {
            throw new ApiMessageInterceptionException(argerr("l3 network [uuid %s: name %s] is not a public network, address pool range can not be added", l3Vo.getUuid(), l3Vo.getName()));
        }

        if (NetworkUtils.isIpv4RangeOverlap("224.0.0.0", "239.255.255.255", ipr.getStartIp(), ipr.getEndIp())) {
            throw new ApiMessageInterceptionException(argerr("the IP range[%s ~ %s] contains D class addresses which are for multicast", ipr.getStartIp(), ipr.getEndIp()));
        }

        if (NetworkUtils.isIpv4RangeOverlap("240.0.0.0", "255.255.255.255", ipr.getStartIp(), ipr.getEndIp())) {
            throw new ApiMessageInterceptionException(argerr("the IP range[%s ~ %s] contains E class addresses which are reserved", ipr.getStartIp(), ipr.getEndIp()));
        }

        if (NetworkUtils.isIpv4RangeOverlap("169.254.1.0", "169.254.254.255", ipr.getStartIp(), ipr.getEndIp())) {
            throw new ApiMessageInterceptionException(argerr("the IP range[%s ~ %s] contains link local addresses which are reserved", ipr.getStartIp(), ipr.getEndIp()));
        }

        SubnetUtils sub = new SubnetUtils(ipr.getStartIp(), ipr.getNetmask());
        SubnetInfo info = sub.getInfo();
        if (!info.isInRange(ipr.getGateway())) {
            throw new ApiMessageInterceptionException(argerr("the gateway[%s] is not in the subnet %s/%s", ipr.getGateway(), ipr.getStartIp(), ipr.getNetmask()));
        }

        if (ipr.getStartIp().equals(info.getNetworkAddress()) || ipr.getEndIp().equals(info.getBroadcastAddress())) {
            throw new ApiMessageInterceptionException(argerr(
                    "ip allocation can not contain network address or broadcast address")
            );
        }

        if (!NetworkUtils.isIpv4Address(ipr.getStartIp())) {
            throw new ApiMessageInterceptionException(argerr("start ip[%s] is not a IPv4 address", ipr.getStartIp()));
        }

        if (!NetworkUtils.isIpv4Address(ipr.getEndIp())) {
            throw new ApiMessageInterceptionException(argerr("end ip[%s] is not a IPv4 address", ipr.getEndIp()));
        }

        if (!NetworkUtils.isIpv4Address(ipr.getGateway())) {
            throw new ApiMessageInterceptionException(argerr("gateway[%s] is not a IPv4 address", ipr.getGateway()));
        }

        if (!NetworkUtils.isNetmaskExcept(ipr.getNetmask(), "0.0.0.0")) {
            throw new ApiMessageInterceptionException(argerr("netmask[%s] is not a netmask, and the IP range netmask cannot be 0.0.0.0", ipr.getNetmask()));
        }

        long startip = NetworkUtils.ipv4StringToLong(ipr.getStartIp());
        long endip = NetworkUtils.ipv4StringToLong(ipr.getEndIp());
        if (startip > endip) {
            throw new ApiMessageInterceptionException(argerr("start ip[%s] is behind end ip[%s]", ipr.getStartIp(), ipr.getEndIp()));
        }

        String cidr = ipr.toSubnetUtils().getInfo().getCidrSignature();
        List<String> l3Uuids = Q.New(L3NetworkVO.class).eq(L3NetworkVO_.l2NetworkUuid, l3Vo.getL2NetworkUuid()).select(L3NetworkVO_.uuid).listValues();
        SimpleQuery<IpRangeVO> q = dbf.createQuery(IpRangeVO.class);
        q.add(IpRangeVO_.l3NetworkUuid, Op.IN, l3Uuids);
        q.add(IpRangeVO_.ipVersion, Op.EQ, IPv6Constants.IPv4);
        List<IpRangeVO> ranges = q.list();
        for (IpRangeVO r : ranges) {
            if (NetworkUtils.isIpv4RangeOverlap(ipr.getStartIp(), ipr.getEndIp(), r.getStartIp(), r.getEndIp())) {
                throw new ApiMessageInterceptionException(argerr("overlap with ip range[uuid:%s, start ip:%s, end ip: %s]", r.getUuid(), r.getStartIp(), r.getEndIp()));
            }

            if (!r.getL3NetworkUuid().equals(ipr.getL3NetworkUuid())) {
                continue;
            }

            if (ipr.getIpRangeType() == IpRangeType.Normal) {
                /* normal ip ranges must in same cidr */
                if (!Q.New(NormalIpRangeVO.class).eq(NormalIpRangeVO_.uuid, r.getUuid()).isExists()) {
                    continue;
                }

                /* same l3 network can have only 1 cidr */
                String rcidr = IpRangeInventory.valueOf(r).toSubnetUtils().getInfo().getCidrSignature();
                if (!cidr.equals(rcidr)) {
                    throw new ApiMessageInterceptionException(argerr("multiple CIDR on the same L3 network is not allowed. There has been a IP" +
                                    " range[uuid:%s, CIDR:%s], the new IP range[CIDR:%s] is not in the CIDR with the existing one",
                            r.getUuid(), rcidr, cidr));
                }
            }
        }

        /* normal ip ranges of same l3 network must have same gateway */
        if (ipr.getIpRangeType() == IpRangeType.Normal) {
            if (!info.isInRange(ipr.getEndIp())) {
                throw new ApiMessageInterceptionException(argerr("the endip[%s] is not in the subnet %s/%s", ipr.getEndIp(), ipr.getStartIp(), ipr.getNetmask()));
            }

            long gw = NetworkUtils.ipv4StringToLong(ipr.getGateway());
            if (startip <= gw && gw <= endip) {
                throw new ApiMessageInterceptionException(argerr("gateway[%s] can not be part of range[%s, %s]", ipr.getGateway(), ipr.getStartIp(), ipr.getEndIp()));
            }

            List<NormalIpRangeVO> l3IpRanges = Q.New(NormalIpRangeVO.class).eq(NormalIpRangeVO_.l3NetworkUuid, ipr.getL3NetworkUuid())
                    .eq(NormalIpRangeVO_.ipVersion, IPv6Constants.IPv4).list();
            for (NormalIpRangeVO r : l3IpRanges) {
                if (!r.getGateway().equals(ipr.getGateway())) {
                    throw new ApiMessageInterceptionException(argerr("new add ip range gateway %s is different from old gateway %s", ipr.getGateway(), r.getGateway()));
                }
            }
        } else if (ipr.getIpRangeType() == IpRangeType.AddressPool) {
            validateAddressPool(ipr);
        }
    }

    private void validate(APIAddIpRangeMsg msg) {
        Boolean enableIPAM = Q.New(L3NetworkVO.class)
                .eq(L3NetworkVO_.uuid, msg.getL3NetworkUuid())
                .select(L3NetworkVO_.enableIPAM)
                .findValue();
        if (!enableIPAM) {
            throw new ApiMessageInterceptionException(argerr("IPAM is disabled for L3 network[uuid:%s], cannot add ip range", msg.getL3NetworkUuid()));
        }

        if (msg.getIpRangeType() == null) {
            msg.setIpRangeType(IpRangeType.Normal.toString());
        }
        /* normal ip range must has netmask and gateway */
        if (msg.getIpRangeType().equals(IpRangeType.Normal.toString())) {
            if (msg.getGateway() == null) {
                throw new ApiMessageInterceptionException(argerr("adding normal ip range must specify gateway ip address"));
            }
        }

        if (msg.getIpRangeType().equals(IpRangeType.AddressPool.toString())) {
            /* fake gateway */
            msg.setGateway(msg.getStartIp());
        }

        IpRangeInventory ipr = IpRangeInventory.fromMessage(msg);
        validate(ipr);
    }

    private void validate(APIAddDnsToL3NetworkMsg msg) {
        if (NetworkUtils.isNotIpAddress(msg.getDns())) {
            throw new ApiMessageInterceptionException(argerr("dns[%s] is not a IP address", msg.getDns()));
        }

        List<L3NetworkDnsVO> l3NetworkDnsVOS = Q.New(L3NetworkDnsVO.class).eq(L3NetworkDnsVO_.l3NetworkUuid, msg.getL3NetworkUuid()).list();
        if (l3NetworkDnsVOS.isEmpty()) {
            return;
        }

        if (NetworkUtils.isIpv4Address(msg.getDns())) {
            boolean exist = l3NetworkDnsVOS.stream().anyMatch(l3NetworkDnsVO -> msg.getDns().equals(l3NetworkDnsVO.getDns()));
            if (exist) {
                throw new ApiMessageInterceptionException(operr("there has been a DNS[%s] on L3 network[uuid:%s]", msg.getDns(), msg.getL3NetworkUuid()));
            }
        } else {
            for (L3NetworkDnsVO l3NetworkDnsVO : l3NetworkDnsVOS) {
                if (!IPv6NetworkUtils.isIpv6Address(l3NetworkDnsVO.getDns())) {
                    continue;
                }
                if (IPv6Address.fromString(msg.getDns()).toBigInteger().equals(IPv6Address.fromString(l3NetworkDnsVO.getDns()).toBigInteger())) {
                    throw new ApiMessageInterceptionException(operr("there has been a DNS[%s] on L3 network[uuid:%s]", msg.getDns(), msg.getL3NetworkUuid()));
                }
            }
        }
    }

    private void validate(APIAddHostRouteToL3NetworkMsg msg) {
        if (!NetworkUtils.isCidr(msg.getPrefix())) {
            throw new ApiMessageInterceptionException(argerr("prefix [%s] is not a IPv4 network cidr", msg.getL3NetworkUuid()));
        }

        if (!NetworkUtils.isIpv4Address(msg.getNexthop())) {
            throw new ApiMessageInterceptionException(argerr("nexthop[%s] is not a IPv4 address", msg.getNexthop()));
        }

        SimpleQuery<L3NetworkHostRouteVO> q = dbf.createQuery(L3NetworkHostRouteVO.class);
        q.add(L3NetworkHostRouteVO_.l3NetworkUuid, Op.EQ, msg.getL3NetworkUuid());
        q.add(L3NetworkHostRouteVO_.prefix, Op.EQ, msg.getPrefix());
        if (q.isExists()) {
            throw new ApiMessageInterceptionException(operr("there has been a hostroute for prefix[%s] on L3 network[uuid:%s]", msg.getPrefix(), msg.getL3NetworkUuid()));
        }
    }

    private void validate(APIRemoveHostRouteFromL3NetworkMsg msg) {
        if (!NetworkUtils.isCidr(msg.getPrefix())) {
            throw new ApiMessageInterceptionException(argerr("prefix [%s] is not a IPv4 network cidr", msg.getL3NetworkUuid()));
        }

        SimpleQuery<L3NetworkHostRouteVO> q = dbf.createQuery(L3NetworkHostRouteVO.class);
        q.add(L3NetworkHostRouteVO_.l3NetworkUuid, Op.EQ, msg.getL3NetworkUuid());
        q.add(L3NetworkHostRouteVO_.prefix, Op.EQ, msg.getPrefix());
        if (!q.isExists()) {
            throw new ApiMessageInterceptionException(operr("there is no hostroute for prefix[%s] on L3 network[uuid:%s]", msg.getPrefix(), msg.getL3NetworkUuid()));
        }
    }
}
