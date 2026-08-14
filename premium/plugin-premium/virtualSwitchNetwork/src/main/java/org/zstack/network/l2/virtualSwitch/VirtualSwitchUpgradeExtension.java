package org.zstack.network.l2.virtualSwitch;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.compute.bonding.HostNetworkBondingConstant;
import org.zstack.compute.sriov.SriovSystemTags;
import org.zstack.core.db.*;
import org.zstack.header.Component;
import org.zstack.header.network.l2.L2NetworkHostRefVO;
import org.zstack.header.network.l2.L2NetworkVO;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.network.l3.L3NetworkVO_;
import org.zstack.header.tag.SystemTagInventory;
import org.zstack.header.tag.SystemTagVO;
import org.zstack.header.tag.SystemTagVO_;
import org.zstack.kvm.KVMSystemTags;
import org.zstack.network.hostNetworkInterface.HostNetworkBondingVO;
import org.zstack.network.hostNetworkInterface.HostNetworkBondingVO_;
import org.zstack.network.l2.virtualSwitch.header.*;
import org.zstack.tag.TagManager;
import org.zstack.utils.TagUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Tuple;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;


public class VirtualSwitchUpgradeExtension implements Component {
    private static final CLogger logger = Utils.getLogger(VirtualSwitchUpgradeExtension.class);

    @Autowired
    DatabaseFacade dbf;
    @Autowired
    private TagManager tagMgr;

    private void upgradeUplinkBonding() {
        List<Tuple> tuples = Q.New(L2VirtualSwitchNetworkVO.class)
                .select(L2VirtualSwitchNetworkVO_.uuid, L2VirtualSwitchNetworkVO_.physicalInterface)
                .listTuple();
        if (tuples.isEmpty()) {
            logger.debug("no need to upgrade uplink bonding because there are no virtual switches");
            return;
        }

        List<SystemTagInventory> tagInventories = VirtualSwitchSystemTags.UPLINK_BONDING.getTagInventories(
                tuples.stream().map(tuple -> tuple.get(0, String.class)).collect(Collectors.toList()));
        List<String> l2WithTag = tagInventories.stream().map(SystemTagInventory::getResourceUuid).collect(Collectors.toList());
        List<Tuple> tuplesWithoutTag = tuples.stream().filter(tuple -> !l2WithTag.contains(tuple.get(0, String.class))).collect(Collectors.toList());

        String defaultTag = VirtualSwitchSystemTags.UPLINK_BONDING.instantiateTag(map(
                e(VirtualSwitchSystemTags.BONDING_MODE_TOKEN, HostNetworkBondingConstant.BONDING_MODE_LACP),
                e(VirtualSwitchSystemTags.XMIT_HASH_POLICY_TOKEN, HostNetworkBondingConstant.BONDING_XMIT_HASH_POLICY_LAYER_TWO_AND_THREE)));

        for (Tuple tuple : tuplesWithoutTag) {
            String uuid = tuple.get(0, String.class);
            String bondingName = tuple.get(1, String.class);

            List<Tuple> tupleList = SQL.New("select bond.mode, bond.xmitHashPolicy from L2NetworkClusterRefVO ref, HostVO host, HostNetworkBondingVO bond" +
                            " where ref.l2NetworkUuid = :l2Uuid" +
                            " and ref.clusterUuid = host.clusterUuid" +
                            " and host.uuid = bond.hostUuid" +
                            " and bond.bondingName = :bondingName", Tuple.class)
                    .param("l2Uuid", uuid)
                    .param("bondingName", bondingName)
                    .list();

            if (StringUtils.isEmpty(bondingName)) {
                logger.debug(String.format("no need to upgrade uplink bonding for virtual switch[uuid:%s]" +
                        " because it's physical interface is empty", uuid));
            } else if (tupleList.isEmpty()) {
                logger.debug(String.format("There are no bonds[%s] for virtual switch[uuid:%s], use default tag[%s] instead",
                        bondingName, uuid, defaultTag));
                tagMgr.createNonInherentSystemTag(uuid, defaultTag, L2NetworkVO.class.getSimpleName());
            } else {
                String mode = tupleList.get(0).get(0, String.class);
                String xmitHashPolicy = tupleList.get(0).get(1, String.class);
                VirtualSwitchUtils.createUplinkBondingSystemTag(uuid, mode, xmitHashPolicy);
            }
        }
    }

    private void upgradePortGroup() {
        List<L2PortGroupNetworkVO> l2PortGroups = Q.New(L2PortGroupNetworkVO.class).list();
        if (l2PortGroups.isEmpty()) {
            logger.debug("no need to upgrade port group because there are no port groups");
            return;
        }

        List<String> l2PortGroupUuids = l2PortGroups.stream().map(L2PortGroupNetworkVO::getUuid).collect(Collectors.toList());
        List<L3NetworkVO> l3s = Q.New(L3NetworkVO.class)
                .in(L3NetworkVO_.l2NetworkUuid, l2PortGroupUuids)
                .notEq(L3NetworkVO_.type, VirtualSwitchConstant.PORT_GROUP_NETWORK_TYPE)
                .list();

        l3s.forEach(l3 -> {
            L2PortGroupNetworkVO l2PortGroup = l2PortGroups.stream()
                    .filter(l2 -> l2.getUuid().equals(l3.getL2NetworkUuid()))
                    .findFirst().orElse(new L2PortGroupNetworkVO());
            new SQLBatch() {
                @Override
                protected void scripts() {
                    dbf.getEntityManager().createNativeQuery(
                                    String.format("insert into PortGroupVO (uuid, vSwitchUuid, vlanMode, vlanId, vlanRanges)" +
                                                    " values ('%s', '%s', '%s', '%s', '%s')",
                                            l3.getUuid(), l2PortGroup.getvSwitchUuid(), l2PortGroup.getVlanMode(),
                                            l2PortGroup.getVlanId(), l2PortGroup.getVlanRanges()))
                            .executeUpdate();
                    l3.setType(VirtualSwitchConstant.PORT_GROUP_NETWORK_TYPE);
                    dbf.updateAndRefresh(l3);
                }
            }.execute();
        });
    }

    private void upgradeDefaultSystemTags() {
        List<String> vSwitchUuids = SQL.New("select vs.uuid from L2VirtualSwitchNetworkVO vs, SystemTagVO sysTag" +
                        " where sysTag.resourceUuid = vs.uuid" +
                        " and sysTag.tag = :tag", String.class)
                .param("tag", VirtualSwitchSystemTags.L2_DEFAULT_NETWORK.getTagFormat())
                .list();

        if (vSwitchUuids.isEmpty()) {
            logger.debug("no need to upgrade default system tags because there are no default virtual switches that have not been upgraded yet");
            return;
        }

        for (String vSwitchUuid : vSwitchUuids) {
            VirtualSwitchSystemTags.L2_DEFAULT_NETWORK.delete(vSwitchUuid);
            tagMgr.createNonInherentSystemTag(vSwitchUuid, VirtualSwitchSystemTags.VIRTUAL_SWITCH_DEFAULT.getTagFormat(), L2VirtualSwitchNetworkVO.class.getSimpleName());

            List<PortGroupVO> defaultPortGroups = SQL.New("select pg" +
                            " from PortGroupVO pg, SystemTagVO sysTag, HostKernelInterfaceVO if" +
                            " where pg.vSwitchUuid = :vSwitchUuid" +
                            " and pg.l2NetworkUuid = if.l2NetworkUuid" +
                            " and pg.uuid = if.l3NetworkUuid" +
                            " and sysTag.resourceUuid = if.uuid" +
                            " and sysTag.tag = :tag", PortGroupVO.class)
                    .param("vSwitchUuid", vSwitchUuid)
                    .param("tag", VirtualSwitchSystemTags.HOST_KERNEL_DEFAULT_INTERFACE.getTagFormat())
                    .list();
            defaultPortGroups = defaultPortGroups.stream().distinct().collect(Collectors.toList());

            for (PortGroupVO pg : defaultPortGroups) {
                VirtualSwitchSystemTags.L2_DEFAULT_NETWORK.delete(pg.getL2NetworkUuid());
                tagMgr.createNonInherentSystemTag(pg.getUuid(), VirtualSwitchSystemTags.PORT_GROUP_DEFAULT.getTagFormat(), PortGroupVO.class.getSimpleName());
            }
        }
    }

    private void upgradeSriovSystemTags() {
        List<String> l2WithTags = SQL.New("select l2.uuid from L2PortGroupNetworkVO l2, SystemTagVO sysTag" +
                " where sysTag.resourceUuid = l2.uuid" +
                " and sysTag.tag = :tag", String.class)
                .param("tag", SriovSystemTags.L2_ENABLE_SRIOV.getTagFormat())
                .list();

        List<String> l2WithoutTags;
        if (l2WithTags.isEmpty()) {
            l2WithoutTags = Q.New(L2PortGroupNetworkVO.class).select(L2PortGroupNetworkVO_.uuid).listValues();
        } else {
            l2WithoutTags = Q.New(L2PortGroupNetworkVO.class).notIn(L2PortGroupNetworkVO_.uuid, l2WithTags).select(L2PortGroupNetworkVO_.uuid).listValues();
        }

        for (String l2Uuid : l2WithoutTags) {
            tagMgr.createNonInherentSystemTag(l2Uuid, SriovSystemTags.L2_ENABLE_SRIOV.getTagFormat(), L2NetworkVO.class.getSimpleName());
        }
    }

    private void upgradeBridgeName() {
        List<Tuple> needUpdateTuples = SQL.New("select ref, pg.vSwitchUuid, pg.vlanId from" +
                        " L2NetworkHostRefVO ref, L2PortGroupNetworkVO pg" +
                        " where ref.l2NetworkUuid = pg.uuid" +
                        " and ref.bridgeName is null", Tuple.class)
                .list();

        if (needUpdateTuples.isEmpty()) {
            logger.debug("no need to upgrade bridge name," +
                    " because there are no port groups attached to hosts without bridge name");
            return;
        }

        for (Tuple t : needUpdateTuples) {
            L2NetworkHostRefVO ref = t.get(0, L2NetworkHostRefVO.class);
            String vSwitchUuid = t.get(1, String.class);
            Integer vlanId = t.get(2, Integer.class);

            ref.setBridgeName(VirtualSwitchUtils.makeBridgeName(vSwitchUuid, ref.getL2NetworkUuid(), vlanId));
            dbf.updateAndRefresh(ref);
        }
    }

    private void upgradeUplinkGroup() {
        List<Tuple> needUpdateTuples = SQL.New("select ref, vs from L2NetworkHostRefVO ref, L2VirtualSwitchNetworkVO vs" +
                        " where ref.l2NetworkUuid = vs.uuid" +
                        " and ref.id not in (select ug.id from UplinkGroupVO ug)", Tuple.class)
                .list();

        if (needUpdateTuples.isEmpty()) {
            logger.debug("no need to upgrade uplink group," +
                    " because there are no virtual switches attached to hosts without uplink group");
            return;
        }

        for (Tuple t : needUpdateTuples) {
            L2NetworkHostRefVO ref = t.get(0, L2NetworkHostRefVO.class);
            L2VirtualSwitchNetworkVO vs = t.get(1, L2VirtualSwitchNetworkVO.class);
            String bondingUuid = Q.New(HostNetworkBondingVO.class)
                    .select(HostNetworkBondingVO_.uuid)
                    .eq(HostNetworkBondingVO_.hostUuid, ref.getHostUuid())
                    .eq(HostNetworkBondingVO_.bondingName, vs.getPhysicalInterface())
                    .findValue();

            if (bondingUuid == null) {
                logger.warn(String.format("cannot find bonding[%s] for host[uuid:%s]",
                        vs.getPhysicalInterface(), ref.getHostUuid()));
                continue;
            }

            new SQLBatch() {
                @Override
                protected void scripts() {
                    dbf.getEntityManager().createNativeQuery(
                            String.format("insert into UplinkGroupVO (id, interfaceName, type, bondingUuid)" +
                                            " values ('%s', '%s', '%s', '%s')",
                                    ref.getId(), vs.getPhysicalInterface(), UplinkGroupType.Bonding, bondingUuid))
                            .executeUpdate();
                }
            }.execute();
        }
    }

    private void cleanUpRedundantBridgeNameTags() {
        List<String> pgUuids = Q.New(L2PortGroupNetworkVO.class)
                .select(L2PortGroupNetworkVO_.uuid)
                .listValues();

        if (pgUuids.isEmpty()) {
            logger.debug("no need to clean up redundant bridge name tags because there are no port groups");
            return;
        }

        List<Tuple> tuples = Q.New(SystemTagVO.class)
                .select(SystemTagVO_.resourceUuid, SystemTagVO_.uuid, SystemTagVO_.tag)
                .in(SystemTagVO_.resourceUuid, pgUuids)
                .eq(SystemTagVO_.resourceType, L2NetworkVO.class.getSimpleName())
                .like(SystemTagVO_.tag, TagUtils.tagPatternToSqlPattern(KVMSystemTags.L2_BRIDGE_NAME.getTagFormat()))
                .orderBy(SystemTagVO_.createDate, SimpleQuery.Od.ASC)
                .listTuple();

        List<String> pgUuidsWithTag = new ArrayList<>();
        for (Tuple t : tuples) {
            String pgUuid = t.get(0, String.class);
            String tagUuid = t.get(1, String.class);
            String tag = t.get(2, String.class);
            if (pgUuidsWithTag.contains(pgUuid)) {
                logger.debug(String.format("delete redundant bridgeName system tag[%s] for port group[uuid:%s]", tag, pgUuid));
                tagMgr.deleteSystemTag(tagUuid);
            } else {
                pgUuidsWithTag.add(pgUuid);
            }
        }
    }

    private void disableIPAMForDefaultPortGroup() {
        List<String> defaultPgUuids= SQL.New("select pg.uuid from PortGroupVO pg, SystemTagVO sysTag" +
                        " where sysTag.resourceUuid = pg.uuid" +
                        " and sysTag.tag = :tag", String.class)
                .param("tag", VirtualSwitchSystemTags.PORT_GROUP_DEFAULT.getTagFormat())
                .list();

        if (defaultPgUuids.isEmpty()) {
            logger.debug("no need to disable IPAM for default port groups because there are no default port groups");
            return;
        }

        SQL.New(PortGroupVO.class)
                .in(PortGroupVO_.uuid, defaultPgUuids)
                .set(PortGroupVO_.enableIPAM, false)
                .update();
    }

    @Override
    public boolean start() {
        if (VirtualSwitchGlobalProperty.UPGRADE_L2_VIRTUAL_SWITCH_UPLINK_BONDING) {
            upgradeUplinkBonding();
        }

        if (VirtualSwitchGlobalProperty.UPGRADE_PORT_GROUP) {
            upgradePortGroup();
            upgradeDefaultSystemTags();
        }

        upgradeSriovSystemTags();
        upgradeBridgeName();
        upgradeUplinkGroup();
        cleanUpRedundantBridgeNameTags();
        disableIPAMForDefaultPortGroup();

        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }
}
