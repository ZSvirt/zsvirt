package org.zstack.storage.snapshot.group;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupInventory;
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupVO;
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupVO_;
import org.zstack.header.volume.VolumeType;
import org.zstack.header.volume.VolumeVO;

import javax.persistence.TypedQuery;
import java.util.List;

/**
 * Created by LiangHanYu on 2022/7/8 17:08
 */
public class VolumeMemorySnapshotGroupReferenceFactory implements MemorySnapshotGroupReferenceFactory {
    @Autowired
    private DatabaseFacade dbf;

    @Override
    public String getReferenceResourceType() {
        return VolumeVO.class.getSimpleName();
    }

    @Override
    public List<VolumeSnapshotGroupInventory> getVolumeSnapshotGroupReferenceList(String resourceUuid) {
        String sql = "select distinct mGroupRef.volumeSnapshotGroupUuid from VolumeSnapshotGroupRefVO mGroupRef where mGroupRef.volumeType = :mVolumeType and mGroupRef.volumeSnapshotGroupUuid in (select dGroupRef.volumeSnapshotGroupUuid from VolumeSnapshotGroupRefVO dGroupRef where dGroupRef.volumeType = :dVolumeType and dGroupRef.volumeUuid = :volumeUuid)";
        TypedQuery<String> q = dbf.getEntityManager().createQuery(sql, String.class);
        q.setParameter("mVolumeType", VolumeType.Memory.toString());
        q.setParameter("dVolumeType", VolumeType.Data.toString());
        q.setParameter("volumeUuid", resourceUuid);
        List<String> result = q.getResultList();
        if (result.isEmpty()) {
            return null;
        }
        return VolumeSnapshotGroupInventory.valueOf(Q.New(VolumeSnapshotGroupVO.class).in(VolumeSnapshotGroupVO_.uuid, result).list());
    }
}
