package org.zstack.header.storage.backup;

import org.zstack.header.image.ImageBackupStorageRefVO;
import org.zstack.header.vo.BaseResource;
import org.zstack.header.vo.EO;
import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.NoView;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table
@EO(EOClazz = BackupStorageEO.class)
@BaseResource
@EntityGraph(
        friends = {
                @EntityGraph.Neighbour(type = BackupStorageZoneRefVO.class, myField = "uuid", targetField = "backupStorageUuid"),
                @EntityGraph.Neighbour(type = ImageBackupStorageRefVO.class, myField = "uuid", targetField = "backupStorageUuid")
        }
)
public class BackupStorageVO extends BackupStorageAO {
    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "backupStorageUuid", insertable = false, updatable = false)
    @NoView
    private Set<BackupStorageZoneRefVO> attachedZoneRefs = new HashSet<BackupStorageZoneRefVO>();

    public BackupStorageVO() {
    }

    protected BackupStorageVO(BackupStorageVO vo) {
        this.setUuid(vo.getUuid());
        this.setAttachedZoneRefs(vo.getAttachedZoneRefs());
        this.setCreateDate(vo.getCreateDate());
        this.setDescription(vo.getDescription());
        this.setLastOpDate(vo.getLastOpDate());
        this.setName(vo.getName());
        this.setState(vo.getState());
        this.setStatus(vo.getStatus());
        this.setTotalCapacity(vo.getTotalCapacity());
        this.setType(vo.getType());
        this.setUrl(vo.getUrl());
        this.setAvailableCapacity(vo.getAvailableCapacity());
    }

    public Set<BackupStorageZoneRefVO> getAttachedZoneRefs() {
        return attachedZoneRefs;
    }

    public void setAttachedZoneRefs(Set<BackupStorageZoneRefVO> attachedZoneRefs) {
        this.attachedZoneRefs = attachedZoneRefs;
    }

}
