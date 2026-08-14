package org.zstack.compute.sriov;

import org.apache.commons.lang.StringUtils;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.db.SimpleQuery;
import org.zstack.header.network.l2.L2NetworkVO;
import org.zstack.header.network.l2.L2NetworkVO_;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.network.l3.L3NetworkVO_;
import org.zstack.header.sriov.VmVfNicVO;
import org.zstack.header.sriov.VmVfNicVO_;
import org.zstack.header.tag.SystemTagVO;
import org.zstack.header.tag.SystemTagVO_;
import org.zstack.header.tag.TagAO;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmNicVO;
import org.zstack.network.hostNetworkInterface.HostNetworkBondingVO;
import org.zstack.network.hostNetworkInterface.HostNetworkBondingVO_;
import org.zstack.network.hostNetworkInterface.HostNetworkInterfaceVO;
import org.zstack.network.hostNetworkInterface.HostNetworkInterfaceVO_;
import org.zstack.pciDevice.*;
import org.zstack.pciDevice.virtual.PciDeviceVirtStatus;
import org.zstack.utils.TagUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Tuple;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class VmVfNicUtils {
    private static final CLogger logger = Utils.getLogger(VmVfNicUtils.class);

    /**
     * Get all reserved vf nic pci devices that are recorded by system tag PCI_DEVICE_AS_VF_NIC.
     * @return all reserved vf nic pci devices
     */
    public static List<String> getReservedPciDevicesForVfNic() {
        List<SystemTagVO> tags = Q.New(SystemTagVO.class)
                .eq(SystemTagVO_.resourceType, PciDeviceVO.class.getSimpleName())
                .like(SystemTagVO_.tag, TagUtils.tagPatternToSqlPattern(PciDeviceSystemTags.PCI_DEVICE_AS_VF_NIC.getTagFormat()))
                .list();

        return tags.stream().map(TagAO::getResourceUuid).collect(Collectors.toList());
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
     * Randomly get the name a splited pf in host, which is also a bond slave of bondingName.
     * @param hostUuid the host uuid
     * @param bondingName the bonding name
     * @return splited pf name or null if cannot find one
     */
    @Transactional(readOnly = true)
    public static String getRandomPfNameInHostBonding(String hostUuid, String bondingName) {
        String bondUuid = Q.New(HostNetworkBondingVO.class)
                .eq(HostNetworkBondingVO_.hostUuid, hostUuid)
                .eq(HostNetworkBondingVO_.bondingName, bondingName)
                .select(HostNetworkBondingVO_.uuid)
                .findValue();
        if (StringUtils.isBlank(bondUuid)) {
            return null;
        }

        List<String> pfAddrs = Q.New(PciDeviceVO.class)
                .eq(PciDeviceVO_.hostUuid, hostUuid)
                .eq(PciDeviceVO_.type, PciDeviceType.Ethernet_Controller)
                .eq(PciDeviceVO_.virtStatus, PciDeviceVirtStatus.SRIOV_VIRTUALIZED)
                .select(PciDeviceVO_.pciDeviceAddress)
                .listValues();
        if (pfAddrs.isEmpty()) {
            return null;
        }

        return Q.New(HostNetworkInterfaceVO.class)
                .eq(HostNetworkInterfaceVO_.hostUuid, hostUuid)
                .eq(HostNetworkInterfaceVO_.bondingUuid, bondUuid)
                .in(HostNetworkInterfaceVO_.pciDeviceAddress, pfAddrs)
                .select(HostNetworkInterfaceVO_.interfaceName)
                .limit(1)
                .findValue();
    }

    /**
     * Select a vf nic from the host.
     * @param hostUuid uuid of the host
     * @param l3Uuid uuid of l3 network
     * @param accessiblePciUuids the pcis that account can access, null means the account is admin
     * @return vf nic uuid
     */
    @Transactional
    public static String selectRandomVfNicPciDevice(String hostUuid, String l3Uuid, List<String> accessiblePciUuids) {
        // if l2NicName is a bond with single slave, then try to get vf from the bond slave;
        // otherwise get vf from single pf.
        String l2NicName = getL2NicNameFromL3Uuid(l3Uuid);
        List<String> pfAddrs = new ArrayList<>();
        HostNetworkBondingVO bonding = Q.New(HostNetworkBondingVO.class)
                .eq(HostNetworkBondingVO_.hostUuid, hostUuid)
                .eq(HostNetworkBondingVO_.bondingName, l2NicName)
                .find();
        if (bonding != null) {
            pfAddrs.addAll(bonding.getSlaves().stream().map(HostNetworkInterfaceVO::getPciDeviceAddress).collect(Collectors.toList()));
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
            logger.debug(String.format("no interface named[%s] in host[uuid:%s]", l2NicName, hostUuid));
            return null;
        }

        if (pfAddrs.size() > 1) {
            logger.debug(String.format("[%s] is a bond with multi slaves in host[uuid:%s]", l2NicName, hostUuid));
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
            logger.debug(String.format("no usable sr-iov pf of nic name[%s] in host[uuid:%s]", l2NicName, hostUuid));
            return null;
        }

        List<String> reservedVfNicPciDevices = getReservedPciDevicesForVfNic();
        Q query = Q.New(PciDeviceVO.class)
                .eq(PciDeviceVO_.hostUuid, hostUuid)
                .eq(PciDeviceVO_.type, PciDeviceType.Ethernet_Controller)
                .eq(PciDeviceVO_.virtStatus, PciDeviceVirtStatus.SRIOV_VIRTUAL)
                .in(PciDeviceVO_.parentUuid, pfUuids)
                .eq(PciDeviceVO_.state, PciDeviceState.Enabled)
                .notEq(PciDeviceVO_.status, PciDeviceStatus.Attached);

        if (!reservedVfNicPciDevices.isEmpty()) {
            query = query.notIn(PciDeviceVO_.uuid, reservedVfNicPciDevices);
        }

        if (accessiblePciUuids != null) {
            query = query.in(PciDeviceVO_.uuid, accessiblePciUuids);
        }

        return query.select(PciDeviceVO_.uuid).limit(1).findValue();
    }

    /**
     * Allocate vf nic pci device to the vm.
     * @param vfNicUuid uuid of vf nic
     * @param pciUuid uuid of pci device
     */
    @Transactional
    public static void allocatePciDeviceToVfNic(String vfNicUuid, String pciUuid) {
        VmVfNicVO vf = Q.New(VmVfNicVO.class).eq(VmVfNicVO_.uuid, vfNicUuid).find();
        if (vf == null) {
            return;
        }

        SQL.New(PciDeviceVO.class)
                .eq(PciDeviceVO_.uuid, pciUuid)
                .set(PciDeviceVO_.vmInstanceUuid, vf.getVmInstanceUuid())
                .set(PciDeviceVO_.status, PciDeviceStatus.Attached)
                .update();

        SQL.New(VmVfNicVO.class)
                .eq(VmVfNicVO_.uuid, vfNicUuid)
                .set(VmVfNicVO_.pciDeviceUuid, pciUuid)
                .update();

        logger.debug(String.format("allocated pci device[uuid:%s] to vf nic[uuid:%s] of vm[uuid:%s]", pciUuid, vf.getUuid(), vf.getVmInstanceUuid()));
    }

    /**
     * Release pci device from the vf nic.
     * @param vfNicUuid uuid of vf nic
     */
    @Transactional
    public static void releaseVfNicPciDevice(String vfNicUuid) {
        VmVfNicVO vf = Q.New(VmVfNicVO.class).eq(VmVfNicVO_.uuid, vfNicUuid).find();
        if (vf == null || StringUtils.isEmpty(vf.getPciDeviceUuid())) {
            return;
        }

        SQL.New(PciDeviceVO.class)
                .eq(PciDeviceVO_.uuid, vf.getPciDeviceUuid())
                .set(PciDeviceVO_.vmInstanceUuid, null)
                .set(PciDeviceVO_.status, PciDeviceStatus.System)
                .update();

        SQL.New(VmVfNicVO.class)
                .eq(VmVfNicVO_.uuid, vfNicUuid)
                .set(VmVfNicVO_.pciDeviceUuid, null)
                .update();

        logger.debug(String.format("relesed pci device[uuid:%s] from vf nic[uuid:%s] of vm[uuid:%s]", vf.getPciDeviceUuid(), vf.getUuid(), vf.getVmInstanceUuid()));
    }

    /**
     * Release vf nic pci devices from the vm
     * @param vmUuid uuid of vm instance
     */
    public static void releaseVfNicPciDevicesOnVm(String vmUuid) {
        List<String> vfNicUuids = Q.New(VmVfNicVO.class).eq(VmVfNicVO_.vmInstanceUuid, vmUuid).select(VmVfNicVO_.uuid).listValues();
        if (vfNicUuids.isEmpty()) {
            return;
        }

        vfNicUuids.forEach(VmVfNicUtils::releaseVfNicPciDevice);
        logger.debug(String.format("detached vf nic pci devices from vm[uuid:%s], " +
                "usually because it's stopped and libvirt reattach the pci device to host", vmUuid));
    }

    public static void cleanUpPciAsVfSystemTags(String vmUuid) {
        SQL.New(SystemTagVO.class)
                .eq(SystemTagVO_.resourceType, PciDeviceVO.class.getSimpleName())
                .like(SystemTagVO_.tag, String.format("pciDeviceAsVfNic::%s::%%", vmUuid))
                .delete();
        logger.debug(String.format("cleaned up pci as vf system tags for vm[uuid:%s]", vmUuid));
    }

    public static boolean isSriovEnabledOnL2Network(String l2Uuid) {
        return SriovSystemTags.L2_ENABLE_SRIOV.hasTag(l2Uuid);
    }

    public static boolean isVirtioTypeNic(String vmUuid, String l3Uuid) {
        return Q.New(VmNicVO.class).eq(VmVfNicVO_.vmInstanceUuid, vmUuid).eq(VmVfNicVO_.l3NetworkUuid, l3Uuid)
                .eq(VmVfNicVO_.type, VmInstanceConstant.VIRTUAL_NIC_TYPE).isExists();
    }

    public static Map<String, List<String>> getL2NetworkUuidsFromPciDevice(PciDeviceInventory pci) {
        Tuple tup = Q.New(HostNetworkInterfaceVO.class).eq(HostNetworkInterfaceVO_.hostUuid, pci.getHostUuid())
                .eq(HostNetworkInterfaceVO_.pciDeviceAddress, pci.getPciDeviceAddress())
                .select(HostNetworkInterfaceVO_.interfaceName, HostNetworkInterfaceVO_.bondingUuid).findTuple();
        if (tup == null) {
            return new HashMap<>();
        }

        /* not bonding slave  */
        if (tup.get(1) == null) {
            Map<String, List<String>> res = new HashMap<>();
            List<String> ret = Q.New(L2NetworkVO.class).select(L2NetworkVO_.uuid)
                    .eq(L2NetworkVO_.physicalInterface, tup.get(0)).listValues();
            if (!ret.isEmpty()) {
                res.put((String) tup.get(0), ret);
            }
            return res;
        }

        String bondName = Q.New(HostNetworkBondingVO.class).eq(HostNetworkBondingVO_.uuid, tup.get(1))
                .select(HostNetworkBondingVO_.bondingName).findValue();
        if (bondName == null) {
            return new HashMap<>();
        }

        List<String> ret = Q.New(L2NetworkVO.class).select(L2NetworkVO_.uuid)
                .eq(L2NetworkVO_.physicalInterface, bondName).listValues();
        Map<String, List<String>> res = new HashMap<>();
        if (!ret.isEmpty()) {
            res.put(bondName, ret);
        }

        return res;
    }

    public static String getSriovPhysicalInterfaceName(String hostUuid, String l2Uuid) {
        String phyNicName = Q.New(L2NetworkVO.class).select(L2NetworkVO_.physicalInterface)
                .eq(L2NetworkVO_.uuid, l2Uuid).findValue();
        if (phyNicName == null || phyNicName.isEmpty()) {
            return "";
        }

        String bondingUuid = Q.New(HostNetworkBondingVO.class).select(HostNetworkBondingVO_.uuid)
                .eq(HostNetworkBondingVO_.bondingName, phyNicName)
                .eq(HostNetworkBondingVO_.hostUuid, hostUuid).findValue();
        /* bonding interface */
        if (bondingUuid != null) {
            List<Tuple> tuples = Q.New(HostNetworkInterfaceVO.class)
                    .select(HostNetworkInterfaceVO_.pciDeviceAddress, HostNetworkInterfaceVO_.interfaceName)
                    .eq(HostNetworkInterfaceVO_.hostUuid, hostUuid)
                    .eq(HostNetworkInterfaceVO_.bondingUuid, bondingUuid)
                    .orderBy(HostNetworkInterfaceVO_.interfaceName, SimpleQuery.Od.DESC).listTuple();
            if (tuples.isEmpty()) {
                return "";
            }

            for (Tuple t : tuples){
                if (Q.New(PciDeviceVO.class).eq(PciDeviceVO_.pciDeviceAddress, t.get(0))
                        .eq(PciDeviceVO_.hostUuid, hostUuid)
                        .eq(PciDeviceVO_.virtStatus, PciDeviceVirtStatus.SRIOV_VIRTUALIZED).isExists()) {
                    return (String) t.get(1);
                }
            }

            return "";
        }

        /* not bonding interface */
        String pciAddress = Q.New(HostNetworkInterfaceVO.class).eq(HostNetworkInterfaceVO_.hostUuid, hostUuid)
                .eq(HostNetworkInterfaceVO_.interfaceName, phyNicName)
                .select(HostNetworkInterfaceVO_.pciDeviceAddress).findValue();
        if (pciAddress == null) {
            return "";
        }

        if (Q.New(PciDeviceVO.class).eq(PciDeviceVO_.pciDeviceAddress, pciAddress)
                .eq(PciDeviceVO_.hostUuid, hostUuid)
                .eq(PciDeviceVO_.virtStatus, PciDeviceVirtStatus.SRIOV_VIRTUALIZED).isExists()) {
            return phyNicName;
        }

        return "";
    }
}
