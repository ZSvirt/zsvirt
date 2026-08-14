package org.zstack.header.storage.addon.primary;

import org.zstack.header.storage.primary.PrimaryStorageHostRefVO;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;

@Entity
@Table
@PrimaryKeyJoinColumn(name = "id", referencedColumnName = "id")
public class ExternalPrimaryStorageHostRefVO extends PrimaryStorageHostRefVO {
    @Column
    private String protocol;

    @Column
    private int hostId;

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public int getHostId() {
        return hostId;
    }

    public void setHostId(int hostId) {
        this.hostId = hostId;
    }
}
