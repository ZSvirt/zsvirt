package org.zstack.storage.device.multipath;

import org.zstack.header.tag.AutoDeleteTag;
import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.ResourceVO;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * Create by weiwang at 2018/8/2
 */

@Entity
@Table
@AutoDeleteTag
@EntityGraph(
        parents = {
                @EntityGraph.Neighbour(type = MultipathDeviceVO.class, myField = "multipathDeviceUuid", targetField = "uuid")
        }
)
public class MultiPathGroupVO extends ResourceVO {
    @Column
    @ForeignKey(parentEntityClass = MultipathDeviceVO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String multipathDeviceUuid;

    @Column
    private String schedulingPolicy;

    @Column
    private String priority;

    @Column
    private String status;
}
