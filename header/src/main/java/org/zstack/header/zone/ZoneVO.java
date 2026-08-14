package org.zstack.header.zone;

import org.zstack.header.storage.backup.BackupStorageZoneRefVO;
import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.BaseResource;
import org.zstack.header.vo.EO;

import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table
@EO(EOClazz = ZoneEO.class)
@BaseResource
@EntityGraph(
        friends = {
                @EntityGraph.Neighbour(type = BackupStorageZoneRefVO.class, myField = "uuid", targetField = "zoneUuid")
        }
)
public class ZoneVO extends ZoneAO {
}
