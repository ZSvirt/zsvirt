package org.zstack.compute.vdpa;

import org.apache.commons.lang.StringUtils;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.compute.bonding.HostNetworkBondingConstant;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.network.hostNetworkInterface.HostNetworkBondingVO;
import org.zstack.network.hostNetworkInterface.HostNetworkBondingVO_;
import org.zstack.network.hostNetworkInterface.HostNetworkInterfaceVO;
import org.zstack.network.hostNetworkInterface.HostNetworkInterfaceVO_;
import org.zstack.header.network.l2.L2NetworkConstant;
import org.zstack.header.network.l2.L2NetworkVO;
import org.zstack.header.network.l2.L2NetworkVO_;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.network.l3.L3NetworkVO_;
import org.zstack.header.vdpa.VmVdpaNicVO;
import org.zstack.header.vdpa.VmVdpaNicVO_;
import org.zstack.header.vm.VmNicVO;
import org.zstack.header.vm.VmNicVO_;
import org.zstack.network.service.NetworkServiceGlobalConfig;
import org.zstack.pciDevice.*;
import org.zstack.pciDevice.virtual.PciDeviceVirtStatus;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class VmVdpaNicUtils {
    private static final CLogger logger = Utils.getLogger(VmVdpaNicUtils.class);

    /**
     * Get all reserved vdpa nic pci devices reserved for vm.
     * @param  vmUuid uuid of vm
     * @return all reserved vdpa nic pci devices uuid of vm
     */
    public static List<String> getReservedPciDevicesForVdpaNic(String vmUuid) {
        List<String> pciDeviceUuids = Q.New(PciDeviceVO.class)
                .select(PciDeviceVO_.uuid)
                .eq(PciDeviceVO_.vmInstanceUuid, vmUuid)
                .eq(PciDeviceVO_.status, PciDeviceStatus.Reserved)
                .eq(PciDeviceVO_.type, PciDeviceType.Ethernet_Controller)
                .listValues();

        return pciDeviceUuids;
    }

    /**
     * Get physical interface of l2 network by the uuid of l3 network.
     * @param l3Uuid l3 network uuid
     * @return physical interface of l2 network
     */
    public static String getL2NicNameFromL3Uuid(String l3Uuid) {
        String l2Uuid = Q.New(L3NetworkVO.class).eq(L3NetworkVO_.uuid, l3Uuid).select(L3NetworkVO_.l2NetworkUuid).findValue();
        return Q.New(L2NetworkVO.class).eq(L2NetworkVO_.uuid, l2Uuid).select(L2NetworkVO_.physicalInterface).findValue();
    }

    /**
     * get reserved pci device of l3 network
     * @param vmUuid
     * @param l3NetworkUuid
     * @param hostUuid
     * @return
     */
    public static String getPciDeviceUnderL3Network(String vmUuid, String l3NetworkUuid, String hostUuid) {
        String l2NicName = getL2NicNameFromL3Uuid(l3NetworkUuid);
        List<String> pfAddrs = new ArrayList<>();
        HostNetworkBondingVO bonding = Q.New(HostNetworkBondingVO.class)
                .eq(HostNetworkBondingVO_.hostUuid, hostUuid)
                .eq(HostNetworkBondingVO_.bondingName, l2NicName)
                .eq(HostNetworkBondingVO_.type, HostNetworkBondingConstant.OVS_BONDING_TYPE)
                .find();
        if (bonding != null) {  // get vf from bond slaves
            pfAddrs.addAll(bonding.getSlaves().stream().map(HostNetworkInterfaceVO::getPciDeviceAddress).collect(Collectors.toList()));
        } else {                // get vf from an normal interface
            String pfUuid = Q.New(HostNetworkInterfaceVO.class)
                    .eq(HostNetworkInterfaceVO_.hostUuid, hostUuid)
                    .eq(HostNetworkInterfaceVO_.interfaceName, l2NicName)
                    .select(HostNetworkInterfaceVO_.pciDeviceAddress)
                    .findValue();
            if (pfUuid != null) {
                pfAddrs.add(pfUuid);
            }
        }

        if (pfAddrs.isEmpty()) {
            logger.debug(String.format("no interface named[%s] in host[uuid:%s] support vdpa nic", l2NicName, hostUuid));
            return null;
        }

        List<String> pfUuids = Q.New(PciDeviceVO.class)
                .eq(PciDeviceVO_.hostUuid, hostUuid)
                .in(PciDeviceVO_.pciDeviceAddress, pfAddrs)
                .eq(PciDeviceVO_.type, PciDeviceType.Ethernet_Controller)
                .eq(PciDeviceVO_.virtStatus, PciDeviceVirtStatus.SRIOV_VIRTUALIZED)
                .eq(PciDeviceVO_.state, PciDeviceState.Enabled)
                .notEq(PciDeviceVO_.status, PciDeviceStatus.Attached)
                .select(PciDeviceVO_.uuid)
                .listValues();

        if (pfUuids.isEmpty()) {
            logger.debug(String.format("no usable vdpa pf of nic name[%s] in host[uuid:%s]", l2NicName, hostUuid));
            return null;
        }

        List<String> reservedVdpaNicPciDevices = getReservedPciDevicesForVdpaNic(vmUuid);
        if (reservedVdpaNicPciDevices.isEmpty()) {
            logger.debug(String.format("no reserved vdpa in host[uuid:%s]", hostUuid));
            return null;
        }

        Q query = Q.New(PciDeviceVO.class)
                .eq(PciDeviceVO_.hostUuid, hostUuid)
                .eq(PciDeviceVO_.virtStatus, PciDeviceVirtStatus.SRIOV_VIRTUAL)
                .in(PciDeviceVO_.parentUuid, pfUuids)
                .in(PciDeviceVO_.uuid, reservedVdpaNicPciDevices);

        return query.select(PciDeviceVO_.uuid).limit(1).findValue();
    }

    public static boolean hasEnoughPciDevices(String l3NetworkUuid, String hostUuid) {
        String l2NicName = getL2NicNameFromL3Uuid(l3NetworkUuid);
        return hasEnoughPciDevicesByL2PhysicalInterface(l2NicName, hostUuid);
    }

    public static boolean hasEnoughPciDevicesByL2PhysicalInterface(String l2NicName, String hostUuid) {
        List<String> pfUuids = getPciDevicesByL2PhysicalInterface(l2NicName, hostUuid);
        if (pfUuids == null || pfUuids.isEmpty()) {
            return false;
        }

        Long unoccupiedPciDevices = Q.New(PciDeviceVO.class)
                .eq(PciDeviceVO_.hostUuid, hostUuid)
                .eq(PciDeviceVO_.virtStatus, PciDeviceVirtStatus.SRIOV_VIRTUAL)
                .in(PciDeviceVO_.parentUuid, pfUuids)
                .eq(PciDeviceVO_.type, PciDeviceType.Ethernet_Controller)
                .eq(PciDeviceVO_.status, PciDeviceStatus.System).count();

        if (unoccupiedPciDevices > 0) {
            return true;
        }
        return false;
    }

    public static List<String> getPciDevicesByL2PhysicalInterface(String l2NicName, String hostUuid) {
        List<String> pfAddrs = new ArrayList<>();
        HostNetworkBondingVO bonding = Q.New(HostNetworkBondingVO.class)
                .eq(HostNetworkBondingVO_.hostUuid, hostUuid)
                .eq(HostNetworkBondingVO_.bondingName, l2NicName)
                .eq(HostNetworkBondingVO_.type, HostNetworkBondingConstant.OVS_BONDING_TYPE)
                .find();

        if (bonding != null) {
            pfAddrs.addAll(bonding.getSlaves()
                    .stream()
                    .map(HostNetworkInterfaceVO::getPciDeviceAddress)
                    .collect(Collectors.toList()));
        } else {
            String pfUuid = Q.New(HostNetworkInterfaceVO.class)
                    .eq(HostNetworkInterfaceVO_.hostUuid, hostUuid)
                    .eq(HostNetworkInterfaceVO_.interfaceName, l2NicName)
                    .select(HostNetworkInterfaceVO_.pciDeviceAddress)
                    .findValue();
            if (pfUuid != null) {
                pfAddrs.add(pfUuid);
            }
        }

        if (pfAddrs.isEmpty()) {
            logger.debug(String.format("no interface named[%s] in host[uuid:%s] support vdpa nic",
                    l2NicName, hostUuid));
            return Collections.emptyList();
        }

        List<String> pfUuids = Q.New(PciDeviceVO.class)
                .eq(PciDeviceVO_.hostUuid, hostUuid)
                .in(PciDeviceVO_.pciDeviceAddress, pfAddrs)
                .eq(PciDeviceVO_.type, PciDeviceType.Ethernet_Controller)
                .eq(PciDeviceVO_.virtStatus, PciDeviceVirtStatus.SRIOV_VIRTUALIZED)
                .eq(PciDeviceVO_.state, PciDeviceState.Enabled)
                .notEq(PciDeviceVO_.status, PciDeviceStatus.Attached)
                .select(PciDeviceVO_.uuid)
                .listValues();

        if (pfUuids.isEmpty()) {
            logger.debug(String.format("no usable vdpa pf of nic name[%s] in host[uuid:%s]", l2NicName, hostUuid));
        }
        return pfUuids;
    }

    /**
     * Select a vdpa nic from the host for vm.
     * @param vmUuid uuid of the vm
     * @param hostUuid uuid of the host
     * @param l3Uuid uuid of l3 network
     * @param accessiblePciUuids the pcis that account can access, null means the account is admin
     * @return vf nic uuid
     */
    @Transactional
    public static String selectRandomVdpaNicPciDevice(String vmUuid, String hostUuid, String l3Uuid, List<String> accessiblePciUuids) {
        /**
         *  if l2NicName is a bond with single slave, then try to get vf from the bond slave;
         *  otherwise get vf from single pf.
         *  vdpa only support dpdk bond now.
         */
        String l2NicName = getL2NicNameFromL3Uuid(l3Uuid);
        List<String> pfAddrs = new ArrayList<>();
        HostNetworkBondingVO bonding = Q.New(HostNetworkBondingVO.class)
                .eq(HostNetworkBondingVO_.hostUuid, hostUuid)
                .eq(HostNetworkBondingVO_.bondingName, l2NicName)
                .eq(HostNetworkBondingVO_.type, HostNetworkBondingConstant.OVS_BONDING_TYPE)
                .find();
        if (bonding != null) {  // get vf from bond slaves
            pfAddrs.addAll(bonding.getSlaves().stream().map(HostNetworkInterfaceVO::getPciDeviceAddress).collect(Collectors.toList()));
        } else {                // get vf from an normal interface
            String pfUuid = Q.New(HostNetworkInterfaceVO.class)
                    .eq(HostNetworkInterfaceVO_.hostUuid, hostUuid)
                    .eq(HostNetworkInterfaceVO_.interfaceName, l2NicName)
                    .select(HostNetworkInterfaceVO_.pciDeviceAddress)
                    .findValue();
            if (pfUuid != null) {
                pfAddrs.add(pfUuid);
            }
        }

        if (pfAddrs.isEmpty()) {
            logger.debug(String.format("no interface named[%s] in host[uuid:%s] support vdpa nic", l2NicName, hostUuid));
            return null;
        }

        List<String> pfUuids = Q.New(PciDeviceVO.class)
                .eq(PciDeviceVO_.hostUuid, hostUuid)
                .in(PciDeviceVO_.pciDeviceAddress, pfAddrs)
                .eq(PciDeviceVO_.type, PciDeviceType.Ethernet_Controller)
                .eq(PciDeviceVO_.virtStatus, PciDeviceVirtStatus.SRIOV_VIRTUALIZED)
                .eq(PciDeviceVO_.state, PciDeviceState.Enabled)
                .notEq(PciDeviceVO_.status, PciDeviceStatus.Attached)
                .select(PciDeviceVO_.uuid)
                .listValues();

        if (pfUuids.isEmpty()) {
            logger.debug(String.format("no usable sriov pf of nic name[%s] in host[uuid:%s]", l2NicName, hostUuid));
            return null;
        }

        List<String> reservedVdpaNicPciDevices = getReservedPciDevicesForVdpaNic(vmUuid);
        Q query = Q.New(PciDeviceVO.class)
                .eq(PciDeviceVO_.hostUuid, hostUuid)
                .isNull(PciDeviceVO_.vmInstanceUuid)
                .eq(PciDeviceVO_.type, PciDeviceType.Ethernet_Controller)
                .eq(PciDeviceVO_.virtStatus, PciDeviceVirtStatus.SRIOV_VIRTUAL)
                .in(PciDeviceVO_.parentUuid, pfUuids)
                .eq(PciDeviceVO_.state, PciDeviceState.Enabled)
                .notEq(PciDeviceVO_.status, PciDeviceStatus.Attached);

        if (!reservedVdpaNicPciDevices.isEmpty()) {
            query = query.notIn(PciDeviceVO_.uuid, reservedVdpaNicPciDevices);
        }

        if (accessiblePciUuids != null) {
            query = query.in(PciDeviceVO_.uuid, accessiblePciUuids);
        }

        return query.select(PciDeviceVO_.uuid).limit(1).findValue();
    }

    /**
     * generate src path for vdpa.
     * @param vdpaVO
     * @return
     */
    public static String generateSrcPath(VmVdpaNicVO vdpaVO) {
        return String.format("/var/run/zstack/vdpa/%s/%s", vdpaVO.getVmInstanceUuid(), vdpaVO.getInternalName());
    }

    /**
     * Allocate vdpa nic pci device to the vm.
     * @param vdpaNicUuid uuid of vf nic
     * @param pciUuid uuid of pci device
     */
    @Transactional
    public static void allocatePciDeviceToVdpaNic(String vdpaNicUuid, String pciUuid) {
        VmVdpaNicVO vdpa = Q.New(VmVdpaNicVO.class).eq(VmVdpaNicVO_.uuid, vdpaNicUuid).find();
        if (vdpa == null) {
            return;
        }

        SQL.New(PciDeviceVO.class)
                .eq(PciDeviceVO_.uuid, pciUuid)
                .set(PciDeviceVO_.vmInstanceUuid, vdpa.getVmInstanceUuid())
                .set(PciDeviceVO_.status, PciDeviceStatus.Attached)
                .update();

        if (vdpa.getPciDeviceUuid() != null) {
            SQL.New(VmVdpaNicVO.class)
                    .eq(VmVdpaNicVO_.uuid, vdpaNicUuid)
                    .set(VmVdpaNicVO_.lastPciDeviceUuid, vdpa.getPciDeviceUuid())
                    .set(VmVdpaNicVO_.pciDeviceUuid, pciUuid)
                    .set(VmVdpaNicVO_.srcPath, generateSrcPath(vdpa))
                    .update();
        } else {
            SQL.New(VmVdpaNicVO.class)
                    .eq(VmVdpaNicVO_.uuid, vdpaNicUuid)
                    .set(VmVdpaNicVO_.pciDeviceUuid, pciUuid)
                    .set(VmVdpaNicVO_.srcPath, generateSrcPath(vdpa))
                    .update();
        }

        logger.debug(String.format("allocated pci device[uuid:%s] to vdpa nic[uuid:%s] of vm[uuid:%s]", pciUuid, vdpa.getUuid(), vdpa.getVmInstanceUuid()));
    }


    /**
     * Release pci device from the vdpa nic.
     * @param vdpaNicUuid uuid of vf nic
     */
    @Transactional
    public static void releaseVdpaNicPciDevice(String vdpaNicUuid) {
        VmVdpaNicVO vdpa = Q.New(VmVdpaNicVO.class).eq(VmVdpaNicVO_.uuid, vdpaNicUuid).find();
        if (vdpa == null || (StringUtils.isEmpty(vdpa.getPciDeviceUuid()) && StringUtils.isEmpty(vdpa.getSrcPath()))) {
            return;
        }

        SQL.New(PciDeviceVO.class)
                .eq(PciDeviceVO_.uuid, vdpa.getPciDeviceUuid())
                .set(PciDeviceVO_.vmInstanceUuid, null)
                .set(PciDeviceVO_.status, PciDeviceStatus.System)
                .update();

        SQL.New(VmVdpaNicVO.class)
                .eq(VmVdpaNicVO_.uuid, vdpaNicUuid)
                .set(VmVdpaNicVO_.pciDeviceUuid, null)
                .set(VmVdpaNicVO_.lastPciDeviceUuid, null)
                .set(VmVdpaNicVO_.srcPath, null)
                .update();

        logger.debug(String.format("relesed pci device[uuid:%s] from vf nic[uuid:%s] of vm[uuid:%s]",
                vdpa.getPciDeviceUuid(), vdpa.getUuid(), vdpa.getVmInstanceUuid()));
    }

    public static void releaseVdpaNicPciDeviceOnVm(String vmUuid) {
        List<String> vdpaNicUuids = Q.New(VmVdpaNicVO.class).select(VmVdpaNicVO_.uuid).eq(VmVdpaNicVO_.vmInstanceUuid, vmUuid).listValues();

        if (vdpaNicUuids.isEmpty()) {
            return;
        }

        vdpaNicUuids.forEach(VmVdpaNicUtils::releaseVdpaNicPciDevice);
        logger.debug(String.format("detached vdpa nic pci devices from vm[uuid:%s]", vmUuid));
    }

    @Transactional
    public static void releasePciDevice(String pciUuid) {

        SQL.New(PciDeviceVO.class)
                .eq(PciDeviceVO_.uuid, pciUuid)
                .set(PciDeviceVO_.vmInstanceUuid, null)
                .set(PciDeviceVO_.status, PciDeviceStatus.System)
                .update();

        logger.debug(String.format("relesed pci device[uuid:%s]", pciUuid));
    }

    @Transactional
    public static void rollbackVdpaNicPciDevice(String vdpaNicUuid) {
        VmVdpaNicVO vdpa = Q.New(VmVdpaNicVO.class).eq(VmVdpaNicVO_.uuid, vdpaNicUuid).find();
        if (vdpa == null || StringUtils.isEmpty(vdpa.getPciDeviceUuid())) {
            return;
        }

        SQL.New(VmVdpaNicVO.class)
                .eq(VmVdpaNicVO_.uuid, vdpaNicUuid)
                .set(VmVdpaNicVO_.pciDeviceUuid, vdpa.getLastPciDeviceUuid())
                .set(VmVdpaNicVO_.lastPciDeviceUuid, null)
                .update();

        logger.debug(String.format("rollback pci device from [uuid:%s] to [uuid:%s] of vm[uuid:%s]",
                vdpa.getPciDeviceUuid(), vdpa.getLastPciDeviceUuid(), vdpa.getVmInstanceUuid()));
    }

    public static void rollbackVdpaNicPciDeviceOnVm(String vmUuid) {
        List<String> vdpaNicUuids = Q.New(VmVdpaNicVO.class).select(VmVdpaNicVO_.uuid).eq(VmVdpaNicVO_.vmInstanceUuid, vmUuid).listValues();

        if (vdpaNicUuids.isEmpty()) {
            return;
        }

        vdpaNicUuids.forEach(VmVdpaNicUtils::rollbackVdpaNicPciDevice);
        logger.debug(String.format("rollback vdpa nic pci devices of vm[uuid:%s]", vmUuid));
    }

    /**
     * Get vdpa nic not allocated l3 uuids in the vm.
     * For existing vms, get from VmVdpaNicVO whose pciDeviceUuid & srcPath is null;
     * For vms in creating, get from who's l2 vSwitchType is OVS;
     * @param vmUuid vm instance uuid
     * @return vf nic not allocated l3 uuids in the vm
     */
    public static List<String> getVdpaNicNotAllocatedL3UuidsInVm(String vmUuid, List<String> hostL3Uuids) {
        List<String> l3Uuids = Q.New(VmVdpaNicVO.class)
                .eq(VmVdpaNicVO_.vmInstanceUuid, vmUuid)
                .isNull(VmVdpaNicVO_.pciDeviceUuid)
                .isNull(VmVdpaNicVO_.srcPath)
                .select(VmVdpaNicVO_.l3NetworkUuid)
                .listValues();

        if (l3Uuids.isEmpty()) {
            for (String hostL3Uuid: hostL3Uuids) {
                String l2Uuid = Q.New(L3NetworkVO.class)
                        .eq(L3NetworkVO_.uuid, hostL3Uuid)
                        .select(L3NetworkVO_.l2NetworkUuid)
                        .findValue();
                boolean isOvs = Q.New(L2NetworkVO.class)
                        .eq(L2NetworkVO_.uuid, l2Uuid)
                        .eq(L2NetworkVO_.vSwitchType, L2NetworkConstant.VSWITCH_TYPE_OVS_DPDK)
                        .isExists();
                if (isOvs && !NetworkServiceGlobalConfig.ENABLE_VHOSTUSER.value(Boolean.class)) {
                    l3Uuids.add(hostL3Uuid);
                }
            }
        }

        return l3Uuids;
    }

    /**
     * Get nic type of the l3 network of vm.
     * @param vmUuid vm instance uuid
     * @param l3Uuid l3 network uuid
     * @return nic type
     */
    public static String getVmNicTypeOnL3Network(String vmUuid, String l3Uuid) {
        String nicType = Q.New(VmNicVO.class).select(VmNicVO_.type)
                .eq(VmNicVO_.vmInstanceUuid, vmUuid)
                .eq(VmNicVO_.l3NetworkUuid, l3Uuid)
                .limit(1)
                .findValue();
        return nicType;
    }
}
