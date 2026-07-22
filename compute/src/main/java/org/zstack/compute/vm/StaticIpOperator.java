package org.zstack.compute.vm;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.message.APIMessage;
import org.zstack.header.network.l3.*;
import org.zstack.header.tag.SystemTagVO;
import org.zstack.header.tag.SystemTagVO_;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmNicParam;
import org.zstack.network.l3.L3NetworkManager;
import org.zstack.tag.SystemTagCreator;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.TagUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.NicIpAddressInfo;
import org.zstack.utils.network.IPv6Constants;
import org.zstack.utils.network.IPv6NetworkUtils;
import org.zstack.utils.network.NetworkUtils;

import javax.persistence.Tuple;
import java.util.*;

import static org.zstack.core.Platform.*;
import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

/**
 * Created by xing5 on 2016/5/25.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class StaticIpOperator {
    private static final CLogger logger = Utils.getLogger(StaticIpOperator.class);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private L3NetworkManager l3Mgr;

    public Map<String, List<String>> getStaticIpByVmUuid(String vmUuid) {
        Map<String, List<String>> ret = new HashMap<>();

        List<Map<String, String>> tokenList = VmSystemTags.STATIC_IP.getTokensOfTagsByResourceUuid(vmUuid);
        for (Map<String, String> tokens : tokenList) {
            String l3Uuid = tokens.get(VmSystemTags.STATIC_IP_L3_UUID_TOKEN);
            String ip = tokens.get(VmSystemTags.STATIC_IP_TOKEN);
            ip = IPv6NetworkUtils.ipv6TagValueToAddress(ip);
            ret.computeIfAbsent(l3Uuid, k -> new ArrayList<>()).add(ip);
        }

        return ret;
    }

    public Map<String, List<String>> getStaticIpByNicIpAddressInfo(Map<String, NicIpAddressInfo> infoMap) {
        Map<String, List<String>> ret = new HashMap<>();
        if (MapUtils.isEmpty(infoMap)) {
            return ret;
        }

        for (Map.Entry<String, NicIpAddressInfo> e : infoMap.entrySet()) {
            String l3Uuid = e.getKey();
            NicIpAddressInfo info = e.getValue();
            if (!StringUtils.isEmpty(info.ipv4Address)) {
                ret.computeIfAbsent(l3Uuid, k -> new ArrayList<>()).add(info.ipv4Address);
            }
            if (!StringUtils.isEmpty(info.ipv6Address)) {
                ret.computeIfAbsent(l3Uuid, k -> new ArrayList<>()).add(info.ipv6Address);
            }
        }

        return ret;
    }

    public Map<String, NicIpAddressInfo> getNicNetworkInfoByVmUuid(String vmUuid) {
        return getNicNetworkInfoBySystemTag(Q.New(SystemTagVO.class).select(SystemTagVO_.tag)
                .eq(SystemTagVO_.resourceUuid, vmUuid).listValues());
    }

    public Map<String, NicIpAddressInfo> getNicNetworkInfoBySystemTag(List<String> systemTags) {
        Map<String, NicIpAddressInfo> ret = new HashMap<>();
        if (systemTags == null || systemTags.isEmpty()) {
            return ret;
        }

        for (String sysTag : systemTags) {
            if(VmSystemTags.STATIC_IP.isMatch(sysTag)) {
                Map<String, String> token = TagUtils.parse(VmSystemTags.STATIC_IP.getTagFormat(), sysTag);
                String l3Uuid = token.get(VmSystemTags.STATIC_IP_L3_UUID_TOKEN);
                ret.computeIfAbsent(l3Uuid, k -> new NicIpAddressInfo());
                String ip = token.get(VmSystemTags.STATIC_IP_TOKEN);
                ip = IPv6NetworkUtils.ipv6TagValueToAddress(ip);
                if (NetworkUtils.isIpv4Address(ip)) {
                    ret.get(l3Uuid).ipv4Address = ip;
                } else if (IPv6NetworkUtils.isIpv6Address(ip)) {
                    ret.get(l3Uuid).ipv6Address = IPv6NetworkUtils.getIpv6AddressCanonicalString(ip);
                } else {
                    throw new ApiMessageInterceptionException(argerr("the static IP[%s] format error", ip));
                }
            }
        }

        if (ret.isEmpty()) {
            return ret;
        }

        for (String sysTag : systemTags) {
            if(VmSystemTags.IPV4_GATEWAY.isMatch(sysTag)) {
                Map<String, String> token = TagUtils.parse(VmSystemTags.IPV4_GATEWAY.getTagFormat(), sysTag);
                String l3Uuid = token.get(VmSystemTags.IPV4_GATEWAY_L3_UUID_TOKEN);
                if (ret.get(l3Uuid) == null) {
                    continue;
                }
                ret.get(l3Uuid).ipv4Gateway = token.get(VmSystemTags.IPV4_GATEWAY_TOKEN);
            }
            if(VmSystemTags.IPV4_NETMASK.isMatch(sysTag)) {
                Map<String, String> token = TagUtils.parse(VmSystemTags.IPV4_NETMASK.getTagFormat(), sysTag);
                String l3Uuid = token.get(VmSystemTags.IPV4_NETMASK_L3_UUID_TOKEN);
                if (ret.get(l3Uuid) == null) {
                    continue;
                }
                ret.get(l3Uuid).ipv4Netmask = token.get(VmSystemTags.IPV4_NETMASK_TOKEN);
            }
            if(VmSystemTags.IPV6_GATEWAY.isMatch(sysTag)) {
                Map<String, String> token = TagUtils.parse(VmSystemTags.IPV6_GATEWAY.getTagFormat(), sysTag);
                String l3Uuid = token.get(VmSystemTags.IPV6_GATEWAY_L3_UUID_TOKEN);
                if (ret.get(l3Uuid) == null) {
                    continue;
                }
                ret.get(l3Uuid).ipv6Gateway = IPv6NetworkUtils.getIpv6AddressCanonicalString(
                        IPv6NetworkUtils.ipv6TagValueToAddress(token.get(VmSystemTags.IPV6_GATEWAY_TOKEN)));
            }
            if(VmSystemTags.IPV6_PREFIX.isMatch(sysTag)) {
                Map<String, String> token = TagUtils.parse(VmSystemTags.IPV6_PREFIX.getTagFormat(), sysTag);
                String l3Uuid = token.get(VmSystemTags.IPV6_PREFIX_L3_UUID_TOKEN);
                if (ret.get(l3Uuid) == null) {
                    continue;
                }
                ret.get(l3Uuid).ipv6Prefix = Integer.valueOf(token.get(VmSystemTags.IPV6_PREFIX_TOKEN));
            }
        }

        return ret;
    }

    public void updateNicNetworkInfoByVmNicParam(String vmUuid, Map<String, NicIpAddressInfo> infoMap, List<VmNicParam> vmNicParams) {
        if (CollectionUtils.isEmpty(vmNicParams)) {
            return;
        }

        for (VmNicParam vmNicParam : vmNicParams) {
            String l3Uuid = vmNicParam.getL3NetworkUuid();
            infoMap.compute(l3Uuid, (k, info) -> {
                boolean isNew = (info == null);
                info = isNew ? new NicIpAddressInfo() : info;

                boolean isSet = false;
                if (StringUtils.isEmpty(info.ipv4Address) && !StringUtils.isEmpty(vmNicParam.getIp())) {
                    info.ipv4Address = vmNicParam.getIp();
                    // set system tag for IPAM enabled l3
                    setStaticIp(vmUuid, l3Uuid, info.ipv4Address);

                    info.ipv4Netmask = vmNicParam.getNetmask();
                    info.ipv4Gateway = vmNicParam.getGateway();
                    NormalIpRangeVO ipRangeVO = Q.New(NormalIpRangeVO.class)
                            .eq(NormalIpRangeVO_.l3NetworkUuid, l3Uuid)
                            .eq(NormalIpRangeVO_.ipVersion, IPv6Constants.IPv4)
                            .limit(1).find();

                    if (ipRangeVO != null) {
                        if (StringUtils.isEmpty(info.ipv4Netmask)) {
                            info.ipv4Netmask = ipRangeVO.getNetmask();
                        }

                        if (StringUtils.isEmpty(info.ipv4Gateway)) {
                            info.ipv4Gateway = ipRangeVO.getGateway();
                        }
                    }
                    isSet = true;
                }
                if (StringUtils.isEmpty(info.ipv6Address) && !StringUtils.isEmpty(vmNicParam.getIp6())) {
                    info.ipv6Address = IPv6NetworkUtils.getIpv6AddressCanonicalString(vmNicParam.getIp6());
                    // set system tag for IPAM enabled l3
                    setStaticIp(vmUuid, l3Uuid, info.ipv6Address);

                    info.ipv6Prefix = vmNicParam.getIpv6Prefix();
                    if (vmNicParam.getIpv6Gateway() != null) {
                        info.ipv6Gateway = IPv6NetworkUtils.getIpv6AddressCanonicalString(vmNicParam.getIpv6Gateway());
                    }

                    NormalIpRangeVO ipRangeVO = Q.New(NormalIpRangeVO.class)
                            .eq(NormalIpRangeVO_.l3NetworkUuid, l3Uuid)
                            .eq(NormalIpRangeVO_.ipVersion, IPv6Constants.IPv6)
                            .limit(1).find();

                    if (ipRangeVO != null) {
                        if (info.ipv6Prefix == null) {
                            info.ipv6Prefix = ipRangeVO.getPrefixLen();
                        }

                        if (StringUtils.isEmpty(info.ipv6Gateway)) {
                            info.ipv6Gateway = IPv6NetworkUtils.getIpv6AddressCanonicalString(ipRangeVO.getGateway());
                        }
                    }
                    isSet = true;
                }

                return (isNew && !isSet) ? null : info;
            });
        }
    }

    public Map<String, List<String>> getStaticIpBySystemTag(List<String> systemTags) {
        Map<String, List<String>> ret = new HashMap<>();

        if (systemTags == null) {
            return ret;
        }

        for (String sysTag : systemTags) {
            if(!VmSystemTags.STATIC_IP.isMatch(sysTag)) {
                continue;
            }

            Map<String, String> token = TagUtils.parse(VmSystemTags.STATIC_IP.getTagFormat(), sysTag);
            String l3Uuid = token.get(VmSystemTags.STATIC_IP_L3_UUID_TOKEN);
            String ip = token.get(VmSystemTags.STATIC_IP_TOKEN);
            ip = IPv6NetworkUtils.ipv6TagValueToAddress(ip);
            ret.computeIfAbsent(l3Uuid, k -> new ArrayList<>()).add(ip);
        }

        return ret;
    }

    public void setStaticIp(String vmUuid, String l3Uuid, String ip) {
        SimpleQuery<SystemTagVO> q = dbf.createQuery(SystemTagVO.class);
        q.select(SystemTagVO_.uuid, SystemTagVO_.tag);
        q.add(SystemTagVO_.resourceType, Op.EQ, VmInstanceVO.class.getSimpleName());
        q.add(SystemTagVO_.resourceUuid, Op.EQ, vmUuid);
        q.add(SystemTagVO_.tag, Op.LIKE, TagUtils.tagPatternToSqlPattern(VmSystemTags.STATIC_IP.instantiateTag(
                map(e(VmSystemTags.STATIC_IP_L3_UUID_TOKEN, l3Uuid))
        )));
        final List<Tuple> tags = q.listTuple();

        String tagUuid = null;
        boolean isIpv4 = NetworkUtils.isIpv4Address(ip);
        if (tags != null && !tags.isEmpty()) {
            for (Tuple tag : tags) {
                Map<String, String> token = TagUtils.parse(VmSystemTags.STATIC_IP.getTagFormat(), (String)tag.get(1));
                String oldIp = token.get(VmSystemTags.STATIC_IP_TOKEN);
                oldIp = IPv6NetworkUtils.ipv6TagValueToAddress(oldIp);
                boolean isIpv4Tag = NetworkUtils.isIpv4Address(oldIp);
                if (isIpv4 == isIpv4Tag) { /* compare ip version */
                    tagUuid = (String) tag.get(0);
                    break;
                }
            }
        }

        /* '::' is token used by systemTag, replace with "--" */
        ip = IPv6NetworkUtils.ipv6AddessToTagValue(ip);
        if (tagUuid == null) {
            SystemTagCreator creator = VmSystemTags.STATIC_IP.newSystemTagCreator(vmUuid);
            creator.setTagByTokens(map(
                    e(VmSystemTags.STATIC_IP_L3_UUID_TOKEN, l3Uuid),
                    e(VmSystemTags.STATIC_IP_TOKEN, ip)
            ));
            creator.create();
        } else {
            VmSystemTags.STATIC_IP.updateByTagUuid(tagUuid, VmSystemTags.STATIC_IP.instantiateTag(map(
                    e(VmSystemTags.STATIC_IP_L3_UUID_TOKEN, l3Uuid),
                    e(VmSystemTags.STATIC_IP_TOKEN, ip)
            )));
        }
    }

    public void deleteStaticIpByVmUuidAndL3Uuid(String vmUuid, String l3Uuid) {
        VmSystemTags.STATIC_IP.delete(vmUuid, TagUtils.tagPatternToSqlPattern(VmSystemTags.STATIC_IP.instantiateTag(
                map(e(VmSystemTags.STATIC_IP_L3_UUID_TOKEN, l3Uuid))
        )));
    }

    public void deleteStaticIpByVmUuidAndL3Uuid(String vmUuid, String l3Uuid, String ip) {
        VmSystemTags.STATIC_IP.delete(vmUuid, TagUtils.tagPatternToSqlPattern(VmSystemTags.STATIC_IP.instantiateTag(
                map(e(VmSystemTags.STATIC_IP_L3_UUID_TOKEN, l3Uuid), e(VmSystemTags.STATIC_IP_TOKEN, ip))
        )));
    }

    public void deleteStaticIpByL3NetworkUuid(String l3Uuid) {
        VmSystemTags.STATIC_IP.delete(null, VmSystemTags.STATIC_IP.instantiateTag(map(
                e(VmSystemTags.STATIC_IP_L3_UUID_TOKEN, l3Uuid),
                e(VmSystemTags.STATIC_IP_TOKEN, "%")
        )));
    }

    public Map<Integer, String> getNicStaticIpMap(List<String> nicStaticIpList) {
        Map<Integer, String> nicStaticIpMap = new HashMap<>();
        if (nicStaticIpList != null) {
            for (String ip : nicStaticIpList) {
                if (NetworkUtils.isIpv4Address(ip)) {
                    nicStaticIpMap.put(IPv6Constants.IPv4, ip);
                } else {
                    nicStaticIpMap.put(IPv6Constants.IPv6, ip);
                }
            }
        }

        return nicStaticIpMap;
    }

    public Map<Integer, String> getNicStaticIpMap(List<String> nicStaticIpList, VmNicParam vmNicParam,
                                                  Set<Integer> vmNicParamIpVersions) {
        Map<Integer, String> nicStaticIpMap = getNicStaticIpMap(nicStaticIpList);
        if (vmNicParamIpVersions != null) {
            vmNicParamIpVersions.forEach(nicStaticIpMap::remove);
        }
        if (vmNicParam == null) {
            return nicStaticIpMap;
        }

        if (!StringUtils.isEmpty(vmNicParam.getIp())) {
            nicStaticIpMap.put(IPv6Constants.IPv4, vmNicParam.getIp());
        }
        if (!StringUtils.isEmpty(vmNicParam.getIp6())) {
            nicStaticIpMap.put(IPv6Constants.IPv6,
                    IPv6NetworkUtils.getIpv6AddressCanonicalString(vmNicParam.getIp6()));
        }

        return nicStaticIpMap;
    }

    public void setIpChange(String vmUuid, String l3Uuid) {
        SystemTagCreator creator = VmSystemTags.VM_IP_CHANGED.newSystemTagCreator(vmUuid);
        creator.recreate = true;
        creator.inherent = false;
        creator.setTagByTokens(map(
                e(VmSystemTags.VM_IP_CHANGED_TOKEN, l3Uuid)
        ));
        creator.create();
    }

    public void deleteIpChange(String vmUuid) {
        VmSystemTags.VM_IP_CHANGED.delete(vmUuid, VmInstanceVO.class);
    }

    public boolean isIpChange(String vmUuid, String l3Uuid) {
        List<Map<String, String>> tokenList = VmSystemTags.VM_IP_CHANGED.getTokensOfTagsByResourceUuid(vmUuid);
        for (Map<String, String> tokens : tokenList) {
            String uuid = tokens.get(VmSystemTags.VM_IP_CHANGED_TOKEN);
            if (uuid.equals(l3Uuid)) {
                return true;
            }
        }

        return false;
    }

    public void checkIpAvailability(L3NetworkVO l3, String ip) {
        if (!l3.getEnableIPAM()) {
            logger.debug(String.format("L3 network[uuid:%s] does not enable ip address management, skip checking IP availability",
                    l3.getUuid()));
            return;
        }

        L3NetworkFactory factory = l3Mgr.getL3NetworkFactory(L3NetworkType.valueOf(l3.getType()));
        L3Network nw = factory.getL3Network(l3);
        CheckIpAvailabilityStruct struct = new CheckIpAvailabilityStruct();
        struct.setIp(ip);
        struct.setL3NetworkUuid(l3.getUuid());
        CheckIpAvailabilityResult result = nw.checkIpAvailability(struct);

        if (!result.isAvailable()) {
            throw new ApiMessageInterceptionException(argerr("IP[%s] is not available on the L3 network[uuid:%s]",
                    ip, l3.getUuid())
                    .withException(result.getReason()));
        }
    }

    public void validateStaticIpTagsInApiMessage(APIMessage msg) {
        validateStaticIpTagsInApiMessage(msg, null, null);
    }

    public Map<String, NicIpAddressInfo> validateStaticIpTagsInApiMessage(APIMessage msg, String vmUuid, List<VmNicParam> vmNicParams) {
        Map<String, NicIpAddressInfo> staticIps = getNicNetworkInfoBySystemTag(msg.getSystemTags());
        if (vmUuid != null && !CollectionUtils.isEmpty(vmNicParams)) {
            updateNicNetworkInfoByVmNicParam(vmUuid, staticIps, vmNicParams);
        }
        if (staticIps.isEmpty()) {
            return staticIps;
        }

        List<L3NetworkVO> l3s = Q.New(L3NetworkVO.class)
                .in(L3NetworkVO_.uuid, staticIps.keySet())
                .list();
        List<String> newSysTags = new ArrayList<>();
        for (Map.Entry<String, NicIpAddressInfo> e : staticIps.entrySet()) {
            L3NetworkVO l3 = l3s.stream().filter(vo -> vo.getUuid().equals(e.getKey())).findFirst().orElse(null);
            if (l3 == null) {
                continue;
            }

            NicIpAddressInfo info = e.getValue();
            validateStaticIp(info, l3, newSysTags);
        }

        if (msg.getSystemTags() == null) {
            msg.setSystemTags(newSysTags);
        } else {
            msg.getSystemTags().addAll(newSysTags);
        }


        return staticIps;
    }

    public void validateStaticIp(NicIpAddressInfo info, L3NetworkVO l3, List<String> newSysTags) {
        String l3Uuid = l3.getUuid();

        if (!StringUtils.isEmpty(info.ipv4Address)) {
            if (!NetworkUtils.isIpv4Address(info.ipv4Address)) {
                throw new ApiMessageInterceptionException(argerr("ipv4 address[%s] is not valid", info.ipv4Address));
            }
            checkIpAvailability(l3, info.ipv4Address);
        }

        if (!StringUtils.isEmpty(info.ipv6Address)) {
            if (!IPv6NetworkUtils.isIpv6Address(info.ipv6Address)) {
                throw new ApiMessageInterceptionException(argerr("ipv6 address[%s] is not valid", info.ipv6Address));
            }
            info.ipv6Address = IPv6NetworkUtils.getIpv6AddressCanonicalString(info.ipv6Address);
            checkIpAvailability(l3, info.ipv6Address);
        }

        if (!StringUtils.isEmpty(info.ipv4Netmask)) {
            if (!NetworkUtils.isNetmask(info.ipv4Netmask)) {
                throw new ApiMessageInterceptionException(argerr("ipv4 netmask[%s] is not valid", info.ipv4Netmask));
            }
        }

        if (!StringUtils.isEmpty(info.ipv4Gateway)) {
            if (!NetworkUtils.isIpv4Address(info.ipv4Gateway)) {
                throw new ApiMessageInterceptionException(argerr("ipv4 gateway[%s] should be ipv4 address", info.ipv4Gateway));
            }
        }

        if (info.ipv6Prefix != null) {
            int prefixLen = info.ipv6Prefix;
            if (prefixLen > IPv6Constants.IPV6_PREFIX_LEN_MAX || prefixLen < IPv6Constants.IPV6_PREFIX_LEN_MIN) {
                throw new ApiMessageInterceptionException(argerr("ip range prefix length[%d] is out of range [%d - %d]",
                        prefixLen, IPv6Constants.IPV6_PREFIX_LEN_MIN, IPv6Constants.IPV6_PREFIX_LEN_MAX));
            }
        }

        if (!StringUtils.isEmpty(info.ipv6Gateway)) {
            if (!IPv6NetworkUtils.isIpv6Address(info.ipv6Gateway)) {
                throw new ApiMessageInterceptionException(argerr("ipv6 gateway[%s] should be ipv6 address", info.ipv6Gateway));
            }
            info.ipv6Gateway = IPv6NetworkUtils.getIpv6AddressCanonicalString(info.ipv6Gateway);
        }

        if (!StringUtils.isEmpty(info.ipv4Address)) {
            NormalIpRangeVO ipRangeVO = Q.New(NormalIpRangeVO.class)
                    .eq(NormalIpRangeVO_.l3NetworkUuid, l3Uuid)
                    .eq(NormalIpRangeVO_.ipVersion, IPv6Constants.IPv4)
                    .limit(1).find();
            if (ipRangeVO == null) {
                if (StringUtils.isEmpty(info.ipv4Netmask)) {
                    throw new ApiMessageInterceptionException(argerr("netmask must be set"));
                }
            } else {
                if (StringUtils.isEmpty(info.ipv4Netmask)) {
                    info.ipv4Netmask = ipRangeVO.getNetmask();
                    newSysTags.add(VmSystemTags.IPV4_NETMASK.instantiateTag(
                            map(e(VmSystemTags.IPV4_NETMASK_L3_UUID_TOKEN, l3Uuid),
                                    e(VmSystemTags.IPV4_NETMASK_TOKEN, ipRangeVO.getNetmask()))
                    ));
                } else if (!info.ipv4Netmask.equals(ipRangeVO.getNetmask())) {
                    throw new ApiMessageInterceptionException(argerr("netmask error, expect: %s, got: %s",
                            ipRangeVO.getNetmask(), info.ipv4Netmask));
                }

                if (StringUtils.isEmpty(info.ipv4Gateway)) {
                    info.ipv4Gateway = ipRangeVO.getGateway();
                    newSysTags.add(VmSystemTags.IPV4_GATEWAY.instantiateTag(
                            map(e(VmSystemTags.IPV4_GATEWAY_L3_UUID_TOKEN, l3Uuid),
                                    e(VmSystemTags.IPV4_GATEWAY_TOKEN, ipRangeVO.getGateway()))
                    ));
                } else if (!info.ipv4Gateway.equals(ipRangeVO.getGateway())) {
                    throw new ApiMessageInterceptionException(argerr("gateway error, expect: %s, got: %s",
                            ipRangeVO.getGateway(), info.ipv4Gateway));
                }
            }
        }

        if (!StringUtils.isEmpty(info.ipv6Address)) {
            NormalIpRangeVO ipRangeVO = Q.New(NormalIpRangeVO.class)
                    .eq(NormalIpRangeVO_.l3NetworkUuid, l3Uuid)
                    .eq(NormalIpRangeVO_.ipVersion, IPv6Constants.IPv6)
                    .limit(1).find();
            if (ipRangeVO == null) {
                if (info.ipv6Prefix == null) {
                    throw new ApiMessageInterceptionException(argerr("ipv6 prefix length must be set"));
                }
            } else {
                if (info.ipv6Prefix == null) {
                    info.ipv6Prefix = ipRangeVO.getPrefixLen();
                    newSysTags.add(VmSystemTags.IPV6_PREFIX.instantiateTag(
                            map(e(VmSystemTags.IPV6_PREFIX_L3_UUID_TOKEN, l3Uuid),
                                    e(VmSystemTags.IPV6_PREFIX_TOKEN, ipRangeVO.getPrefixLen()))
                    ));
                } else if (!info.ipv6Prefix.equals(ipRangeVO.getPrefixLen())) {
                    throw new ApiMessageInterceptionException(argerr("ipv6 prefix length error, expect: %s, got: %s",
                            ipRangeVO.getPrefixLen(), info.ipv6Prefix));
                }

                if (StringUtils.isEmpty(info.ipv6Gateway)) {
                    info.ipv6Gateway = IPv6NetworkUtils.getIpv6AddressCanonicalString(ipRangeVO.getGateway());
                    newSysTags.add(VmSystemTags.IPV6_GATEWAY.instantiateTag(
                            map(e(VmSystemTags.IPV6_GATEWAY_L3_UUID_TOKEN, l3Uuid),
                                    e(VmSystemTags.IPV6_GATEWAY_TOKEN,
                                            IPv6NetworkUtils.ipv6AddressToTagValue(ipRangeVO.getGateway())))
                    ));
                } else if (!info.ipv6Gateway.equals(IPv6NetworkUtils.getIpv6AddressCanonicalString(ipRangeVO.getGateway()))) {
                    throw new ApiMessageInterceptionException(argerr("gateway error, expect: %s, got: %s",
                            ipRangeVO.getGateway(), info.ipv6Gateway));
                }
            }
        }

    }
}
