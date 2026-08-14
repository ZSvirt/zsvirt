package org.zstack.header.storageDevice;

import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.NoView;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

/**
 * Create by weiwang at 2018/10/18
 */
@Entity
@Table
@EntityGraph(
        friends = {
                @EntityGraph.Neighbour(type = ScsiLunHostRefVO.class, myField = "uuid", targetField = "scsiLunUuid"),
                @EntityGraph.Neighbour(type = ScsiLunVmInstanceRefVO.class, myField = "uuid", targetField = "scsiLunUuid")
        }
)
public class ScsiLunVO extends LunAO {
    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name="scsiLunUuid", insertable=false, updatable=false)
    @NoView
    private Set<ScsiLunHostRefVO> scsiLunHostRefs = new HashSet<ScsiLunHostRefVO>();

    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name="scsiLunUuid", insertable=false, updatable=false)
    @NoView
    private Set<ScsiLunVmInstanceRefVO> scsiLunVmInstanceRefs = new HashSet<ScsiLunVmInstanceRefVO>();

    public void setScsiLunHostRefs(Set<ScsiLunHostRefVO> scsiLunHostRefs) {
        this.scsiLunHostRefs = scsiLunHostRefs;
    }

    public Set<ScsiLunHostRefVO> getScsiLunHostRefs() {
        return scsiLunHostRefs;
    }

    public void setScsiLunVmInstanceRefs(Set<ScsiLunVmInstanceRefVO> scsiLunVmInstanceRefs) {
        this.scsiLunVmInstanceRefs = scsiLunVmInstanceRefs;
    }

    public Set<ScsiLunVmInstanceRefVO> getScsiLunVmInstanceRefs() {
        return scsiLunVmInstanceRefs;
    }
}
