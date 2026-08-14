package org.zstack.storage.snapshot.group;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.network.l2.L2NetworkOwnedL3ExtensionPoint;
import org.zstack.header.network.l2.L2NetworkVO;
import org.zstack.header.network.l2.L2NetworkVO_;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.network.l3.L3NetworkVO_;
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupInventory;
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupVO;
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupVO_;
import org.zstack.header.vm.ArchiveVmNicBundle;
import org.zstack.header.vm.devices.VmInstanceResourceMetadataArchiveVO;
import org.zstack.header.vm.devices.VmInstanceResourceMetadataArchiveVO_;
import org.zstack.utils.gson.JSONObjectUtil;

import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import java.util.*;

public class L2NetworkMemorySnapshotGroupReference implements MemorySnapshotGroupReferenceFactory {
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private PluginRegistry pluginRgty;

    @Override
    public String getReferenceResourceType() {
        return L2NetworkVO.class.getSimpleName();
    }

    @Override
    public List<VolumeSnapshotGroupInventory> getVolumeSnapshotGroupReferenceList(String resourceUuid) {
        List<Tuple> archiveGroups = Q.New(VmInstanceResourceMetadataArchiveVO.class)
                .select(VmInstanceResourceMetadataArchiveVO_.addressGroupUuid, VmInstanceResourceMetadataArchiveVO_.metadata,VmInstanceResourceMetadataArchiveVO_.resourceUuid)
                .eq(VmInstanceResourceMetadataArchiveVO_.metadataClass, ArchiveVmNicBundle.class.getCanonicalName())
                .listTuple();
        if (archiveGroups.isEmpty()) {
            return null;
        }

        List<String> l2OwnedL3NetworkUuids = getL3NetworkUuids(resourceUuid);

        Map<String, List<String>> l3NetworkUuidByGroupUuid = new HashMap<>();
        for (Tuple tuple : archiveGroups) {
            String groupUuid = tuple.get(0, String.class);
            String metadata = tuple.get(1, String.class);
            ArchiveVmNicBundle archiveVmNicBundle = JSONObjectUtil.toObject(metadata, ArchiveVmNicBundle.class);
            String l3NetworkUuid = archiveVmNicBundle.getVmNicInventory().getL3NetworkUuid();
            l3NetworkUuidByGroupUuid.computeIfAbsent(groupUuid, k -> new ArrayList<>());
            l3NetworkUuidByGroupUuid.get(groupUuid).add(l3NetworkUuid);
        }

        Set<String> addressGroupUuids = new HashSet<>();
        l3NetworkUuidByGroupUuid.forEach((groupUuid, l3Uuids) -> {
            l3Uuids.forEach(l3Uuid -> {
                if (l2OwnedL3NetworkUuids.contains(l3Uuid)) {
                    addressGroupUuids.add(groupUuid);
                }
            });
        });
        if (addressGroupUuids.isEmpty()) {
            return new ArrayList<>();
        }

        String sql = "select snapshotGroup from VolumeSnapshotGroupVO snapshotGroup, VmInstanceResourceMetadataGroupVO deviceAddressGroup " +
                "where snapshotGroup.uuid = deviceAddressGroup.resourceUuid and deviceAddressGroup.uuid in :addressGroupUuids";
        TypedQuery<VolumeSnapshotGroupVO> q = dbf.getEntityManager().createQuery(sql, VolumeSnapshotGroupVO.class);
        q.setParameter("addressGroupUuids", addressGroupUuids);
        List<VolumeSnapshotGroupVO> result = q.getResultList();
        return VolumeSnapshotGroupInventory.valueOf(result);
    }

    private List<String> getL3NetworkUuids(String l2NetworkUuid) {
        List<String> l3Uuids = Q.New(L3NetworkVO.class).eq(L3NetworkVO_.l2NetworkUuid, l2NetworkUuid).select(L3NetworkVO_.uuid).listValues();
        String l2Type = Q.New(L2NetworkVO.class).eq(L2NetworkVO_.uuid, l2NetworkUuid).select(L2NetworkVO_.type).findValue();
        for (L2NetworkOwnedL3ExtensionPoint ext : pluginRgty.getExtensionList(L2NetworkOwnedL3ExtensionPoint.class)) {
            if (Objects.equals(ext.getType().toString(), l2Type)) {
                l3Uuids.addAll(ext.getOwnedL3NetworkUuids(l2NetworkUuid));
            }
        }
        return l3Uuids;
    }
}