package org.zstack.storage.snapshot.group;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupInventory;
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupVO;
import org.zstack.header.vm.ArchiveVmNicBundle;
import org.zstack.header.vm.devices.VmInstanceResourceMetadataArchiveVO;
import org.zstack.header.vm.devices.VmInstanceResourceMetadataArchiveVO_;

import javax.persistence.TypedQuery;
import java.util.List;

/**
 * Created by LiangHanYu on 2022/7/8 16:50
 */
public class L3NetworkMemorySnapshotGroupReference implements MemorySnapshotGroupReferenceFactory {
    @Autowired
    private DatabaseFacade dbf;

    @Override
    public String getReferenceResourceType() {
        return L3NetworkVO.class.getSimpleName();
    }

    @Override
    public List<VolumeSnapshotGroupInventory> getVolumeSnapshotGroupReferenceList(String resourceUuid) {
        List<String> quotedArchiveGroupList = Q.New(VmInstanceResourceMetadataArchiveVO.class)
                .select(VmInstanceResourceMetadataArchiveVO_.addressGroupUuid)
                .eq(VmInstanceResourceMetadataArchiveVO_.metadataClass, ArchiveVmNicBundle.class.getCanonicalName())
                .like(VmInstanceResourceMetadataArchiveVO_.metadata, String.format("%%\"l3NetworkUuid\":\"%s\"%%", resourceUuid)).listValues();

        if (quotedArchiveGroupList.isEmpty()) {
            return null;
        }

        String sql = "select snapshotGroup from VolumeSnapshotGroupVO snapshotGroup, VmInstanceResourceMetadataGroupVO deviceAddressGroup where snapshotGroup.uuid = deviceAddressGroup.resourceUuid and deviceAddressGroup.uuid in :addressGroupUuids";
        TypedQuery<VolumeSnapshotGroupVO> q = dbf.getEntityManager().createQuery(sql, VolumeSnapshotGroupVO.class);
        q.setParameter("addressGroupUuids", quotedArchiveGroupList);
        List<VolumeSnapshotGroupVO> result = q.getResultList();
        return VolumeSnapshotGroupInventory.valueOf(result);
    }
}
