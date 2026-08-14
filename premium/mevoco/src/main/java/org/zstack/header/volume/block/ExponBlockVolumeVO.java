package org.zstack.header.volume.block;

import org.zstack.header.vo.ToInventory;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;

@Entity
@Table
@PrimaryKeyJoinColumn(name="uuid", referencedColumnName="uuid")
public class ExponBlockVolumeVO extends BlockVolumeVO implements ToInventory {
    @Column
    private String exponStatus;

    public String getExponStatus() {
        return exponStatus;
    }

    public void setExponStatus(String exponStatus) {
        this.exponStatus = exponStatus;
    }

    public ExponBlockVolumeVO() {}

    public ExponBlockVolumeVO(BlockVolumeVO other) {
        super(other);
    }
}
