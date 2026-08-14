package org.zstack.network.l2.virtualSwitch;

import org.apache.commons.collections.CollectionUtils;
import org.zstack.compute.bonding.HostNetworkBondingConstant;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.header.host.NetworkInterfaceType;
import org.zstack.header.network.l2.L2NetworkInventory;
import org.zstack.header.network.l2.L2NetworkVO;
import org.zstack.kvm.KVMHostUtils;
import org.zstack.kvm.KVMSystemTags;
import org.zstack.network.hostNetworkInterface.HostNetworkBondingVO;
import org.zstack.network.hostNetworkInterface.HostNetworkBondingVO_;
import org.zstack.network.hostNetworkInterface.HostNetworkInterfaceVO;
import org.zstack.network.hostNetworkInterface.HostNetworkInterfaceVO_;
import org.zstack.network.l2.virtualSwitch.header.*;
import org.zstack.tag.SystemTagCreator;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

public class VirtualSwitchUtils {
    private static final CLogger logger = Utils.getLogger(VirtualSwitchUtils.class);

    public static boolean isUpLinkBondingExist(String vSwitchUuid, String hostUuid, String bondingName) {
        String mode = VirtualSwitchSystemTags.UPLINK_BONDING.getTokenByResourceUuid(vSwitchUuid, VirtualSwitchSystemTags.BONDING_MODE_TOKEN);
        if (mode == null) {
            return false;
        }

        Q query = Q.New(HostNetworkBondingVO.class)
                .eq(HostNetworkBondingVO_.bondingName, bondingName)
                .eq(HostNetworkBondingVO_.hostUuid, hostUuid)
                .eq(HostNetworkBondingVO_.mode, mode);

        if (HostNetworkBondingConstant.BONDING_MODE_AB.equals(mode)) {
            return query.isExists();
        }

        String xmitHashPolicy = VirtualSwitchSystemTags.UPLINK_BONDING.getTokenByResourceUuid(vSwitchUuid, VirtualSwitchSystemTags.XMIT_HASH_POLICY_TOKEN);
        return query.eq(HostNetworkBondingVO_.xmitHashPolicy, xmitHashPolicy).isExists();
    }

    public static boolean isPhysicalInterfaceValid(String interfaceName, String hostUuid) {
        return Q.New(HostNetworkInterfaceVO.class)
                .eq(HostNetworkInterfaceVO_.hostUuid, hostUuid)
                .eq(HostNetworkInterfaceVO_.interfaceName, interfaceName)
                .eq(HostNetworkInterfaceVO_.interfaceType, NetworkInterfaceType.noMaster.toString())
                .isNull(HostNetworkInterfaceVO_.bondingUuid)
                .isExists();
    }

    public static void createUplinkBondingSystemTag(String vSwitchUuid, String mode, String xmitHashPolicy) {
        if (HostNetworkBondingConstant.BONDING_MODE_AB.equals(mode)) {
            xmitHashPolicy = HostNetworkBondingConstant.BONDING_XMIT_HASH_POLICY_NULL;
        }

        SystemTagCreator creator = VirtualSwitchSystemTags.UPLINK_BONDING.newSystemTagCreator(vSwitchUuid);
        creator.inherent = false;
        creator.recreate = true;
        creator.setTagByTokens(map(e(VirtualSwitchSystemTags.BONDING_MODE_TOKEN, mode),
                e(VirtualSwitchSystemTags.XMIT_HASH_POLICY_TOKEN, xmitHashPolicy)));
        creator.create();
    }

    public static void deleteUplinkBondingSystemTag(String vSwitchUuid) {
        VirtualSwitchSystemTags.UPLINK_BONDING.delete(vSwitchUuid);
    }

    public static void deleteUplinkGroup(String vSwitchUuid, List<String> hostUuids) {
        if (hostUuids.isEmpty()) {
            return;
        }

        logger.debug(String.format("del UplinkGroupVO, vSwitchUuid: %s, hostUuids: %s",
                vSwitchUuid, hostUuids));
        SQL.New(UplinkGroupVO.class)
                .eq(UplinkGroupVO_.l2NetworkUuid, vSwitchUuid)
                .in(UplinkGroupVO_.hostUuid, hostUuids)
                .delete();
    }

    public static void deleteUplinkGroup(String vSwitchUuid, String hostUuid) {
        deleteUplinkGroup(vSwitchUuid, Collections.singletonList(hostUuid));
    }

    public static String getInterfaceNameOfL2PortGroupOnHost(L2NetworkInventory l2Network, String hostUuid) {
        // use L2PortGroupNetworkVO instead of PortGroupVO,
        // because when create port group and realize l2, PortGroupVO is not created yet
        String interfaceName = SQL.New("select ug.interfaceName from UplinkGroupVO ug, L2PortGroupNetworkVO pg" +
                        " where pg.uuid = :l2Uuid" +
                        " and pg.vSwitchUuid = ug.l2NetworkUuid" +
                        " and ug.hostUuid = :hostUuid", String.class)
                .param("l2Uuid", l2Network.getUuid())
                .param("hostUuid", hostUuid)
                .find();

        if (interfaceName != null) {
            return interfaceName;
        }

        return l2Network.getPhysicalInterface();
    }

    public static List<String> getPhysicalInterfaceNamesOfL2PortGroupOnHost(String pgUuid, String hostUuid) {
        L2PortGroupNetworkVO pg = Q.New(L2PortGroupNetworkVO.class).eq(L2PortGroupNetworkVO_.uuid, pgUuid).find();
        UplinkGroupVO ug = getUplinkGroup(pg.getvSwitchUuid(), hostUuid);

        if (UplinkGroupType.PhysicalInterface.equals(ug.getType())) {
            return Collections.singletonList(ug.getInterfaceName());
        } else if (UplinkGroupType.Bonding.equals(ug.getType())) {
            return Q.New(HostNetworkInterfaceVO.class)
                    .select(HostNetworkInterfaceVO_.interfaceName)
                    .eq(HostNetworkInterfaceVO_.hostUuid, hostUuid)
                    .eq(HostNetworkInterfaceVO_.bondingUuid, ug.getBondingUuid())
                    .listValues();
        }

        return new ArrayList<>();
    }

    public static String getInterfaceNameOfvSwitchOnHost(L2NetworkInventory vs, String hostUuid) {
        String interfaceName = Q.New(UplinkGroupVO.class).select(UplinkGroupVO_.interfaceName)
                .eq(UplinkGroupVO_.l2NetworkUuid, vs.getUuid())
                .eq(UplinkGroupVO_.hostUuid, hostUuid)
                .findValue();

        if (interfaceName != null) {
            return interfaceName;
        }

        return vs.getPhysicalInterface();
    }

    public static String makeBridgeName(String vSwitchUuid, String pgUuid, int vlan) {
        return makeBridgeName(vSwitchUuid, pgUuid, vlan, false);
    }

    public static String makeBridgeName(String vSwitchUuid, String pgUuid, int vlan, boolean ignoreTag) {
        if (!ignoreTag && KVMSystemTags.L2_BRIDGE_NAME.hasTag(pgUuid, L2NetworkVO.class)) {
            return KVMSystemTags.L2_BRIDGE_NAME.getTokenByResourceUuid(pgUuid, KVMSystemTags.L2_BRIDGE_NAME_TOKEN);
        }

        Integer index = Q.New(L2VirtualSwitchNetworkVO.class)
                .select(L2VirtualSwitchNetworkVO_.vSwitchIndex)
                .eq(L2VirtualSwitchNetworkVO_.uuid, vSwitchUuid)
                .findValue();

        String bridgeName = makeBridgeName(index, vlan);
        if (bridgeName != null) {
            // bridgeName is needed before attaching to cluster or host,
            // so KVMHostUtils.checkNameConflict will not be called here
            if (checkIfBridgeNameConflicts(bridgeName, vSwitchUuid, pgUuid)) {
                return KVMHostUtils.generateBridgeNameWithL2Uuid(pgUuid);
            }

            return bridgeName;
        }

        if (vlan != 0) {
            return KVMHostUtils.getNormalizedBridgeName(pgUuid, "br_%s_" + vlan);
        } else {
            return KVMHostUtils.getNormalizedBridgeName(pgUuid, "br_%s");
        }
    }

    public static String makeBridgeName(Integer index, int vlan) {
        if (index == null) {
            return null;
        }

        String bridgeName = String.format("br_%s", VirtualSwitchConstant.DEFAULT_BRIDGE_NAME_PREFIX + index);
        if (vlan != 0) {
            return String.format("%s_%s", bridgeName, vlan);
        }

        return bridgeName;
    }

    private static boolean checkIfBridgeNameConflicts(String bridgeName, String vSwitchUuid, String pgUuid) {
        return SQL.New("select ref.bridgeName from L2NetworkHostRefVO ref, L2PortGroupNetworkVO pg" +
                        " where ref.l2NetworkUuid = pg.uuid" +
                        " and ref.bridgeName = :bridgeName" +
                        " and pg.vSwitchUuid = :vSwitchUuid" +
                        " and pg.uuid != :pgUuid", String.class)
                .param("vSwitchUuid", vSwitchUuid)
                .param("bridgeName", bridgeName)
                .param("pgUuid", pgUuid)
                .limit(1)
                .find() != null;
    }

    public static List<HostNetworkBondingVO> getUplinkBondingOfvSwitch(String vSwitchUuid) {
        return SQL.New("select bonding from HostNetworkBondingVO bonding, UplinkGroupVO ug" +
                        " where ug.l2NetworkUuid = :vSwitchUuid" +
                        " and ug.type = :type" +
                        " and ug.hostUuid = bonding.hostUuid" +
                        " and ug.interfaceName = bonding.bondingName", HostNetworkBondingVO.class)
                .param("vSwitchUuid", vSwitchUuid)
                .param("type", UplinkGroupType.Bonding)
                .list();
    }

    public static List<HostNetworkBondingVO> getUplinkBondingOfvSwitch(String vSwitchUuid, List<String> hostUuids) {
        if (CollectionUtils.isEmpty(hostUuids)) {
            return new ArrayList<>();
        }

        return SQL.New("select bonding from HostNetworkBondingVO bonding, UplinkGroupVO ug" +
                        " where ug.l2NetworkUuid = :vSwitchUuid" +
                        " and ug.type = :type" +
                        " and ug.hostUuid = bonding.hostUuid" +
                        " and ug.interfaceName = bonding.bondingName" +
                        " and ug.hostUuid in (:hostUuids)", HostNetworkBondingVO.class)
                .param("vSwitchUuid", vSwitchUuid)
                .param("type", UplinkGroupType.Bonding)
                .param("hostUuids", hostUuids)
                .list();
    }

    public static Q getUplinkGroupQuery(String vSwitchUuid, String hostUuid, UplinkGroupType type) {
        Q query = Q.New(UplinkGroupVO.class)
                .eq(UplinkGroupVO_.l2NetworkUuid, vSwitchUuid);

        if (hostUuid != null) {
            query = query.eq(UplinkGroupVO_.hostUuid, hostUuid);
        }

        if (type != null) {
            return query.eq(UplinkGroupVO_.type, type);
        }

        return query;
    }

    public static boolean isUplinkBondingExist(String vSwitchUuid) {
        return getUplinkGroupQuery(vSwitchUuid, null, UplinkGroupType.Bonding).isExists();
    }

    public static boolean isUplinkGroupExist(String vSwitchUuid, String hostUuid) {
        return getUplinkGroupQuery(vSwitchUuid, hostUuid, null).isExists();
    }

    public static String getVSwitchUuidOfUplinkGroup(String interfaceName, String hostUuid) {
        return Q.New(UplinkGroupVO.class)
                .select(UplinkGroupVO_.l2NetworkUuid)
                .eq(UplinkGroupVO_.interfaceName, interfaceName)
                .eq(UplinkGroupVO_.hostUuid, hostUuid)
                .findValue();
    }

    public static UplinkGroupVO getUplinkGroup(String vSwitchUuid, String hostUuid, UplinkGroupType type) {
        return getUplinkGroupQuery(vSwitchUuid, hostUuid, type).find();
    }

    public static UplinkGroupVO getUplinkGroup(String vSwitchUuid, String hostUuid) {
        return getUplinkGroup(vSwitchUuid, hostUuid, null);
    }

    public static List<UplinkGroupVO> getUplinkGroups(String vSwitchUuid, List<String> hostUuids, UplinkGroupType type) {
        if (CollectionUtils.isEmpty(hostUuids)) {
            return new ArrayList<>();
        }

        return Q.New(UplinkGroupVO.class)
                .eq(UplinkGroupVO_.l2NetworkUuid, vSwitchUuid)
                .in(UplinkGroupVO_.hostUuid, hostUuids)
                .eq(UplinkGroupVO_.type, type)
                .list();
    }

    public static List<UplinkGroupVO> getUplinkGroups(List<String> vSwitchUuids, String hostUuid, UplinkGroupType type) {
        if (CollectionUtils.isEmpty(vSwitchUuids)) {
            return new ArrayList<>();
        }

        return Q.New(UplinkGroupVO.class)
                .in(UplinkGroupVO_.l2NetworkUuid, vSwitchUuids)
                .eq(UplinkGroupVO_.hostUuid, hostUuid)
                .eq(UplinkGroupVO_.type, type)
                .list();
    }

    public static Integer getVirtualSwitchIndexOfZone(String zoneUuid) {
        String indexStr = VirtualSwitchSystemTags.VIRTUAL_SWITCH_INDEX
                .getTokenByResourceUuid(zoneUuid, VirtualSwitchSystemTags.VIRTUAL_SWITCH_INDEX_TOKEN);
        if (indexStr == null) {
            SystemTagCreator creator = VirtualSwitchSystemTags.VIRTUAL_SWITCH_INDEX.newSystemTagCreator(zoneUuid);
            creator.setTagByTokens(map(e(VirtualSwitchSystemTags.VIRTUAL_SWITCH_INDEX_TOKEN, 0)));
            creator.inherent = true;
            creator.recreate = true;
            creator.create();
            return 0;
        }

        try {
            return Integer.valueOf(indexStr);
        } catch (NumberFormatException e) {
            logger.debug(String.format("invalid index[%s] for virtual switch in zone[uuid: %s]", indexStr, zoneUuid));
            return null;
        }
    }

    public static void increaseVirtualSwitchIndexOfZone(String zoneUuid) {
        Integer index = getVirtualSwitchIndexOfZone(zoneUuid);
        if (index == null) {
            index = 0;
        }

        VirtualSwitchSystemTags.VIRTUAL_SWITCH_INDEX.updateTagByToken(
                zoneUuid, VirtualSwitchSystemTags.VIRTUAL_SWITCH_INDEX_TOKEN, String.valueOf(index + 1));
    }

    public static void rollbackVirtualSwitchIndexOfZone(String zoneUuid) {
        Integer index = getVirtualSwitchIndexOfZone(zoneUuid);
        if (index == null || index == 0) {
            return;
        }

        VirtualSwitchSystemTags.VIRTUAL_SWITCH_INDEX.updateTagByToken(
                zoneUuid, VirtualSwitchSystemTags.VIRTUAL_SWITCH_INDEX_TOKEN, String.valueOf(index - 1));
    }

    public static void changeVirtualSwitchUplinkBondingName(String vSwitchUuid, String interfaceName) {
        SQL.New(L2VirtualSwitchNetworkVO.class)
                .eq(L2VirtualSwitchNetworkVO_.uuid, vSwitchUuid)
                .set(L2VirtualSwitchNetworkVO_.physicalInterface, interfaceName)
                .update();

        SQL.New(L2PortGroupNetworkVO.class)
                .eq(L2PortGroupNetworkVO_.vSwitchUuid, vSwitchUuid)
                .set(L2PortGroupNetworkVO_.physicalInterface, interfaceName)
                .update();
    }

    public static boolean hasNoVlanPortGroup(String vSwitchUuid) {
        return Q.New(PortGroupVO.class)
                .eq(PortGroupVO_.vSwitchUuid, vSwitchUuid)
                .eq(PortGroupVO_.vlanId, 0)
                .isExists();
    }

    public static HostKernelInterfaceVO getDefaultKernelInterface(String l2Uuid, String hostUuid) {
        return SQL.New("select KIF from HostKernelInterfaceVO KIF, SystemTagVO sysTag" +
                        " where KIF.l2NetworkUuid = :l2Uuid" +
                        " and sysTag.resourceUuid = KIF.uuid" +
                        " and sysTag.tag = :tag" +
                        " and KIF.hostUuid = :hostUuid", HostKernelInterfaceVO.class)
                .param("l2Uuid", l2Uuid)
                .param("tag", VirtualSwitchSystemTags.HOST_KERNEL_DEFAULT_INTERFACE.getTagFormat())
                .param("hostUuid", hostUuid)
                .find();
    }
}
