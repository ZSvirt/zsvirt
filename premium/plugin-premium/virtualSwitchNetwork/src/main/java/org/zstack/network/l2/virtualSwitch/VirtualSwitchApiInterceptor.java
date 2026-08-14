package org.zstack.network.l2.virtualSwitch;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.compute.bonding.HostNetworkBondingConstant;
import org.zstack.compute.bonding.HostNetworkBondingUtils;
import org.zstack.compute.vm.StaticIpOperator;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.GlobalApiMessageInterceptor;
import org.zstack.header.bonding.APIDeleteBondingMsg;
import org.zstack.header.bonding.APIUpdateBondingMsg;
import org.zstack.header.cluster.ClusterConstant;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.cluster.ClusterVO_;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.host.HostParam;
import org.zstack.header.host.HostStatus;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.HostVO_;
import org.zstack.header.message.APIMessage;
import org.zstack.header.network.l2.*;
import org.zstack.header.network.l3.*;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.kvm.KVMConstant;
import org.zstack.network.hostNetworkInterface.HostNetworkBondingVO;
import org.zstack.network.hostNetworkInterface.HostNetworkBondingVO_;
import org.zstack.network.hostNetworkInterface.HostNetworkInterfaceVO;
import org.zstack.network.hostNetworkInterface.HostNetworkInterfaceVO_;
import org.zstack.network.l2.virtualSwitch.header.*;
import org.zstack.pciDevice.PciDeviceUtils;
import org.zstack.tag.PatternedSystemTag;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.IPv6Constants;
import org.zstack.utils.network.NetworkUtils;
import org.zstack.utils.network.NicIpAddressInfo;

import java.util.*;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.argerr;
import static org.zstack.core.Platform.operr;

/**
 * Created by shixin.ruan 2023.09.06
 */
public class VirtualSwitchApiInterceptor implements GlobalApiMessageInterceptor {
    private static final CLogger logger = Utils.getLogger(VirtualSwitchApiInterceptor.class);
    private static final StaticIpOperator ipOperator = new StaticIpOperator();

    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    CloudBus bus;

    private void setServiceId(APIMessage msg) {
        if (msg instanceof L2NetworkMessage) {
            L2NetworkMessage l2msg = (L2NetworkMessage) msg;
            bus.makeTargetServiceIdByResourceUuid(msg, L2NetworkConstant.SERVICE_ID, l2msg.getL2NetworkUuid());
        } else if (msg instanceof L3NetworkMessage) {
            L3NetworkMessage l3msg = (L3NetworkMessage) msg;
            bus.makeTargetServiceIdByResourceUuid(msg, L3NetworkConstant.SERVICE_ID, l3msg.getL3NetworkUuid());
        } else if (msg instanceof APICreatePortGroupMsg) {
            APICreatePortGroupMsg cmsg = (APICreatePortGroupMsg) msg;
            bus.makeTargetServiceIdByResourceUuid(msg, L3NetworkConstant.SERVICE_ID, cmsg.getvSwitchUuid());
        } else if (msg instanceof HostKernelInterfaceMessage) {
            HostKernelInterfaceMessage hmsg = (HostKernelInterfaceMessage) msg;
            bus.makeTargetServiceIdByResourceUuid(msg, VirtualSwitchConstant.VIRTUAL_SWITCH_SERVICE_ID, hmsg.getHostKernelInterfaceUuid());
        } else if (msg instanceof APICreateHostKernelInterfaceMsg) {
            APICreateHostKernelInterfaceMsg cmsg = (APICreateHostKernelInterfaceMsg) msg;
            bus.makeTargetServiceIdByResourceUuid(msg, VirtualSwitchConstant.VIRTUAL_SWITCH_SERVICE_ID, cmsg.getL2NetworkUuid());
        } else if (msg instanceof APIBatchCreateHostKernelInterfaceMsg) {
            APIBatchCreateHostKernelInterfaceMsg cmsg = (APIBatchCreateHostKernelInterfaceMsg) msg;
            bus.makeTargetServiceIdByResourceUuid(msg, VirtualSwitchConstant.VIRTUAL_SWITCH_SERVICE_ID, cmsg.getL2NetworkUuid());
        } else if (msg instanceof APIGetCandidateHostKernelInterfacesMsg) {
            bus.makeLocalServiceId(msg, VirtualSwitchConstant.VIRTUAL_SWITCH_SERVICE_ID);
        }
    }

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APICreateL2VirtualSwitchMsg) {
            validate((APICreateL2VirtualSwitchMsg) msg);
        } else if (msg instanceof APICreateL2PortGroupMsg) {
            validate((APICreateL2PortGroupMsg) msg);
        } else if (msg instanceof APIAttachL2NetworkToClusterMsg) {
            validate((APIAttachL2NetworkToClusterMsg) msg);
        } else if (msg instanceof APIAttachL2NetworkToHostMsg) {
            validate((APIAttachL2NetworkToHostMsg) msg);
        } else if (msg instanceof APIDetachL2NetworkFromClusterMsg) {
            validate((APIDetachL2NetworkFromClusterMsg) msg);
        } else if (msg instanceof APIDetachL2NetworkFromHostMsg) {
            validate((APIDetachL2NetworkFromHostMsg) msg);
        } else if (msg instanceof APICreatePortGroupMsg) {
            validate((APICreatePortGroupMsg) msg);
        } else if (msg instanceof APICreateL3NetworkMsg) {
            validate((APICreateL3NetworkMsg) msg);
        } else if (msg instanceof APIUpdateVirtualSwitchUplinkBondingsMsg) {
            validate((APIUpdateVirtualSwitchUplinkBondingsMsg) msg);
        } else if (msg instanceof APIUpdateVirtualSwitchUplinkGroupMsg) {
            validate((APIUpdateVirtualSwitchUplinkGroupMsg) msg);
        } else if (msg instanceof APIDeleteL2NetworkMsg) {
            validate((APIDeleteL2NetworkMsg) msg);
        } else if (msg instanceof APIDeletePortGroupMsg) {
            validate((APIDeletePortGroupMsg) msg);
        } else if (msg instanceof APIUpdateL2NetworkVirtualNetworkIdMsg) {
            validate((APIUpdateL2NetworkVirtualNetworkIdMsg) msg);
        } else if (msg instanceof APICreateHostKernelInterfaceMsg) {
            validate((APICreateHostKernelInterfaceMsg) msg);
        } else if (msg instanceof APIBatchCreateHostKernelInterfaceMsg) {
            validate((APIBatchCreateHostKernelInterfaceMsg) msg);
        } else if (msg instanceof APIUpdateHostKernelInterfaceMsg) {
            validate((APIUpdateHostKernelInterfaceMsg) msg);
        } else if (msg instanceof APIDeleteHostKernelInterfaceMsg) {
            validate((APIDeleteHostKernelInterfaceMsg) msg);
        } else if (msg instanceof APIGetCandidateHostKernelInterfacesMsg) {
            validate((APIGetCandidateHostKernelInterfacesMsg) msg);
        } else if (msg instanceof APIUpdateBondingMsg) {
            validate((APIUpdateBondingMsg) msg);
        } else if (msg instanceof APIDeleteBondingMsg) {
            validate((APIDeleteBondingMsg) msg);
        }

        setServiceId(msg);

        return msg;
    }

    @Override
    public List<Class> getMessageClassToIntercept() {
        List<Class> ret = new ArrayList<>();

        // former serviceConfig(network.l2) bindings
        ret.add(APICreateL2VirtualSwitchMsg.class);
        ret.add(APICreateL2PortGroupMsg.class);
        ret.add(APIUpdateVirtualSwitchUplinkBondingsMsg.class);
        ret.add(APIUpdateVirtualSwitchUplinkGroupMsg.class);
        ret.add(APIQueryUplinkGroupMsg.class);
        ret.add(APIQueryL2VirtualSwitchNetworkMsg.class);
        ret.add(APIQueryL2PortGroupNetworkMsg.class);
        ret.add(APICreateHostKernelInterfaceMsg.class);
        ret.add(APIBatchCreateHostKernelInterfaceMsg.class);
        ret.add(APIUpdateHostKernelInterfaceMsg.class);
        ret.add(APIDeleteHostKernelInterfaceMsg.class);
        ret.add(APIQueryHostKernelInterfaceMsg.class);
        ret.add(APIGetCandidateHostKernelInterfacesMsg.class);
        ret.add(APICreatePortGroupMsg.class);
        ret.add(APIDeletePortGroupMsg.class);
        ret.add(APIUpdatePortGroupMsg.class);
        ret.add(APIQueryPortGroupMsg.class);

        // cross-service globals (previous Global list)
        ret.add(APIAttachL2NetworkToClusterMsg.class);
        ret.add(APIAttachL2NetworkToHostMsg.class);
        ret.add(APIDetachL2NetworkFromClusterMsg.class);
        ret.add(APIDetachL2NetworkFromHostMsg.class);
        ret.add(APICreateL3NetworkMsg.class);
        ret.add(APIDeleteL2NetworkMsg.class);
        ret.add(APIDeleteL3NetworkMsg.class);
        ret.add(APIUpdateL2NetworkVirtualNetworkIdMsg.class);
        ret.add(APIUpdateBondingMsg.class);
        ret.add(APIDeleteBondingMsg.class);

        return ret;
    }

    @Override
    public InterceptorPosition getPosition() {
        return InterceptorPosition.END;
    }

    private void validate(APIDeleteL2NetworkMsg msg) {
        L2NetworkVO l2vo = Q.New(L2NetworkVO.class).eq(L2NetworkVO_.uuid, msg.getUuid()).find();
        if (l2vo == null) {
            return;
        }

        if (l2vo.getType().equals(VirtualSwitchConstant.VIRTUAL_SWITCH_NETWORK_TYPE)) {
            if (VirtualSwitchSystemTags.VIRTUAL_SWITCH_DEFAULT.hasTag(l2vo.getUuid()) && !l2vo.getAttachedHostRefs().isEmpty()) {
                throw new ApiMessageInterceptionException(argerr("the default virtual switch network[uuid:%s] cannot be deleted when" +
                        " it is still attached to hosts", msg.getUuid()));
            }

            List<String> l2Uuids = Q.New(L2PortGroupNetworkVO.class).select(L2PortGroupNetworkVO_.uuid).eq(L2PortGroupNetworkVO_.vSwitchUuid, l2vo.getUuid()).listValues();
            if (!l2Uuids.isEmpty()) {
                List<String> interfaceUuids = SQL.New("select iface.uuid from HostKernelInterfaceVO iface, HostVO host" +
                                " where iface.hostUuid = host.uuid" +
                                " and host.status != :status" +
                                " and iface.l2NetworkUuid in (:l2Uuids)", String.class)
                        .param("status", HostStatus.Connected)
                        .param("l2Uuids", l2Uuids)
                        .list();
                if (!interfaceUuids.isEmpty()) {
                    throw new ApiMessageInterceptionException(argerr("could not delete virtual switch network[uuid:%s]," +
                            "because host kernel interface[uuid:%s] still exists on the virtual switch and its host status is not connected",
                            msg.getUuid(), interfaceUuids.get(0)));
                }
            }
        }

        if (l2vo.getType().equals(VirtualSwitchConstant.PORT_GROUP_NETWORK_TYPE)) {
            List<String> l3Uuids = Q.New(PortGroupVO.class).select(PortGroupVO_.uuid).eq(PortGroupVO_.l2NetworkUuid, l2vo.getUuid()).listValues();
            for (String l3Uuid : l3Uuids) {
                if (VirtualSwitchSystemTags.PORT_GROUP_DEFAULT.hasTag(l3Uuid)) {
                    throw new ApiMessageInterceptionException(argerr("could not delete l2 network[uuid:%s] with default port group", msg.getUuid()));
                }
            }

            List<String> interfaceUuids = SQL.New("select iface.uuid from HostKernelInterfaceVO iface, HostVO host" +
                            " where iface.hostUuid = host.uuid" +
                            " and host.status != :status" +
                            " and iface.l2NetworkUuid = :l2Uuid", String.class)
                    .param("status", HostStatus.Connected)
                    .param("l2Uuid", l2vo.getUuid())
                    .list();
            if (!interfaceUuids.isEmpty()) {
                throw new ApiMessageInterceptionException(argerr("could not delete l2 port group network[uuid:%s]," +
                        "because host kernel interface[%s] still exists on the port group and its host status is not connected",
                        msg.getUuid(), interfaceUuids.get(0)));
            }
        }
    }

    private void validate(APIDeletePortGroupMsg msg) {
        PortGroupVO pvo = Q.New(PortGroupVO.class).eq(PortGroupVO_.uuid, msg.getUuid()).find();
        if (pvo == null) {
            return;
        }

        if (VirtualSwitchSystemTags.PORT_GROUP_DEFAULT.hasTag(pvo.getUuid())) {
            List<String> hostUuidsWithDefaultKernel = SQL.New("select i.hostUuid from HostKernelInterfaceVO i, SystemTagVO sysTag" +
                            " where i.l3NetworkUuid = :l3Uuid" +
                            " and sysTag.resourceUuid = i.uuid" +
                            " and sysTag.tag = :tag", String.class)
                    .param("l3Uuid", pvo.getUuid())
                    .param("tag", VirtualSwitchSystemTags.HOST_KERNEL_DEFAULT_INTERFACE.getTagFormat())
                    .list();
            if (!hostUuidsWithDefaultKernel.isEmpty()) {
                throw new ApiMessageInterceptionException(argerr("cannot delete default port group[uuid:%s]," +
                        " because there are host kernel interfaces still exist on hosts[uuid:%s]",
                        msg.getUuid(), hostUuidsWithDefaultKernel));
            }
        }

        List<String> interfaceUuids = SQL.New("select iface.uuid from HostKernelInterfaceVO iface, HostVO host" +
                        " where iface.hostUuid = host.uuid" +
                        " and host.status != :status" +
                        " and iface.l3NetworkUuid = :l3NetworkUuid", String.class)
                .param("status", HostStatus.Connected)
                .param("l3NetworkUuid", pvo.getUuid())
                .list();
        if (!interfaceUuids.isEmpty()) {
            throw new ApiMessageInterceptionException(argerr("could not delete port group[uuid:%s]," +
                    " because host kernel interface[uuid:%s] still exists on the port group and its host status is not connected",
                    msg.getUuid(), interfaceUuids.get(0)));
        }

    }

    private void validate(APICreateHostKernelInterfaceMsg msg) {
        PortGroupVO pg = dbf.findByUuid(msg.getL3NetworkUuid(), PortGroupVO.class);
        msg.setL2NetworkUuid(pg.getL2NetworkUuid());
        if (!pg.getEnableIPAM() && StringUtils.isEmpty(msg.getRequiredIp())) {
            throw new ApiMessageInterceptionException(argerr("could not create host kernel interface, " +
                    "because requiredIp cannot be null with l3Network[uuid:%s] disable IPAM", msg.getL3NetworkUuid()));
        }

        NicIpAddressInfo info = new NicIpAddressInfo();
        info.ipv4Address = msg.getRequiredIp();
        info.ipv4Netmask = msg.getNetmask();
        ipOperator.validateStaticIp(info, pg, new ArrayList<>());
        msg.setNetmask(info.ipv4Netmask);
    }

    private void validate(APIBatchCreateHostKernelInterfaceMsg msg) {
        PortGroupVO pg = dbf.findByUuid(msg.getL3NetworkUuid(), PortGroupVO.class);
        msg.setL2NetworkUuid(pg.getL2NetworkUuid());

        Set<String> excludeIps = new HashSet<>();
        Set<String> excludeIpv6s = new HashSet<>();
        for (HostKernelInterfaceStruct struct : msg.getStructs()) {
            if (StringUtils.isEmpty(struct.getHostUuid())) {
                throw new ApiMessageInterceptionException(argerr("could not batch create host kernel interface, " +
                        "because hostUuid in struct should be set"));
            }
            if (!dbf.isExist(struct.getHostUuid(), HostVO.class)) {
                throw new ApiMessageInterceptionException(argerr("could not create host kernel interface, " +
                        "because host[uuid:%s] not found", struct.getHostUuid()));
            }

            if (StringUtils.isEmpty(struct.getName())) {
                throw new ApiMessageInterceptionException(argerr("could not create host kernel interface for host[uuid:%s], " +
                        "because name should be set", struct.getHostUuid()));
            }

            if (!pg.getEnableIPAM() && StringUtils.isEmpty(struct.getIp())) {
                throw new ApiMessageInterceptionException(argerr("could not batch create host kernel interface, " +
                        "because ip cannot be null with l3Network[uuid:%s] disable IPAM", msg.getL3NetworkUuid()));
            }

            NicIpAddressInfo info = new NicIpAddressInfo();
            info.ipv4Address = struct.getIp();
            info.ipv4Netmask = struct.getNetmask();
            info.ipv6Address = struct.getIp6();
            info.ipv6Prefix = struct.getIpv6Prefix();
            ipOperator.validateStaticIp(info, pg, new ArrayList<>());
            struct.setNetmask(info.ipv4Netmask);
            struct.setIp6(info.ipv6Address);
            struct.setIpv6Prefix(info.ipv6Prefix);

            if (!StringUtils.isEmpty(struct.getIp())) {
                if (!excludeIps.add(struct.getIp())) {
                    throw new ApiMessageInterceptionException(argerr("could not batch create host kernel interface, " +
                                    "because duplicate ipv4 address[%s] in input structs", struct.getIp()));
                }
            }
            if (!StringUtils.isEmpty(struct.getIp6())) {
                if (!excludeIpv6s.add(struct.getIp6())) {
                    throw new ApiMessageInterceptionException(argerr("could not batch create host kernel interface, " +
                            "because duplicate ipv6 address[%s] in input structs", struct.getIp6()));
                }
            }
        }
    }

    private void validate(APIUpdateHostKernelInterfaceMsg msg) {
        if (msg.getRequiredIp() == null && msg.getNetmask() != null) {
            throw new ApiMessageInterceptionException(argerr("could not update host kernel interface[uuid:%s]," +
                    " because netmask cannot be set without requiredIp", msg.getUuid()));
        }

        if (msg.getRequiredIp() != null) {
            HostKernelInterfaceVO vo = Q.New(HostKernelInterfaceVO.class).eq(HostKernelInterfaceVO_.uuid, msg.getUuid()).find();
            HostVO host = Q.New(HostVO.class).eq(HostVO_.uuid, vo.getHostUuid()).find();
            if (!host.getStatus().equals(HostStatus.Connected)) {
                throw new ApiMessageInterceptionException(argerr("could not update host kernel interface[uuid:%s]," +
                        " because host[uuid:%s] is not connected", msg.getUuid(), host.getUuid()));
            }

            PortGroupVO pg = dbf.findByUuid(vo.getL3NetworkUuid(), PortGroupVO.class);
            NicIpAddressInfo info = new NicIpAddressInfo();
            info.ipv4Address = msg.getRequiredIp();
            info.ipv4Netmask = msg.getNetmask();
            ipOperator.validateStaticIp(info, pg, new ArrayList<>());
            msg.setNetmask(info.ipv4Netmask);
        }
    }

    private void validate(APIDeleteHostKernelInterfaceMsg msg) {
        HostKernelInterfaceVO vo = Q.New(HostKernelInterfaceVO.class).eq(HostKernelInterfaceVO_.uuid, msg.getUuid()).find();
        if (VirtualSwitchSystemTags.HOST_KERNEL_DEFAULT_INTERFACE.hasTag(msg.getUuid())) {
            throw new ApiMessageInterceptionException(argerr("could not delete default host kernel interface[uuid:%s]", msg.getUuid()));
        }

        HostVO host = Q.New(HostVO.class).eq(HostVO_.uuid, vo.getHostUuid()).find();
        if (!host.getStatus().equals(HostStatus.Connected)) {
            throw new ApiMessageInterceptionException(argerr("could not delete host kernel interface[uuid:%s]," +
                    " because host[uuid:%s] is not connected", msg.getUuid(), host.getUuid()));
        }
    }

    private void validate(APIGetCandidateHostKernelInterfacesMsg msg) {
        if (msg.getCidr() != null && !NetworkUtils.isCidr(msg.getCidr())) {
            throw new ApiMessageInterceptionException(argerr("invalid CIDR: %s", msg.getCidr()));
        }
    }

    private void validate(APICreateL2VirtualSwitchMsg msg) {
        validateSystemTagsFormat(msg);

        Integer index = VirtualSwitchUtils.getVirtualSwitchIndexOfZone(msg.getZoneUuid());
        if (index != null && index > VirtualSwitchConstant.MAX_VIRTUAL_SWITCH_INDEX) {
            throw new ApiMessageInterceptionException(argerr("the index of virtual switch in zone[%s] exceeds the maximum[%s]",
                    msg.getZoneUuid(), VirtualSwitchConstant.MAX_VIRTUAL_SWITCH_INDEX));
        }
    }

    private void validateSystemTagsFormat(APICreateL2VirtualSwitchMsg msg) {
        if (CollectionUtils.isEmpty(msg.getSystemTags())) {
            if (!StringUtils.isEmpty(msg.getPhysicalInterface())) {
                throw new ApiMessageInterceptionException(argerr("need to input one system tag like: [%s]",
                        VirtualSwitchSystemTags.UPLINK_BONDING.getTagFormat()));
            }
            return;
        }

        List<String> systemTags = new ArrayList<>(msg.getSystemTags());
        List<String> oldSystemTags = new ArrayList<>();
        List<String> updatedSystemTags = new ArrayList<>();
        Set<PatternedSystemTag> set = new HashSet<>();

        for (String systemTag : systemTags) {
            if (VirtualSwitchSystemTags.UPLINK_BONDING.isMatch(systemTag)) {
                if (StringUtils.isEmpty(msg.getPhysicalInterface())) {
                    throw new ApiMessageInterceptionException(argerr("physicalInterface should not be null" +
                            " when uplink bonding is set"));
                }

                if (set.contains(VirtualSwitchSystemTags.UPLINK_BONDING)) {
                    throw new ApiMessageInterceptionException(argerr("only one systemTag for uplink bonding is allowed"));
                }

                String mode = VirtualSwitchSystemTags.UPLINK_BONDING.getTokenByTag(systemTag, VirtualSwitchSystemTags.BONDING_MODE_TOKEN);
                String xmitHashPolicy = VirtualSwitchSystemTags.UPLINK_BONDING.getTokenByTag(systemTag, VirtualSwitchSystemTags.XMIT_HASH_POLICY_TOKEN);
                if (HostNetworkBondingConstant.BONDING_MODE_AB.equals(mode)) {
                    oldSystemTags.add(systemTag);
                    updatedSystemTags.add(systemTag.replace(xmitHashPolicy, HostNetworkBondingConstant.BONDING_XMIT_HASH_POLICY_NULL));
                } else if (HostNetworkBondingConstant.BONDING_MODE_LACP.equals(mode)) {
                    List<String> policies = new ArrayList<>();
                    policies.add(HostNetworkBondingConstant.BONDING_XMIT_HASH_POLICY_LAYER_TWO);
                    policies.add(HostNetworkBondingConstant.BONDING_XMIT_HASH_POLICY_LAYER_TWO_AND_THREE);
                    policies.add(HostNetworkBondingConstant.BONDING_XMIT_HASH_POLICY_LAYER_THREE_AND_FOUR);
                    if (!policies.contains(xmitHashPolicy)) {
                        throw new ApiMessageInterceptionException(argerr("wrong xmit hash policy in system tag[%s]", systemTag));
                    }
                } else {
                    throw new ApiMessageInterceptionException(argerr("wrong bonding mode in system tag[%s]", systemTag));
                }
                set.add(VirtualSwitchSystemTags.UPLINK_BONDING);

            } else {
                throw new ApiMessageInterceptionException(argerr("wrong system tag[%s], should be like: [%s]",
                        systemTag, VirtualSwitchSystemTags.UPLINK_BONDING.getTagFormat()));
            }
        }

        systemTags.removeAll(oldSystemTags);
        systemTags.addAll(updatedSystemTags);
        msg.setSystemTags(systemTags);
    }

    private void validate(APICreateL2PortGroupMsg msg) {
        boolean vlanIdUsed = Q.New(L2PortGroupNetworkVO.class)
                .eq(L2PortGroupNetworkVO_.vSwitchUuid, msg.getvSwitchUuid())
                .eq(L2PortGroupNetworkVO_.vlanId, msg.getVlan())
                .isExists();
        if (vlanIdUsed) {
            throw new ApiMessageInterceptionException(operr("could not create L2PortGroupNetwork," +
                            " because L2VirtualSwitchNetwork[uuid:%s] already has L2PortGroupNetworks with the same vlanId[%s]"
                    , msg.getvSwitchUuid(), msg.getVlan()));
        }

        // vlanRanges have not been used yet
    }

    private void validate(APIAttachL2NetworkToClusterMsg msg) {
        L2NetworkVO vo = dbf.findByUuid(msg.getL2NetworkUuid(), L2NetworkVO.class);
        if (VirtualSwitchConstant.PORT_GROUP_NETWORK_TYPE.equals(vo.getType())) {
            throw new ApiMessageInterceptionException(operr("could not attach L2PortGroupNetwork[uuid:%s] to cluster[uuid:%s], " +
                    "which L2VirtualSwitchNetwork should be used", vo.getUuid(), msg.getClusterUuid()));
        } else if (!VirtualSwitchConstant.VIRTUAL_SWITCH_NETWORK_TYPE.equals(vo.getType())) {
            return;
        }

        boolean isKVMCluster = Q.New(ClusterVO.class)
                .eq(ClusterVO_.uuid, msg.getClusterUuid())
                .eq(ClusterVO_.type, ClusterConstant.ZSTACK_CLUSTER_TYPE)
                .eq(ClusterVO_.hypervisorType, VmInstanceConstant.KVM_HYPERVISOR_TYPE)
                .isExists();
        if (isKVMCluster) {
            if (VirtualSwitchSystemTags.VIRTUAL_SWITCH_DEFAULT.hasTag(vo.getUuid())) {
                throw new ApiMessageInterceptionException(operr("could not attach L2Network to KVM cluster, because the l2Network[uuid:%s] is default vSwitch", vo.getUuid()));
            }

            if (!StringUtils.isEmpty(vo.getPhysicalInterface())) {
                boolean bondingAlreadyUsed = SQL.New("select l2.uuid from L2VirtualSwitchNetworkVO l2, L2NetworkClusterRefVO ref" +
                                " where l2.uuid = ref.l2NetworkUuid" +
                                " and l2.physicalInterface = :physicalInterface" +
                                " and ref.clusterUuid = :clusterUuid")
                        .param("physicalInterface", vo.getPhysicalInterface())
                        .param("clusterUuid", msg.getClusterUuid())
                        .find() != null;

                if (bondingAlreadyUsed) {
                    throw new ApiMessageInterceptionException(operr("could not attach L2VirtualSwitchNetwork," +
                                    " because interface[%s] in cluster[uuid:%s] is already used for another L2VirtualSwitchNetwork"
                            , vo.getPhysicalInterface(), msg.getClusterUuid()));
                }
            }
        }

        if (msg.getL2ProviderType() == null) {
            msg.setL2ProviderType(KVMConstant.L2_PROVIDER_TYPE_LINUX_BRIDGE);
        }
    }

    private void validate(APIAttachL2NetworkToHostMsg msg) {
        L2NetworkVO vo = dbf.findByUuid(msg.getL2NetworkUuid(), L2NetworkVO.class);
        if (VirtualSwitchConstant.PORT_GROUP_NETWORK_TYPE.equals(vo.getType())) {
            throw new ApiMessageInterceptionException(operr("could not attach L2PortGroupNetwork[uuid:%s] to host[uuid:%s], " +
                    "which L2VirtualSwitchNetwork should be used", vo.getUuid(), msg.getHostUuid()));
        } else if (!VirtualSwitchConstant.VIRTUAL_SWITCH_NETWORK_TYPE.equals(vo.getType())) {
            return;
        }

        if (msg.getL2ProviderType() == null) {
            msg.setL2ProviderType(KVMConstant.L2_PROVIDER_TYPE_LINUX_BRIDGE);
        }

        boolean interfaceValid = false;
        if (!StringUtils.isEmpty(msg.getHostParam())) {
            // json format is checked in L2NetworkApiInterceptor
            HostParam hostParam = JSONObjectUtil.toObject(msg.getHostParam(), HostParam.class);
            interfaceValid = VirtualSwitchUtils.isPhysicalInterfaceValid(hostParam.getPhysicalInterface(), hostParam.getHostUuid());
            if (!interfaceValid) {
                throw new ApiMessageInterceptionException(argerr("could not attach L2VirtualSwitchNetwork[uuid:%s] to host[uuid:%s], " +
                        "because the physical interface[%s] is invalid", vo.getUuid(), msg.getHostUuid(), hostParam.getPhysicalInterface()));
            }

            if (PciDeviceUtils.checkIfPciDevicePassThroughStateIsEnabled(hostParam.getPhysicalInterface(), hostParam.getHostUuid())) {
                throw new ApiMessageInterceptionException(argerr("could not attach L2VirtualSwitchNetwork[uuid:%s] to host[uuid:%s], " +
                        "because the pass-through state of physical interface[%s] is [Enabled]", vo.getUuid(), msg.getHostUuid(), hostParam.getPhysicalInterface()));
            }
        }

        if (!interfaceValid && !VirtualSwitchUtils.isUpLinkBondingExist(vo.getUuid(), msg.getHostUuid(), vo.getPhysicalInterface())) {
            throw new ApiMessageInterceptionException(operr("could not attach L2VirtualSwitchNetwork[uuid:%s] to host[uuid:%s], " +
                    "because there is no uplink configured for the virtual switch on the host", vo.getUuid(), msg.getHostUuid()));
        }
    }

    private void validate(APIDetachL2NetworkFromClusterMsg msg) {
        L2NetworkVO vo = dbf.findByUuid(msg.getL2NetworkUuid(), L2NetworkVO.class);
        if (VirtualSwitchConstant.PORT_GROUP_NETWORK_TYPE.equals(vo.getType())) {
            throw new ApiMessageInterceptionException(operr("could not detach L2PortGroupNetwork[uuid:%s] from cluster[uuid:%s], " +
                    "which L2VirtualSwitchNetwork should be used", vo.getUuid(), msg.getClusterUuid()));
        }

        boolean isKVMCluster = Q.New(ClusterVO.class)
                .eq(ClusterVO_.uuid, msg.getClusterUuid())
                .eq(ClusterVO_.type, ClusterConstant.ZSTACK_CLUSTER_TYPE)
                .eq(ClusterVO_.hypervisorType, VmInstanceConstant.KVM_HYPERVISOR_TYPE)
                .isExists();
        if (isKVMCluster && VirtualSwitchSystemTags.VIRTUAL_SWITCH_DEFAULT.hasTag(vo.getUuid())) {
            throw new ApiMessageInterceptionException(operr("could not detach L2Network from KVM cluster, because the l2Network[uuid:%s] is default vSwitch", vo.getUuid()));
        }
    }

    private void validate(APIDetachL2NetworkFromHostMsg msg) {
        L2NetworkVO vo = dbf.findByUuid(msg.getL2NetworkUuid(), L2NetworkVO.class);
        if (VirtualSwitchConstant.PORT_GROUP_NETWORK_TYPE.equals(vo.getType())) {
            throw new ApiMessageInterceptionException(operr("could not detach L2PortGroupNetwork[uuid:%s] from host[uuid:%s], " +
                    "which L2VirtualSwitchNetwork should be used", vo.getUuid(), msg.getHostUuid()));
        }

        if (VirtualSwitchSystemTags.VIRTUAL_SWITCH_DEFAULT.hasTag(vo.getUuid())) {
            throw new ApiMessageInterceptionException(operr("could not detach L2Network from host, because the l2Network[uuid:%s] is default vSwitch", vo.getUuid()));
        }
    }

    private void validate(APICreatePortGroupMsg msg) {
        if (msg.getL2NetworkUuid() != null) {
            boolean l2BelongsToVSwitch = Q.New(L2PortGroupNetworkVO.class)
                    .eq(L2PortGroupNetworkVO_.uuid, msg.getL2NetworkUuid())
                    .eq(L2PortGroupNetworkVO_.vSwitchUuid, msg.getvSwitchUuid())
                    .isExists();
            if (!l2BelongsToVSwitch) {
                throw new ApiMessageInterceptionException(operr("could not create port group for L2Network[uuid:%s]" +
                                "that does not belong to vSwitch[uuid:%s]"
                        , msg.getL2NetworkUuid(), msg.getvSwitchUuid()));
            }
        }
    }

    private void validate(APICreateL3NetworkMsg msg) {
        L2VirtualSwitchNetworkVO vo = Q.New(L2VirtualSwitchNetworkVO.class)
                .eq(L2VirtualSwitchNetworkVO_.uuid, msg.getL2NetworkUuid()).find();
        if (vo != null) {
            throw new ApiMessageInterceptionException(argerr("could not create l3 network on virtual switch[uuid:%s]",
                    msg.getL2NetworkUuid()));
        }
    }

    private void validate(APIUpdateL2NetworkVirtualNetworkIdMsg msg) {
        L2PortGroupNetworkVO vo = dbf.findByUuid(msg.getL2NetworkUuid(), L2PortGroupNetworkVO.class);
        if (vo == null) {
            return;
        }

        if (!NetworkUtils.isValidVlan(msg.getVirtualNetworkId())) {
            throw new ApiMessageInterceptionException(argerr("vlan[%s] for port group is invalid", msg.getVirtualNetworkId()));
        }

        List<String> l3Uuids = Q.New(PortGroupVO.class).select(PortGroupVO_.uuid).eq(PortGroupVO_.l2NetworkUuid, vo.getUuid()).listValues();
        if (!VirtualSwitchSystemTags.PORT_GROUP_DEFAULT.filterResourceHasTag(l3Uuids).isEmpty()) {
            throw new ApiMessageInterceptionException(argerr("could not update vlan for port group with default port group"));
        }

        boolean vlanIdUsed = Q.New(L2PortGroupNetworkVO.class)
                .eq(L2PortGroupNetworkVO_.vSwitchUuid, vo.getvSwitchUuid())
                .eq(L2PortGroupNetworkVO_.vlanId, msg.getVirtualNetworkId())
                .notEq(L2PortGroupNetworkVO_.uuid, vo.getUuid())
                .isExists();
        if (vlanIdUsed) {
            throw new ApiMessageInterceptionException(operr("could not update vlan for port group," +
                            " because L2VirtualSwitchNetwork[uuid:%s] already has L2PortGroupNetworks with the same vlanId[%s]"
                    , vo.getvSwitchUuid(), msg.getVirtualNetworkId()));
        }
    }

    private void validate(APIUpdateVirtualSwitchUplinkBondingsMsg msg) {
        L2VirtualSwitchNetworkVO vo = dbf.findByUuid(msg.getL2NetworkUuid(), L2VirtualSwitchNetworkVO.class);
        if (VirtualSwitchSystemTags.VIRTUAL_SWITCH_DEFAULT.hasTag(vo.getUuid()) && VirtualSwitchUtils.isUplinkBondingExist(vo.getUuid())) {
            throw new ApiMessageInterceptionException(operr("could not update uplink bonding of default vSwitch" +
                    " when it is still attached to hosts with uplink bonding exist", vo.getUuid()));
        }

        if (vo.getPhysicalInterface().isEmpty() && StringUtils.isEmpty(msg.getBondingName())) {
            throw new ApiMessageInterceptionException(argerr("bondingName cannot be empty " +
                    " when virtual switch has no uplink bonding config"));
        }

        if (!StringUtils.isEmpty(msg.getBondingName())) {
            if (vo.getVSwitchIndex() == null) {
                throw new ApiMessageInterceptionException(argerr("could not update uplink bonding name" +
                        " because the version of the virtual switch[uuid:%s] is old", msg.getUuid()));
            }

            if (VirtualSwitchUtils.isUplinkBondingExist(msg.getUuid())) {
                throw new ApiMessageInterceptionException(argerr("could not update uplink bonding name" +
                        " when virtual switch has uplink bonding group", msg.getUuid()));
            }

            List<String> clusterUuids = vo.getAttachedClusterRefs()
                    .stream().map(L2NetworkClusterRefVO::getClusterUuid).collect(Collectors.toList());
            if (!clusterUuids.isEmpty()) {
                boolean bondingNameOccupied = !SQL.New("select l2.uuid from L2VirtualSwitchNetworkVO l2, L2NetworkClusterRefVO ref" +
                                " where l2.uuid = ref.l2NetworkUuid " +
                                " and l2.physicalInterface = :bondingName" +
                                " and l2.uuid != :l2Uuid" +
                                " and ref.clusterUuid in (:clusterUuids)", String.class)
                        .param("bondingName", msg.getBondingName())
                        .param("l2Uuid", msg.getUuid())
                        .param("clusterUuids", clusterUuids)
                        .list().isEmpty();

                if (bondingNameOccupied) {
                    throw new ApiMessageInterceptionException(argerr("could not update uplink bonding name" +
                            " which has been occupied by another virtual switch attached to the same cluster", msg.getUuid()));
                }
            }
        }

        ErrorCode err = HostNetworkBondingUtils.validateBondingModeAndPolicy(msg.getMode(), msg.getXmitHashPolicy());
        if (err != null) {
            throw new ApiMessageInterceptionException(err);
        }
    }

    private void validate(APIUpdateVirtualSwitchUplinkGroupMsg msg) {
        L2NetworkVO vo = dbf.findByUuid(msg.getL2NetworkUuid(), L2NetworkVO.class);
        UplinkGroupVO ug = VirtualSwitchUtils.getUplinkGroup(msg.getUuid(), msg.getHostUuid());
        if (ug == null) {
            throw new ApiMessageInterceptionException(argerr("virtual switch[uuid:%s] has not attached to host[uuid:%s]",
                    msg.getUuid(), msg.getHostUuid()));
        }

        if (msg.getSlaveUuids() == null && msg.getSlaveNames() == null) {
            throw new ApiMessageInterceptionException(argerr("require one of slaveUuids and slaveNames"));
        } else if (msg.getSlaveUuids() == null && msg.getSlaveNames() != null) {
            msg.setSlaveUuids(HostNetworkBondingUtils.getSlaveUuidsBySlaveNamesOnHosts(msg.getSlaveNames(), msg.getHostUuid()));
        }

        if (CollectionUtils.isEmpty(msg.getSlaveUuids())) {
            throw new ApiMessageInterceptionException(argerr("need input at least one slave"));
        } else if (msg.getSlaveUuids().size() > 1 && !VirtualSwitchSystemTags.UPLINK_BONDING.hasTag(msg.getUuid())) {
            throw new ApiMessageInterceptionException(argerr("virtual switch[uuid:%s] has not created uplink bonding config yet"));
        }

        if (msg.getSlaveUuids().size() > 1) {
            String mode = VirtualSwitchSystemTags.UPLINK_BONDING.getTokenByResourceUuid(msg.getUuid(),
                    VirtualSwitchSystemTags.BONDING_MODE_TOKEN);

            String bondingUuid = Q.New(HostNetworkBondingVO.class)
                    .select(HostNetworkBondingVO_.uuid)
                    .eq(HostNetworkBondingVO_.hostUuid, msg.getHostUuid())
                    .eq(HostNetworkBondingVO_.bondingName, vo.getPhysicalInterface())
                    .findValue();

            if (UplinkGroupType.PhysicalInterface.equals(ug.getType())) {
                String oldInterfaceUuid = Q.New(HostNetworkInterfaceVO.class)
                        .select(HostNetworkInterfaceVO_.uuid)
                        .eq(HostNetworkInterfaceVO_.hostUuid, msg.getHostUuid())
                        .eq(HostNetworkInterfaceVO_.interfaceName, ug.getInterfaceName())
                        .findValue();
                List<String> checkSlaveUuids = new ArrayList<>(msg.getSlaveUuids());
                checkSlaveUuids.remove(oldInterfaceUuid);

                msg.setSlaveUuids(HostNetworkBondingUtils.checkAndGetBondingSlavesOnHost(
                        checkSlaveUuids, msg.getHostUuid(), bondingUuid));
                msg.getSlaveUuids().add(oldInterfaceUuid);
            } else {
                msg.setSlaveUuids(HostNetworkBondingUtils.checkAndGetBondingSlavesOnHost(
                        msg.getSlaveUuids(), msg.getHostUuid(), bondingUuid));
            }

            HostNetworkBondingUtils.checkBondingSlavesAmountWithCertainMode(msg.getSlaveUuids().size(), mode);
        }

        if (msg.getType() != null && !HostNetworkBondingConstant.LINUX_BONDING_TYPE.equals(msg.getType())) {
            throw new ApiMessageInterceptionException(argerr("invalid bonding type[%s]", msg.getType()));
        }

        boolean interfaceToBonding = UplinkGroupType.PhysicalInterface.equals(ug.getType()) && msg.getSlaveUuids().size() > 1;
        if (interfaceToBonding) {
            boolean bondingExist = Q.New(HostNetworkBondingVO.class)
                    .eq(HostNetworkBondingVO_.hostUuid, msg.getHostUuid())
                    .eq(HostNetworkBondingVO_.bondingName, vo.getPhysicalInterface())
                    .isExists();
            if (bondingExist) {
                throw new ApiMessageInterceptionException(operr("cannot update uplink to bonding," +
                                " because bonding[%s] already exists on host[uuid:%s]",
                        vo.getPhysicalInterface(), msg.getHostUuid()));
            }
        }
    }

    private void validate(APIUpdateBondingMsg msg) {
        HostNetworkBondingVO bondingVO = dbf.findByUuid(msg.getUuid(), HostNetworkBondingVO.class);
        String vSwitchUuid = VirtualSwitchUtils.getVSwitchUuidOfUplinkGroup(bondingVO.getBondingName(), bondingVO.getHostUuid());
        if (vSwitchUuid == null) {
            return;
        }

        boolean modeUpdated = msg.getMode() != null && !msg.getMode().equals(bondingVO.getMode());
        boolean xmitHashPolicyUpdated = msg.getXmitHashPolicy() != null && !msg.getXmitHashPolicy().equals(bondingVO.getXmitHashPolicy());
        if (modeUpdated || xmitHashPolicyUpdated) {
            throw new ApiMessageInterceptionException(argerr("could not update mode or xmit_hash_policy" +
                    " of bonding[uuid:%s] which is in use by virtual switch[uuid:%s]", bondingVO.getUuid(), vSwitchUuid));
        }
    }

    private void validate(APIDeleteBondingMsg msg) {
        HostNetworkBondingVO bondingVO = Q.New(HostNetworkBondingVO.class).eq(HostNetworkBondingVO_.uuid, msg.getUuid()).find();
        String vSwitchUuid = VirtualSwitchUtils.getVSwitchUuidOfUplinkGroup(bondingVO.getBondingName(), bondingVO.getHostUuid());

        if (vSwitchUuid != null) {
            throw new ApiMessageInterceptionException(operr("could not delete bonding[uuid:%s], because it is in use by virtual switch[uuid:%s]",
                    bondingVO.getUuid(), vSwitchUuid));
        }
    }
}
