package org.zstack.storage.primary.block;

import org.zstack.header.storage.primary.PrimaryStorageHostRefVO;

import javax.persistence.*;

/**
 * @author Lei Liu lei.liu@zstack.io
 * @date 2022/4/1 00:26
 */

@Entity
@Table
@PrimaryKeyJoinColumn(name = "id", referencedColumnName = "id")
public class BlockPrimaryStorageHostRefVO extends PrimaryStorageHostRefVO {
    @Column
    private String initiatorName;

    @Column
    private String metadata;

    public void setInitiatorName(String initiatorName) {
        this.initiatorName = initiatorName;
    }

    public String getInitiatorName() {
        return initiatorName;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public String getMetadata() {
        return metadata;
    }
}
