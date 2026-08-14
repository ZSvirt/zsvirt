package org.zstack.compute.vdpa;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.compute.cluster.MevocoClusterGlobalConfig;
import org.zstack.compute.sriov.SriovSystemTags;
import org.zstack.compute.sriov.VfPciDeviceUtils;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.GlobalApiMessageInterceptor;
import org.zstack.header.apimediator.InterceptorForService;
import org.zstack.header.host.*;
import org.zstack.header.message.APIMessage;
import org.zstack.header.network.l2.*;
import org.zstack.header.vdpa.VmVdpaNicConstant;
import org.zstack.network.hostNetworkInterface.HostNetworkBondingVO;
import org.zstack.network.hostNetworkInterface.HostNetworkBondingVO_;
import org.zstack.network.hostNetworkInterface.HostNetworkInterfaceVO;
import org.zstack.network.hostNetworkInterface.HostNetworkInterfaceVO_;
import org.zstack.network.service.NetworkServiceGlobalConfig;
import org.zstack.pciDevice.PciDeviceVO;
import org.zstack.pciDevice.PciDeviceVO_;
import org.zstack.pciDevice.virtual.sr_iov.APIGenerateSriovPciDevicesMsg;
import org.zstack.pciDevice.virtual.sr_iov.APIUngenerateSriovPciDevicesMsg;
import org.zstack.resourceconfig.ResourceConfig;
import org.zstack.resourceconfig.ResourceConfigFacade;
import org.zstack.tag.ValidateSystemTagExtensionPoint;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.zstack.core.Platform.*;

/**
 * Created by haibiao.xiao on 4/8/2021
 */
@InterceptorForService("vdpa")
public class VmVdpaNicApiInterceptor implements GlobalApiMessageInterceptor, ValidateSystemTagExtensionPoint {
    private static final CLogger logger = Utils.getLogger(VmVdpaNicApiInterceptor.class);
    private static final VfPciDeviceUtils vfPciDeviceUtils = new VfPciDeviceUtils();

    @Autowired
    protected CloudBus bus;
    @Autowired
    protected ResourceConfigFacade rcf;

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APICreateL2NetworkMsg) {
            validate((APICreateL2NetworkMsg) msg);
        } else if (msg instanceof APIAttachL2NetworkToClusterMsg) {
            validate((APIAttachL2NetworkToClusterMsg) msg);
        } else if (msg instanceof APIGenerateSriovPciDevicesMsg) {
            validate((APIGenerateSriovPciDevicesMsg)msg);
        } else if (msg instanceof APIUngenerateSriovPciDevicesMsg) {
            validate((APIUngenerateSriovPciDevicesMsg)msg);
        }
        return msg;
    }

    private void validate(APIUngenerateSriovPciDevicesMsg msg) {
        List<String> l2Uuids = getL2ByPciDevice(msg.getPciDeviceUuid());
        if (!l2Uuids.isEmpty()) {
            throw new ApiMessageInterceptionException(argerr("could not ungenerate pci device[uuid:%s], because" +
                    "there are another l2[uuid:%s] use the physical network interface attached to cluster", msg.getPciDeviceUuid(), l2Uuids));
        }
    }

    private void validate(APIGenerateSriovPciDevicesMsg msg) {
        List<String> l2Uuids = getL2ByPciDevice(msg.getPciDeviceUuid());
        if (!l2Uuids.isEmpty()) {
            throw new ApiMessageInterceptionException(argerr("could not generate pci device[uuid:%s], because" +
                    "there are another l2[uuid:%s] use the physical network interface attached to cluster", msg.getPciDeviceUuid(), l2Uuids));
        }
    }

    private List getL2ByPciDevice(String pciDeviceUuid) {
        //find l2 from pci device
        PciDeviceVO pciDeviceVO = Q.New(PciDeviceVO.class).eq(PciDeviceVO_.uuid, pciDeviceUuid).find();
        String clusterUuid = Q.New(HostVO.class).eq(HostVO_.uuid, pciDeviceVO.getHostUuid()).select(HostVO_.clusterUuid).findValue();

        final boolean isOvsSupport = ifClusterSupportOvsDpdk(clusterUuid);

        if (!isOvsSupport) {
            return Collections.EMPTY_LIST;
        }
        HostNetworkInterfaceVO hostNetworkInterfaceVO = Q.New(HostNetworkInterfaceVO.class).eq(HostNetworkInterfaceVO_.pciDeviceAddress, pciDeviceVO.getPciDeviceAddress()).eq(HostNetworkInterfaceVO_.hostUuid, pciDeviceVO.getHostUuid()).find();

        if (hostNetworkInterfaceVO == null){
            return Collections.EMPTY_LIST;
        }
        List<String> hostNetworkInterfaceNames = new ArrayList<>();
        hostNetworkInterfaceNames.add(hostNetworkInterfaceVO.getInterfaceName());
        if (hostNetworkInterfaceVO.getBondingUuid() != null) {
            String bondName = Q.New(HostNetworkBondingVO.class).select(HostNetworkBondingVO_.bondingName).eq(HostNetworkBondingVO_.uuid, hostNetworkInterfaceVO.getBondingUuid()).findValue();
            hostNetworkInterfaceNames.add(bondName);
        }

        //if l2 attach to cluster, will exsit bridge
        List<String> l2Uuids = SQL.New("select distinct l2.uuid from L2NetworkVO l2, L2NetworkClusterRefVO ref where" +
                        " l2.uuid = ref.l2NetworkUuid" +
                        " and l2.vSwitchType = :vSwitchType" +
                        " and ref.clusterUuid = :clusterUuid" +
                        " and l2.physicalInterface in :physicalInterfaces")
                .param("vSwitchType", L2NetworkConstant.VSWITCH_TYPE_OVS_DPDK)
                .param("clusterUuid", clusterUuid)
                .param("physicalInterfaces", hostNetworkInterfaceNames)
                .list();
        return l2Uuids;
    }

    private void checkL2NetworkTypeSupportVdpa(String l2Type) {
        if (!VmVdpaNicConstant.VDPA_L2_NETWORK_TYPES.contains(l2Type)) {
            throw new ApiMessageInterceptionException(argerr("only %s support vdpa", VmVdpaNicConstant.VDPA_L2_NETWORK_TYPES));
        }
    }

    private String getL2NetworkVswitchType(String l2Uuid) {
        String vSwitchType = Q.New(L2NetworkVO.class)
                .select(L2NetworkVO_.vSwitchType)
                .eq(L2NetworkVO_.uuid, l2Uuid)
                .findValue();

        return vSwitchType;
    }

    private boolean ifClusterSupportOvsDpdk(String clusterUuid){
        boolean isOvsDpdkSup = false;
        ResourceConfig ovsDpdkSup = rcf.getResourceConfig(MevocoClusterGlobalConfig.OVS_DPDK_SUPPORT.getIdentity());
        if (ovsDpdkSup != null) {
            isOvsDpdkSup = ovsDpdkSup.getResourceConfigValue(clusterUuid, Boolean.class);
        }
        return isOvsDpdkSup;
    }

    private void checkConflictBetweenDifferentVswitchType(String l2Uuid, String clusterUuid) {
        /**
         * l2Networks with same physicalInterface in one cluster
         * can not be different vSwitchType.
         */
        List<String> l2s = Q.New(L2NetworkClusterRefVO.class)
                .select(L2NetworkClusterRefVO_.l2NetworkUuid)
                .eq(L2NetworkClusterRefVO_.clusterUuid, clusterUuid)
                .listValues();
        L2NetworkVO l2Vo = Q.New(L2NetworkVO.class).eq(L2NetworkVO_.uuid, l2Uuid).find();
        List<String> l2Ds = new ArrayList<>();
        if (l2Vo != null){
            l2Ds = Q.New(L2NetworkVO.class)
                    .select(L2NetworkVO_.uuid)
                    .eq(L2NetworkVO_.physicalInterface, l2Vo.getPhysicalInterface())
                    .notEq(L2NetworkVO_.vSwitchType, l2Vo.getvSwitchType())
                    .listValues();
            if (!Collections.disjoint(l2s, l2Ds)) {
                throw new ApiMessageInterceptionException(argerr("can not create %s with physical interface:[%s] which was already been used by another vSwitch type.", l2Vo.getvSwitchType(), l2Vo.getPhysicalInterface()));
            }
        }
    }

    private void validate(APICreateL2NetworkMsg msg) {
        if (!VmVdpaNicConstant.VDPA_VSWITCH_TYPES.contains(msg.getvSwitchType())) {
            return;
        }

        checkL2NetworkTypeSupportVdpa(msg.getType());
    }

    private void validate(APIAttachL2NetworkToClusterMsg msg) {
        checkConflictBetweenDifferentVswitchType(msg.getL2NetworkUuid(), msg.getClusterUuid());

        final String vSwitchType = getL2NetworkVswitchType(msg.getL2NetworkUuid());
        if (!vSwitchType.equals(L2NetworkConstant.VSWITCH_TYPE_OVS_DPDK)) {
            return;
        }

        final boolean isOvsSupport = ifClusterSupportOvsDpdk(msg.getClusterUuid());

        if (!isOvsSupport) {
            throw new ApiMessageInterceptionException(operr("cluster[uuid:%s] do not support ovs-dpdk", msg.getClusterUuid()));
        }

        boolean enableVhostUser = NetworkServiceGlobalConfig.ENABLE_VHOSTUSER.value(Boolean.class);
        if (enableVhostUser) {
            return;
        }

        //if vdpa, check cluster all host split the pf
        List<String> hostUuids = Q.New(HostVO.class).select(HostVO_.uuid).eq(HostVO_.clusterUuid, msg.getClusterUuid()).listValues();
        for (String hostUuid : hostUuids) {
            boolean hasVf = vfPciDeviceUtils.hasAvailableVfDeviceForL2(hostUuid, msg.getL2NetworkUuid());
            if (!hasVf) {
                throw new ApiMessageInterceptionException(
                        argerr("l2 network[uuid:%s] in host[uuid:%s] is not sr-iov virtualized",
                                msg.getL2NetworkUuid(), hostUuid));
            }
        }
    }

    @Override
    public List<Class> getMessageClassToIntercept() {
        return Arrays.asList(
                APICreateL2NetworkMsg.class,
                APIAttachL2NetworkToClusterMsg.class,
                APIGenerateSriovPciDevicesMsg.class,
                APIUngenerateSriovPciDevicesMsg.class
        );
    }

    @Override
    public InterceptorPosition getPosition() {
        return InterceptorPosition.END;
    }

    @Override
    public boolean validateSystemTag(String resourceUuid, String resourceType, String tag) {
        if (!resourceType.equals("L2NetworkVO")) {
            return true;
        }

        boolean isL2OvsType = Q.New(L2NetworkVO.class).eq(L2NetworkVO_.uuid, resourceUuid)
                .eq(L2NetworkVO_.vSwitchType, L2NetworkConstant.VSWITCH_TYPE_OVS_DPDK)
                .isExists();

        if (isL2OvsType && tag.equals(SriovSystemTags.L2_ENABLE_SRIOV.toString())) {
            return false;
        }

        return true;
    }
}
