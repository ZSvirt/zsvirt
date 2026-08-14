package org.zstack.pciDevice;

import org.apache.commons.collections.CollectionUtils;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.db.SQLBatch;
import org.zstack.pciDevice.specification.pci.VmInstancePciDeviceSpecRefVO;
import org.zstack.pciDevice.specification.pci.VmInstancePciDeviceSpecRefVO_;
import org.zstack.pciDevice.virtual.PciDeviceVirtStatus;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Tuple;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by GuoYi on 2019-03-09.
 */
public class PciDeviceUtils {
    static final CLogger logger = Utils.getLogger(PciDeviceUtils.class);

    public static Map<String, Integer> getVmPciSpecUuids(String vmUuid) {
        Map<String, Integer> ret = new HashMap<>();

        // transform PCI_DEVICE_SPEC tags into RefVO records
        List<Map<String, String>> tokenList = PciDeviceSystemTags.PCI_DEVICE_SPEC.getTokensOfTagsByResourceUuid(vmUuid);
        for (Map<String, String> tokens : tokenList) {
            String specUuid = tokens.get(PciDeviceSystemTags.PCI_DEVICE_SPEC_UUID_TOKEN);
            String devNumber = tokens.get(PciDeviceSystemTags.PCI_DEVICE_NUMBER_TOKEN);
            ret.put(specUuid, Integer.valueOf(devNumber));
        }

        // add records in VmInstancePciDeviceSpecRefVO
        new SQLBatch() {
            @Override
            protected void scripts() {
                for (Map.Entry<String, Integer> entry : ret.entrySet()) {
                    String specUuid = entry.getKey();
                    int deviceNum = entry.getValue();
                    boolean exists = q(VmInstancePciDeviceSpecRefVO.class)
                            .eq(VmInstancePciDeviceSpecRefVO_.vmInstanceUuid, vmUuid)
                            .eq(VmInstancePciDeviceSpecRefVO_.pciSpecUuid, specUuid)
                            .eq(VmInstancePciDeviceSpecRefVO_.pciDeviceNumber, deviceNum)
                            .isExists();
                    if (exists) continue;

                    VmInstancePciDeviceSpecRefVO ref = new VmInstancePciDeviceSpecRefVO();
                    ref.setVmInstanceUuid(vmUuid);
                    ref.setPciSpecUuid(entry.getKey());
                    ref.setPciDeviceNumber(entry.getValue());
                    persist(ref);
                }
                flush();
            }
        }.execute();

        // delete PCI_DEVICE_SPEC tags
        if (PciDeviceSystemTags.PCI_DEVICE_SPEC.hasTag(vmUuid)) {
            PciDeviceSystemTags.PCI_DEVICE_SPEC.delete(vmUuid);
        }

        List<Tuple> tuples = Q.New(VmInstancePciDeviceSpecRefVO.class)
                .eq(VmInstancePciDeviceSpecRefVO_.vmInstanceUuid, vmUuid)
                .groupBy(VmInstancePciDeviceSpecRefVO_.pciSpecUuid)
                .select(VmInstancePciDeviceSpecRefVO_.pciSpecUuid, VmInstancePciDeviceSpecRefVO_.pciDeviceNumber)
                .listTuple();
        if (!tuples.isEmpty()) {
            for (Tuple tuple : tuples) {
                String specUuid = (String) tuple.get(0);
                int deviceNum = (int) tuple.get(1);

                if (!ret.containsKey(specUuid)) {
                    ret.put(specUuid, deviceNum);
                }
            }
        }

        return ret;
    }

    public static PciDeviceStatus getAvailableStatus(String pciUuid) {
        if (Q.New(PciDevicePciDeviceOfferingRefVO.class)
                .eq(PciDevicePciDeviceOfferingRefVO_.pciDeviceUuid, pciUuid)
                .isExists()) {
            return PciDeviceStatus.Active;
        } else {
            return PciDeviceStatus.System;
        }
    }

    public static void cleanUpReserveTags(String vmUuid, List<String> pciUuids) {
        if (CollectionUtils.isEmpty(pciUuids)) {
            return;
        }

        if (PciDeviceSystemTags.PCI_DEVICE_SPEC.hasTag(vmUuid)) {
            PciDeviceSystemTags.PCI_DEVICE_SPEC.delete(vmUuid);
        }
    }

    public static boolean releaseSpecReleatedPhysicalPciDevicesWhenStop(String vmUuid) {
        return PciDeviceSystemTags.AUTO_RELEASE_SPEC_RELEATED_PHYSICAL_PCI_DEVICE.hasTag(vmUuid);
    }

    public static boolean releaseSpecReleatedVirtualPciDevicesWhenStop(String vmUuid) {
        return PciDeviceSystemTags.AUTO_RELEASE_SPEC_RELEATED_VIRTUAL_PCI_DEVICE.hasTag(vmUuid);
    }

    // get uuids of all non-bridge pci devices from same iommu group as pciDeviceUuid
    // return empty list if the iommu group that pciDeviceUuid in is not properly organized
    public static List<String> getPciDeviceUuidsFromSameIommuGroup(String pciDeviceUuid) {
        return getPciDeviceUuidsFromSameIommuGroup(pciDeviceUuid, false);
    }

    public static List<String> getPciDeviceUuidsFromSameIommuGroup(String pciDeviceUuid, boolean reversed) {
        PciDeviceVO pci = Q.New(PciDeviceVO.class)
                .eq(PciDeviceVO_.uuid, pciDeviceUuid)
                .find();
        if (pci == null) {
            return new ArrayList<>();
        }

        List<PciDeviceVO> pcis = Q.New(PciDeviceVO.class)
                .eq(PciDeviceVO_.iommuGroup, pci.getIommuGroup())
                .notEq(PciDeviceVO_.type, PciDeviceType.PCI_Bridge)
                .notEq(PciDeviceVO_.type, PciDeviceType.Host_Bridge)
                .list();

        long count = pcis.stream()
                .map(p -> new PciDeviceAddress(p.getPciDeviceAddress()))
                .map(PciDeviceAddress::getBus)
                .distinct()
                .count();
        if (1 != count) {
            logger.error(iommuGroupError(PciDeviceInventory.valueOf(pci)));
            return new ArrayList<>();
        }

        if (!reversed) {
            return pcis.stream().sorted(Comparator.comparing(PciDeviceVO::getPciDeviceAddress))
                    .map(PciDeviceVO::getUuid).collect(Collectors.toList());
        } else {
            return pcis.stream().sorted(Comparator.comparing(PciDeviceVO::getPciDeviceAddress).reversed())
                    .map(PciDeviceVO::getUuid).collect(Collectors.toList());
        }
    }

    public static String iommuGroupError(PciDeviceInventory pci) {
        return String.format("there are other devices in iommu group[%s] of host[uuid:%s], " +
                "you may need to move pci device[uuid:%s] to another pci slot in the motherboard, " +
                "or enable ACS override patch manually.", pci.getIommuGroup(), pci.getHostUuid(), pci.getUuid());
    }

    public static String getPciDeviceUuidByHostNetworkInterface(String interfaceName, String hostUuid) {
        return SQL.New("select pci.uuid from PciDeviceVO pci, HostNetworkInterfaceVO if" +
                " where pci.hostUuid = if.hostUuid" +
                " and pci.pciDeviceAddress = if.pciDeviceAddress" +
                " and if.interfaceName = :interfaceName" +
                " and pci.hostUuid = :hostUuid", String.class)
                .param("interfaceName", interfaceName)
                .param("hostUuid", hostUuid)
                .find();
    }

    public static List<String> getPciDeviceUuidsByHostNetworkInterface(List<String> interfaceUuids, String hostUuid) {
        if (CollectionUtils.isEmpty(interfaceUuids)) {
            return new ArrayList<>();
        }

        return SQL.New("select pci.uuid from PciDeviceVO pci, HostNetworkInterfaceVO if" +
                        " where pci.hostUuid = if.hostUuid" +
                        " and pci.pciDeviceAddress = if.pciDeviceAddress" +
                        " and if.uuid in (:interfaceUuids)" +
                        " and pci.hostUuid = :hostUuid", String.class)
                .param("interfaceUuids", interfaceUuids)
                .param("hostUuid", hostUuid)
                .list();
    }

    public static void updatePciDevicePassThroughState(String pciDeviceUuid,
                                                       PciDevicePassThroughState oldState, PciDevicePassThroughState newState) {
        if (pciDeviceUuid == null) {
            logger.warn("pciDeviceUuid is unexpectedly null");
            return;
        }

        // exclude SRIOV_VIRTUALIZED PF
        SQL.New(PciDeviceVO.class)
                .eq(PciDeviceVO_.uuid, pciDeviceUuid)
                .eq(PciDeviceVO_.passThroughState, oldState)
                .notEq(PciDeviceVO_.virtStatus, PciDeviceVirtStatus.SRIOV_VIRTUALIZED)
                .set(PciDeviceVO_.passThroughState, newState)
                .update();

        logger.debug(String.format("update pci device[uuid: %s] with passThroughState[%s] to [%s], excluding SR-IOV virtualized PF",
                pciDeviceUuid, oldState, newState));

    }

    public static void updatePciDevicePassThroughState(List<String> pciDeviceUuids,
                                                       PciDevicePassThroughState oldState, PciDevicePassThroughState newState) {
        if (CollectionUtils.isEmpty(pciDeviceUuids)) {
            logger.warn("pciDeviceUuid is unexpectedly empty");
            return;
        }

        // exclude SRIOV_VIRTUALIZED PF
        SQL.New(PciDeviceVO.class)
                .in(PciDeviceVO_.uuid, pciDeviceUuids)
                .eq(PciDeviceVO_.passThroughState, oldState)
                .notEq(PciDeviceVO_.virtStatus, PciDeviceVirtStatus.SRIOV_VIRTUALIZED)
                .set(PciDeviceVO_.passThroughState, newState)
                .update();

        logger.debug(String.format("update pci devices[uuids: %s] with passThroughState[%s] to [%s], excluding SR-IOV virtualized PF",
                pciDeviceUuids, oldState, newState));

    }

    public static void updatePciDevicePassThroughStateFromAvailableToDisabled(String interfaceName, String hostUuid) {
        updatePciDevicePassThroughState(getPciDeviceUuidByHostNetworkInterface(interfaceName, hostUuid),
                PciDevicePassThroughState.Available, PciDevicePassThroughState.Disabled);
    }

    public static void updatePciDevicePassThroughStateFromDisabledToAvailable(String interfaceName, String hostUuid) {
        updatePciDevicePassThroughState(getPciDeviceUuidByHostNetworkInterface(interfaceName, hostUuid),
                PciDevicePassThroughState.Disabled, PciDevicePassThroughState.Available);
    }

    public static void updatePciDevicePassThroughStateFromDisabledToAvailable(List<String> interfaceUuids, String hostUuid) {
        updatePciDevicePassThroughState(getPciDeviceUuidsByHostNetworkInterface(interfaceUuids, hostUuid),
                PciDevicePassThroughState.Disabled, PciDevicePassThroughState.Available);
    }

    public static boolean checkIfPciDevicePassThroughStateIsEnabled(String interfaceName, String hostUuid) {
        return Q.New(PciDeviceVO.class)
                .eq(PciDeviceVO_.uuid, getPciDeviceUuidByHostNetworkInterface(interfaceName, hostUuid))
                .eq(PciDeviceVO_.passThroughState, PciDevicePassThroughState.Enabled)
                .isExists();
    }
}
