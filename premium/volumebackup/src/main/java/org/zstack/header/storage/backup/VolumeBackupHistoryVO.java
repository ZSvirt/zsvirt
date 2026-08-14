package org.zstack.header.storage.backup;

import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.volume.VolumeEO;
import org.zstack.header.volume.VolumeVO;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.sql.Timestamp;

@Entity
@Table
@EntityGraph(
        parents = {
                @EntityGraph.Neighbour(type = VolumeVO.class, myField = "uuid", targetField = "uuid"),
                @EntityGraph.Neighbour(type = VolumeBackupVO.class, myField = "lastBackupUuid", targetField = "uuid"),
        }
)
public class VolumeBackupHistoryVO {
    @Column
    @Id
    @ForeignKey(parentEntityClass = VolumeEO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String uuid;

    @Column
    @ForeignKey(parentEntityClass = VolumeBackupVO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String lastBackupUuid;

    @Column
    private String bitmap;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getLastBackupUuid() {
        return lastBackupUuid;
    }

    public void setLastBackupUuid(String lastBackupUuid) {
        this.lastBackupUuid = lastBackupUuid;
    }

    public String getBitmap() {
        return bitmap;
    }

    public void setBitmap(String bitmap) {
        this.bitmap = bitmap;
    }

    public Timestamp getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Timestamp createDate) {
        this.createDate = createDate;
    }

    public Timestamp getLastOpDate() {
        return lastOpDate;
    }

    public void setLastOpDate(Timestamp lastOpDate) {
        this.lastOpDate = lastOpDate;
    }
}
