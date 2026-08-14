package org.zstack.network.service;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.Q;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.header.Component;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.network.l3.*;
import org.zstack.header.network.service.DhcpStruct;
import org.zstack.header.network.service.NetworkServiceDhcpBackend;
import org.zstack.header.network.service.NetworkServiceProviderType;
import org.zstack.header.network.service.NetworkServiceType;
import org.zstack.header.vm.*;
import org.zstack.header.vm.VmInstanceSpec.HostName;
import org.zstack.network.l3.IpRangeHelper;
import org.zstack.network.l3.L3NetworkGlobalConfig;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.IPv6Constants;
import org.zstack.utils.network.NetworkUtils;

import java.util.*;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.operr;
import static org.zstack.utils.CollectionUtils.findOneOrNull;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 7:53 PM
 * To change this template use File | Settings | File Templates.
 */
public class DhcpExtension extends AbstractNetworkServiceExtension implements Component, VmDefaultL3NetworkChangedExtensionPoint {
    private static final CLogger logger = Utils.getLogger(DhcpExtension.class);

    @Autowired
    private PluginRegistry pluginRgty;

    private final Map<NetworkServiceProviderType, NetworkServiceDhcpBackend> dhcpBackends = new HashMap<NetworkServiceProviderType, NetworkServiceDhcpBackend>();

    private final String RESULT = String.format("result.%s", DhcpExtension.class.getName());

    public NetworkServiceType getNetworkServiceType() {
        return NetworkServiceType.DHCP;
    }

    private void doDhcp(final Iterator<Map.Entry<NetworkServiceDhcpBackend, List<DhcpStruct>>> it, final VmInstanceSpec spec, final Completion complete) {
        if (!it.hasNext()) {
            complete.success();
            return;
        }

        Map.Entry<NetworkServiceDhcpBackend, List<DhcpStruct>> e = it.next();
        NetworkServiceDhcpBackend bkd = e.getKey();
        List<DhcpStruct> structs = e.getValue();
        logger.debug(String.format("%s is applying DHCP service", bkd.getClass().getName()));
        bkd.applyDhcpService(structs, spec, new Completion(complete) {
            @Override
            public void success() {
                doDhcp(it, spec, complete);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                complete.fail(errorCode);
            }
        });
    }

    private void releaseDhcp(final Iterator<Map.Entry<NetworkServiceDhcpBackend, List<DhcpStruct>>> it, final VmInstanceSpec spec, final NoErrorCompletion completion) {
        if (!it.hasNext()) {
            completion.done();
            return;
        }
        if (!Optional.ofNullable(spec.getDestHost()).isPresent()){
            completion.done();
            return;
        }
        Map.Entry<NetworkServiceDhcpBackend, List<DhcpStruct>> e = it.next();
        NetworkServiceDhcpBackend bkd = e.getKey();
        List<DhcpStruct> structs = e.getValue();
        logger.debug(String.format("%s is releasing DHCP service", bkd.getClass().getName()));
        bkd.releaseDhcpService(structs, spec, new NoErrorCompletion(completion) {
            @Override
            public void done() {
                releaseDhcp(it, spec, completion);
            }
        });
    }

    @Override
    public void applyNetworkService(VmInstanceSpec spec, Map<String, Object> data, Completion complete) {
        Map<NetworkServiceDhcpBackend, List<DhcpStruct>> entries = workoutDhcp(spec);
        data.put(RESULT, entries);
        doDhcp(entries.entrySet().iterator(), spec, complete);
    }

    @Override
    public void releaseNetworkService(VmInstanceSpec spec, Map<String, Object> data, NoErrorCompletion completion) {
        Map<NetworkServiceDhcpBackend, List<DhcpStruct>> entries = (Map<NetworkServiceDhcpBackend, List<DhcpStruct>>) data.get(RESULT);
        if (entries == null) {
            entries = workoutDhcp(spec);
        }
        releaseDhcp(entries.entrySet().iterator(), spec, completion);
    }

    private void populateExtensions() {
        for (NetworkServiceDhcpBackend bkd : pluginRgty.getExtensionList(NetworkServiceDhcpBackend.class)) {
            NetworkServiceDhcpBackend old = dhcpBackends.get(bkd.getProviderType());
            if (old != null) {
                throw new CloudRuntimeException(String.format("duplicate NetworkServiceDhcpBackend[%s, %s] for type[%s]",
                        bkd.getClass().getName(), old.getClass().getName(), bkd.getProviderType()));
            }
            dhcpBackends.put(bkd.getProviderType(), bkd);
        }
    }

    public boolean isDualStackNicInSingleL3Network(VmNicInventory nic) {
        if (nic.getUsedIps().size() < 2) {
            return false;
        }

        return nic.getUsedIps().stream().map(UsedIpInventory::getL3NetworkUuid).distinct().count() == 1;
    }

    private DhcpStruct getDhcpStruct(VmInstanceInventory vm, List<VmInstanceSpec.HostName> hostNames, VmNicVO nic, UsedIpVO ip, boolean isDefaultNic) {
        String l3Uuid = nic.getL3NetworkUuid();
        if (ip != null) {
            l3Uuid = ip.getL3NetworkUuid();
        }

        L3NetworkInventory l3 = L3NetworkInventory.valueOf(dbf.findByUuid(l3Uuid, L3NetworkVO.class));
        DhcpStruct struct = new DhcpStruct();
        struct.setVmUuid(nic.getVmInstanceUuid());
        VmInstanceSpec.HostName selected = findOneOrNull(hostNames, it -> it.getL3NetworkUuid().equals(l3.getUuid()));
        String hostname = selected == null ? null : selected.getHostname();
        if (hostname != null && l3.getDnsDomain() != null) {
            hostname = String.format("%s.%s", hostname, l3.getDnsDomain());
        }
        struct.setHostname(hostname);
        struct.setDnsDomain(l3.getDnsDomain());
        struct.setL3Network(l3);
        struct.setDefaultL3Network(isDefaultNic);
        struct.setMac(nic.getMac());
        struct.setMtu(new MtuGetter().getMtu(l3.getUuid()));
        struct.setNicType(nic.getType());

        return struct;
    }

    private boolean isEnableRa(String l3Uuid) {
        String l3Type = Q.New(L3NetworkVO.class)
                .select(L3NetworkVO_.type)
                .eq(L3NetworkVO_.uuid, l3Uuid)
                .findValue();
        // vpc network does not need to enable ra
        if (!L3NetworkType.valueOf(l3Type).enableRa()) {
            return false;
        }

        return L3NetworkGlobalConfig.BASIC_NETWORK_ENABLE_RA.value(Boolean.class);
    }

    private void setDualStackNicOfSingleL3Network(DhcpStruct struct, VmNicVO nic) {
        struct.setIpVersion(IPv6Constants.DUAL_STACK);
        List<UsedIpVO> sortedIps = nic.getUsedIps().stream().sorted(Comparator.comparingLong(UsedIpVO::getIpVersionl)).collect(Collectors.toList());
        for (UsedIpVO ip : sortedIps) {
            if (ip.getIpVersion() == IPv6Constants.IPv4) {
                struct.setGateway(ip.getGateway());
                struct.setIp(ip.getIp());
                struct.setNetmask(ip.getNetmask());
                if (struct.getHostname() == null) {
                    struct.setHostname(ip.getIp().replaceAll("\\.", "-"));
                }
            } else {
                List<NormalIpRangeVO> iprs = Q.New(NormalIpRangeVO.class).eq(NormalIpRangeVO_.l3NetworkUuid, ip.getL3NetworkUuid())
                        .eq(NormalIpRangeVO_.ipVersion, ip.getIpVersion()).list();

                struct.setGateway6(ip.getGateway());
                struct.setIp6(ip.getIp());
                struct.setEnableRa(isEnableRa(ip.getL3NetworkUuid()));
                if (iprs.isEmpty() || iprs.get(0).getAddressMode().equals(IPv6Constants.SLAAC)) {
                    continue;
                }
                struct.setRaMode(iprs.get(0).getAddressMode());
                struct.setPrefixLength(iprs.get(0).getPrefixLen());
                struct.setFirstIp(NetworkUtils.getSmallestIp(iprs.stream().map(IpRangeVO::getStartIp).collect(Collectors.toList())));
                struct.setEndIP(NetworkUtils.getBiggesttIp(iprs.stream().map(IpRangeVO::getEndIp).collect(Collectors.toList())));
            }
        }
    }

    private void setNicDhcp(DhcpStruct struct, UsedIpVO ip) {
        if (ip.getIpVersion() == IPv6Constants.IPv4) {
            struct.setGateway(ip.getGateway());
            struct.setIp(ip.getIp());
            struct.setNetmask(ip.getNetmask());
            if (struct.getHostname() == null) {
                struct.setHostname(ip.getIp().replaceAll("\\.", "-"));
            }
        } else {
            List<NormalIpRangeVO> iprs = Q.New(NormalIpRangeVO.class).eq(NormalIpRangeVO_.l3NetworkUuid, ip.getL3NetworkUuid())
                    .eq(NormalIpRangeVO_.ipVersion, IPv6Constants.IPv6).list();
            struct.setGateway6(ip.getGateway());
            struct.setIp6(ip.getIp());
            struct.setEnableRa(isEnableRa(ip.getL3NetworkUuid()));
            if (iprs.isEmpty()) {
                return;
            }
            struct.setRaMode(iprs.get(0).getAddressMode());
            struct.setPrefixLength(iprs.get(0).getPrefixLen());
            struct.setFirstIp(NetworkUtils.getSmallestIp(iprs.stream().map(IpRangeVO::getStartIp).collect(Collectors.toList())));
            struct.setEndIP(NetworkUtils.getBiggesttIp(iprs.stream().map(IpRangeVO::getEndIp).collect(Collectors.toList())));
        }
    }

    public List<DhcpStruct> makeDhcpStruct(VmInstanceInventory vm, List<VmInstanceSpec.HostName> hostNames, List<VmNicVO> nics) {
        List<DhcpStruct> res = new ArrayList<>();

        List<VmNicVO> defaultNics = nics.stream().filter(nic -> nic.getL3NetworkUuid().equals(vm.getDefaultL3NetworkUuid())).collect(Collectors.toList());
        for (VmNicVO nic : nics) {
            boolean isDefaultNic = nic.equals(VmNicVO.findTheEarliestOne(defaultNics));
            if (isDualStackNicInSingleL3Network(VmNicInventory.valueOf(nic))) {
                DhcpStruct struct = getDhcpStruct(vm, hostNames, nic, null, isDefaultNic);
                setDualStackNicOfSingleL3Network(struct, nic);

                if (struct.getIp() != null && struct.getGateway() == null) {
                    /* dnsmasq need gateway parameter when ipv4 */
                    logger.info(String.format("can not get gateway address for vmnic[ip:%s] for vm[name:%s, uuid:%s]",
                            struct.getIp(), vm.getName(), vm.getUuid()));
                    continue;
                }

                if (struct.getIp6() != null && (struct.getFirstIp() == null
                        || struct.getEndIP() == null || struct.getPrefixLength() == null)) {
                    logger.info(String.format("can not get ipv6 range info for vmnic[ip:%s] for vm[name:%s, uuid:%s]",
                            struct.getIp6(), vm.getName(), vm.getUuid()));
                    /* dnsmasq need start ip and end ip when ipv6 */
                    continue;
                }

                res.add(struct);
                continue;
            }

            for (UsedIpVO ip : nic.getUsedIps()) {
                if (ip.getIpVersion() == IPv6Constants.IPv6) {
                    NormalIpRangeVO ipr = Q.New(NormalIpRangeVO.class)
                            .eq(NormalIpRangeVO_.l3NetworkUuid, ip.getL3NetworkUuid())
                            .eq(NormalIpRangeVO_.ipVersion, IPv6Constants.IPv6).limit(1).find();
                    if (ipr == null) {
                        /* dhcp v6 need ra mode and ip range start/end ip */
                        logger.info(String.format("can not get ipv6 range info for vmnic[ip:%s] for vm[name:%s, uuid:%s]",
                                ip.getIp(), vm.getName(), vm.getUuid()));
                        continue;
                    }

                    if (ipr.getAddressMode().equals(IPv6Constants.SLAAC)) {
                        continue;
                    }
                } else {
                    if (StringUtils.isEmpty(ip.getGateway())) {
                        /* dnsmasq need gateway parameter when ipv4 */
                        logger.info(String.format("can not get gateway address for vmnic[ip:%s] for vm[name:%s, uuid:%s]",
                                ip.getIp(), vm.getName(), vm.getUuid()));
                        continue;
                    }
                }

                DhcpStruct struct = getDhcpStruct(vm, hostNames, nic, ip, isDefaultNic);
                struct.setIpVersion(ip.getIpVersion());
                setNicDhcp(struct, ip);
                res.add(struct);
            }
        }

        return res;
    }

    private Map<NetworkServiceDhcpBackend, List<DhcpStruct>> workoutDhcp(VmInstanceSpec spec) {
        Map<NetworkServiceDhcpBackend, List<DhcpStruct>> map = new HashMap<NetworkServiceDhcpBackend, List<DhcpStruct>>();
        Map<NetworkServiceProviderType, List<L3NetworkInventory>> providerMap = getNetworkServiceProviderMap(NetworkServiceType.DHCP,
                VmNicSpec.getL3NetworkInventoryOfSpec(spec.getL3Networks()));

        for (Map.Entry<NetworkServiceProviderType, List<L3NetworkInventory>> e : providerMap.entrySet()) {
            NetworkServiceProviderType ptype = e.getKey();
            List<DhcpStruct> lst = new ArrayList<DhcpStruct>();

            List<VmNicVO> nics = new ArrayList<>();
            Map<String, L3NetworkInventory> l3Map = new HashMap<>();
            for (L3NetworkInventory l3 : e.getValue()) {
                l3Map.put(l3.getUuid(), l3);
            }

            for (VmNicInventory inv : spec.getDestNics()) {
                VmNicVO vmNicVO = dbf.findByUuid(inv.getUuid(), VmNicVO.class);
                for (UsedIpVO ip : vmNicVO.getUsedIps()) {
                    L3NetworkInventory l3 = l3Map.get(ip.getL3NetworkUuid());
                    if (l3 == null) {
                        continue;
                    }

                    List<IpRangeInventory> iprs = IpRangeHelper.getNormalIpRanges(l3);
                    if (iprs.isEmpty()) {
                        continue;
                    }

                    if (!nics.contains(vmNicVO)) {
                        nics.add(vmNicVO);
                    }
                }
            }

            lst.addAll(makeDhcpStruct(spec.getVmInventory(), spec.getHostnames(), nics));

            NetworkServiceDhcpBackend bkd = dhcpBackends.get(ptype);
            if (bkd == null) {
                throw new CloudRuntimeException(String.format("unable to find NetworkServiceDhcpBackend[provider type: %s]", ptype));
            }
            map.put(bkd, lst);

            if (logger.isTraceEnabled()) {
                logger.trace(String.format("DHCP Backend[%s] is about to apply entries: \n%s", bkd.getClass().getName(), lst));
            }
        }

        return map;
    }

    @Override
    public boolean start() {
        populateExtensions();
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public void vmDefaultL3NetworkChanged(VmInstanceInventory vm, String previousL3, String nowL3) {
        List<String> l3Uuids = new ArrayList<String>();
        if (previousL3 != null) {
            l3Uuids.add(previousL3);
        }
        if (nowL3 != null) {
            l3Uuids.add(nowL3);
        }

        SimpleQuery<L3NetworkVO> q = dbf.createQuery(L3NetworkVO.class);
        q.add(L3NetworkVO_.uuid, Op.IN, l3Uuids);
        List<L3NetworkVO> vos = q.list();
        List<L3NetworkInventory> invs = L3NetworkInventory.valueOf(vos);
        Map<NetworkServiceProviderType, List<L3NetworkInventory>> providerMap = getNetworkServiceProviderMap(NetworkServiceType.DHCP, invs);
        for (Map.Entry<NetworkServiceProviderType, List<L3NetworkInventory>> e : providerMap.entrySet()) {
            NetworkServiceProviderType ptype = e.getKey();

            NetworkServiceDhcpBackend bkd = dhcpBackends.get(ptype);
            if (bkd == null) {
                throw new CloudRuntimeException(String.format("unable to find NetworkServiceDhcpBackend[provider type: %s]", ptype));
            }

            bkd.vmDefaultL3NetworkChanged(vm, previousL3, nowL3, new Completion(null) {
                @Override
                public void success() {
                    // pass
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    logger.warn(String.format("unable to change the VM[uuid:%s]'s default L3 network in the DHCP backend, %s. You may need to reboot" +
                            " the VM to use the new default L3 network setting", vm.getUuid(), errorCode));
                }
            });
        }
    }

    @Override
    public void enableNetworkService(L3NetworkVO l3VO, NetworkServiceProviderType providerType, List<String> systemTags, Completion completion) {
        NetworkServiceDhcpBackend bkd = dhcpBackends.get(providerType);
        if (bkd == null) {
            completion.fail(operr("unable to find NetworkServiceDhcpBackend[provider type: %s]", providerType));
            return;
        }

        bkd.enableNetworkService(l3VO, systemTags, completion);
    }

    @Override
    public void disableNetworkService(L3NetworkVO l3VO, NetworkServiceProviderType providerType, Completion completion) {
        NetworkServiceDhcpBackend bkd = dhcpBackends.get(providerType);
        if (bkd == null) {
            completion.fail(operr("unable to find NetworkServiceDhcpBackend[provider type: %s]", providerType));
            return;
        }

        logger.debug(String.format("[%s] disable dhcp service for l3 network[uuid:%s]",
                bkd.getClass().getSimpleName(), l3VO.getUuid()));
        bkd.disableNetworkService(l3VO, completion);
    }
}
