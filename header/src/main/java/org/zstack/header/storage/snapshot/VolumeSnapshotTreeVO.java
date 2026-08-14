package org.zstack.header.storage.snapshot;

import org.zstack.header.vo.BaseResource;
import org.zstack.header.vo.EO;
import org.zstack.header.vo.EntityGraph;
import org.zstack.header.volume.VolumeVO;

import javax.persistence.*;

/**
 */
@Entity
@Table
@EO(EOClazz = VolumeSnapshotTreeEO.class)
@BaseResource
@EntityGraph(
        parents = {
                @EntityGraph.Neighbour(type = VolumeVO.class, myField = "volumeUuid", targetField = "uuid")
        }
)
public class VolumeSnapshotTreeVO extends VolumeSnapshotTreeAO {
}
