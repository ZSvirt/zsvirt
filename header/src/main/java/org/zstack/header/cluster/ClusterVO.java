package org.zstack.header.cluster;

import org.zstack.header.network.l2.L2NetworkClusterRefVO;
import org.zstack.header.vo.EntityGraph;
import org.zstack.header.storage.primary.PrimaryStorageClusterRefVO;
import org.zstack.header.vo.BaseResource;
import org.zstack.header.vo.EO;
import org.zstack.header.zone.ZoneVO;

import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table
@EO(EOClazz = ClusterEO.class)
@BaseResource
@EntityGraph(
        parents = {
                @EntityGraph.Neighbour(type = ZoneVO.class, myField = "zoneUuid", targetField = "uuid")
        },
        friends = {
                @EntityGraph.Neighbour(type = PrimaryStorageClusterRefVO.class, myField = "uuid", targetField = "clusterUuid"),
                @EntityGraph.Neighbour(type = L2NetworkClusterRefVO.class, myField = "uuid", targetField = "clusterUuid")
        }
)
public class ClusterVO extends ClusterAO {
}
