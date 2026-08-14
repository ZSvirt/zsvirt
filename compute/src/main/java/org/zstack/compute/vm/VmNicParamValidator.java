package org.zstack.compute.vm;

import org.apache.commons.lang.StringUtils;
import org.zstack.core.db.Q;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.network.l3.L3NetworkVO_;
import org.zstack.header.network.l3.NormalIpRangeVO;
import org.zstack.header.network.l3.NormalIpRangeVO_;
import org.zstack.header.vm.VmInstanceType;
import org.zstack.header.vm.VmNicParam;
import org.zstack.header.vm.VmNicState;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.network.IPv6Constants;
import org.zstack.utils.network.IPv6NetworkUtils;
import org.zstack.utils.network.NetworkUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static org.zstack.core.Platform.argerr;

public class VmNicParamValidator {
    private List<VmNicParam> vmNicParams;
    private List<String> l3Uuids;
    private String defaultL3Uuid;
    private List<String> supportNicDriverTypes;
    private String vmType;
    private boolean isWindowsVm = false;

    public VmNicParamValidator withVmNicParams(List<VmNicParam> vmNicParams) {
        this.vmNicParams = vmNicParams;
        return this;
    }

    public VmNicParamValidator withVmNicParam(VmNicParam vmNicParam) {
        this.vmNicParams = Collections.singletonList(vmNicParam);
        return this;
    }

    public VmNicParamValidator withL3Uuids(List<String> l3Uuids) {
        this.l3Uuids = l3Uuids;
        return this;
    }

    public VmNicParamValidator withL3Uuid(String l3Uuid) {
        this.l3Uuids = Collections.singletonList(l3Uuid);
        return this;
    }

    public VmNicParamValidator withDefaultL3Uuid(String defaultL3Uuid) {
        this.defaultL3Uuid = defaultL3Uuid;
        return this;
    }

    public VmNicParamValidator withSupportNicDriverTypes(List<String> supportNicDriverTypes) {
        this.supportNicDriverTypes = supportNicDriverTypes;
        return this;
    }

    public VmNicParamValidator withVmType(String vmType) {
        this.vmType = vmType;
        return this;
    }

    public VmNicParamValidator isWindowsVm(boolean isWindowsVm) {
        this.isWindowsVm = isWindowsVm;
        return this;
    }

    public void validate() {
        if (CollectionUtils.isEmpty(vmNicParams)) {
            return;
        }

        List<String> l3UuidsInParams = vmNicParams.stream().map(VmNicParam::getL3NetworkUuid).distinct().collect(Collectors.toList());
        if (l3UuidsInParams.size() != vmNicParams.size()) {
            if (!VmGlobalConfig.MULTI_VNIC_SUPPORT.value(Boolean.class)) {
                throw new ApiMessageInterceptionException(argerr("duplicate nic params"));
            }

            List<VmNicParam> sriovEnabledNics = vmNicParams.stream().filter(VmNicParam::isSriovEnabled).collect(Collectors.toList());
            if (sriovEnabledNics.stream().map(VmNicParam::getL3NetworkUuid).distinct().count() != sriovEnabledNics.size()) {
                throw new ApiMessageInterceptionException(argerr("could not create multi SR-IOV enabled nics on the same l3 network"));
            }
        }

        for (VmNicParam nic : vmNicParams) {
            String l3Uuid = nic.getL3NetworkUuid();
            if (StringUtils.isEmpty(l3Uuid)) {
                throw new ApiMessageInterceptionException(argerr("l3NetworkUuid of vm nic can not be null"));
            }
            if (!CollectionUtils.isEmpty(l3Uuids) && !l3Uuids.contains(nic.getL3NetworkUuid())) {
                throw new ApiMessageInterceptionException(argerr("l3NetworkUuid of vm nic is not in l3[%s]", l3Uuids));
            }

            // only set when cloning vm
            if (nic.isDefaultNic()) {
                if (defaultL3Uuid != null && !l3Uuid.equals(defaultL3Uuid)) {
                    throw new ApiMessageInterceptionException(argerr("duplicate default nic is set"));
                }

                defaultL3Uuid = l3Uuid;
            }

            if (nic.getOutboundBandwidth() != null) {
                if (nic.getOutboundBandwidth() < 8192 || nic.getOutboundBandwidth() > 32212254720L) {
                    throw new ApiMessageInterceptionException(argerr("outbound bandwidth[%d] of vm nic is out of [8192, 32212254720]", nic.getOutboundBandwidth()));
                }
            }

            if (nic.getInboundBandwidth() != null) {
                if (nic.getInboundBandwidth() < 8192 || nic.getInboundBandwidth() > 32212254720L) {
                    throw new ApiMessageInterceptionException(argerr("inbound bandwidth[%d] of vm nic is out of [8192, 32212254720]", nic.getInboundBandwidth()));
                }
            }

            if (nic.getMultiQueueNum() != null ) {
                if (nic.getMultiQueueNum() < 1 || nic.getMultiQueueNum() > 256) {
                    throw new ApiMessageInterceptionException(argerr("multi queue num[%d] of vm nic is out of [1,256]", nic.getMultiQueueNum()));
                }
            }

            if (nic.getState() != null) {
                if (!asList(VmNicState.enable.toString(), VmNicState.disable.toString()).contains(nic.getState())) {
                    throw new ApiMessageInterceptionException(argerr("vm nic of l3[uuid:%s] state[%s] is not %s or %s ", nic.getL3NetworkUuid(), nic.getState(), VmNicState.enable.toString(), VmNicState.disable.toString()));
                }
            }

            String driverType = nic.getDriverType();
            if (!StringUtils.isEmpty(driverType) && !CollectionUtils.isEmpty(supportNicDriverTypes) && !supportNicDriverTypes.contains(driverType)){
                throw new ApiMessageInterceptionException(argerr("vm nic driver %s not support yet", driverType));
            }

            if (nic.isSriovEnabled() && vmType != null && !VmInstanceType.valueOf(vmType).isSriovSupported()) {
                throw new ApiMessageInterceptionException(argerr("vm type[%s] is not supported SR-IOV", vmType));
            }

            if (!nic.isSriovEnabled() && nic.getVfParentUuid() != null) {
                throw new ApiMessageInterceptionException(argerr("vm nic with vf parent uuid[%s] should be SR-IOV enabled", nic.getVfParentUuid()));
            }

            if (!StringUtils.isEmpty(nic.getMac())) {
                MacOperator macOperator = new MacOperator();
                macOperator.validateAvailableMac(nic.getMac());
            }

            if (!StringUtils.isEmpty(nic.getIp())) {
                if (!NetworkUtils.isIpv4Address(nic.getIp())) {
                    throw new ApiMessageInterceptionException(argerr("ipv4 address[%s] is not valid", nic.getIp()));
                }
                L3NetworkVO l3 = Q.New(L3NetworkVO.class).eq(L3NetworkVO_.uuid, l3Uuid).find();
                StaticIpOperator operator = new StaticIpOperator();
                operator.checkIpAvailability(l3, nic.getIp());
            }

            if (!StringUtils.isEmpty(nic.getIp6())) {
                String ip6 = IPv6NetworkUtils.ipv6TagValueToAddress(nic.getIp6());
                if (!IPv6NetworkUtils.isIpv6Address(ip6)) {
                    throw new ApiMessageInterceptionException(argerr("ipv6 address[%s] is not valid", ip6));
                }
                L3NetworkVO l3 = Q.New(L3NetworkVO.class).eq(L3NetworkVO_.uuid, l3Uuid).find();
                StaticIpOperator operator = new StaticIpOperator();
                operator.checkIpAvailability(l3, ip6);
            }

            if (!StringUtils.isEmpty(nic.getNetmask())) {
                if (!NetworkUtils.isNetmask(nic.getNetmask())) {
                    throw new ApiMessageInterceptionException(argerr("netmask[%s] is not valid", nic.getNetmask()));
                }
            }

            if (!StringUtils.isEmpty(nic.getGateway())) {
                if (!NetworkUtils.isIpv4Address(nic.getGateway())) {
                    throw new ApiMessageInterceptionException(argerr("gateway[%s] should be ipv4 address", nic.getGateway()));
                }
            }

            if (nic.getIpv6Prefix() != null) {
                int prefixLen = nic.getIpv6Prefix();
                if (prefixLen > IPv6Constants.IPV6_PREFIX_LEN_MAX || prefixLen < IPv6Constants.IPV6_PREFIX_LEN_MIN) {
                    throw new ApiMessageInterceptionException(argerr("ip range prefix length[%d] is out of range [%d - %d]",
                            prefixLen, IPv6Constants.IPV6_PREFIX_LEN_MIN, IPv6Constants.IPV6_PREFIX_LEN_MAX));
                }
            }

            if (!StringUtils.isEmpty(nic.getIpv6Gateway())) {
                if (!IPv6NetworkUtils.isIpv6Address(nic.getIpv6Gateway())) {
                    throw new ApiMessageInterceptionException(argerr("ipv6 gateway[%s] should be ipv6 address", nic.getIpv6Gateway()));
                }
            }

            if (!StringUtils.isEmpty(nic.getIp())) {
                NormalIpRangeVO ipRangeVO = Q.New(NormalIpRangeVO.class)
                        .eq(NormalIpRangeVO_.l3NetworkUuid, l3Uuid)
                        .eq(NormalIpRangeVO_.ipVersion, IPv6Constants.IPv4)
                        .limit(1).find();
                if (ipRangeVO == null) {
                    if (StringUtils.isEmpty(nic.getNetmask())) {
                        throw new ApiMessageInterceptionException(argerr("netmask must be set when ip is set"));
                    }
                } else {
                    if (!StringUtils.isEmpty(nic.getNetmask()) && !Objects.equals(nic.getNetmask(), ipRangeVO.getNetmask())) {
                        throw new ApiMessageInterceptionException(argerr("netmask error, expect: %s, got: %s",
                                ipRangeVO.getNetmask(), nic.getNetmask()));
                    }
                    if (!StringUtils.isEmpty(nic.getGateway()) && !Objects.equals(nic.getGateway(), ipRangeVO.getGateway())) {
                        throw new ApiMessageInterceptionException(argerr("gateway error, expect: %s, got: %s",
                                ipRangeVO.getGateway(), nic.getGateway()));
                    }
                }
            }

            if (!StringUtils.isEmpty(nic.getIp6())) {
                NormalIpRangeVO ipRangeVO = Q.New(NormalIpRangeVO.class)
                        .eq(NormalIpRangeVO_.l3NetworkUuid, l3Uuid)
                        .eq(NormalIpRangeVO_.ipVersion, IPv6Constants.IPv6)
                        .limit(1).find();
                if (ipRangeVO == null) {
                    if (nic.getIpv6Prefix() == null) {
                        throw new ApiMessageInterceptionException(argerr("ipv6 prefix length must be set when ip6 is set"));
                    }
                } else {
                    if (nic.getIpv6Prefix() != null && !Objects.equals(nic.getIpv6Prefix(), ipRangeVO.getPrefixLen())) {
                        throw new ApiMessageInterceptionException(argerr("ipv6 prefix length error, expect: %s, got: %s",
                                ipRangeVO.getPrefixLen(), nic.getIpv6Prefix()));
                    }
                    if (!StringUtils.isEmpty(nic.getIpv6Gateway()) && !Objects.equals(nic.getIpv6Gateway(), ipRangeVO.getGateway())) {
                        throw new ApiMessageInterceptionException(argerr("ipv6 gateway error, expect: %s, got: %s",
                                ipRangeVO.getGateway(), nic.getIpv6Gateway()));
                    }
                }
            }

            if (!CollectionUtils.isEmpty(nic.getDns6List())) {
                if (isWindowsVm) {
                    if (nic.getDns6List().size() > 2) {
                        throw new ApiMessageInterceptionException(argerr("size of dns6 list for Windows vm should not exceed 2"));
                    }

                    for (String dns: nic.getDns6List()) {
                        if (!IPv6NetworkUtils.isValidIpv6(dns)) {
                            throw new ApiMessageInterceptionException(argerr("dns6[%s] should be ipv6 address", dns));
                        }
                    }

                    if (nic.getDns6List().size() > nic.getDns6List().stream().distinct().count()) {
                        throw new ApiMessageInterceptionException(argerr("duplicate dns in dns6 list %s", nic.getDns6List()));
                    }
                } else {
                    if (nic.getDnsList() == null) {
                        nic.setDnsList(new ArrayList<>());
                    }
                    nic.getDnsList().addAll(nic.getDns6List());
                }
            }

            if (!CollectionUtils.isEmpty(nic.getDnsList())) {
                if (isWindowsVm) {
                    if (nic.getDnsList().size() > 2) {
                        throw new ApiMessageInterceptionException(argerr("size of dns list for Windows vm should not exceed 2"));
                    }

                    for (String dns: nic.getDnsList()) {
                        if (!NetworkUtils.isIpv4Address(dns)) {
                            throw new ApiMessageInterceptionException(argerr("dns[%s] should be ipv4 address", dns));
                        }
                    }
                } else {
                    if (!Objects.equals(l3Uuid, defaultL3Uuid)) {
                        throw new ApiMessageInterceptionException(argerr("dns should be set to the default l3 network"));
                    }

                    if (nic.getDnsList().size() > 3) {
                        throw new ApiMessageInterceptionException(argerr("total size of dns list should not exceed 3"));
                    }

                    for (String dns: nic.getDnsList()) {
                        if (!NetworkUtils.isValidIPAddress(dns)) {
                            throw new ApiMessageInterceptionException(argerr("dns[%s] is not a IP address", dns));
                        }
                    }
                }

                if (nic.getDnsList().size() > nic.getDnsList().stream().distinct().count()) {
                    throw new ApiMessageInterceptionException(argerr("duplicate dns in dns list %s", nic.getDnsList()));
                }
            }
        }

        if (defaultL3Uuid == null) {
            if (vmNicParams.size() > 1) {
                throw new ApiMessageInterceptionException(argerr("default nic is not set, but multiple nics are specified"));
            }
        }
    }
}
