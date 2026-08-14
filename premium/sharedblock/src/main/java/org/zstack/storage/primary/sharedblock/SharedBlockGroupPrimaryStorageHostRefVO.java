package org.zstack.storage.primary.sharedblock;

import org.zstack.header.storage.primary.*;

import javax.persistence.*;

@Entity
@Table
@PrimaryKeyJoinColumn(name = "id", referencedColumnName = "id")
public class SharedBlockGroupPrimaryStorageHostRefVO extends PrimaryStorageHostRefVO {
    @Column
    private Integer hostId;

    public SharedBlockGroupPrimaryStorageHostRefVO() {
    }

    public SharedBlockGroupPrimaryStorageHostRefVO(PrimaryStorageHostRefVO vo) {
        super(vo);
    }

    public Integer getHostId() {
        return hostId;
    }

    public void setHostId(Integer hostId) {
        this.hostId = hostId;
    }
}
