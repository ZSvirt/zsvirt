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
                @EntityGraph.Neighbour(type = MultiPathGroupVO.class, myField = "multipathGroupUuid", targetField = "uuid")
        }
)
public class MultiPathVO extends ResourceVO {
    @Column
    @ForeignKey(parentEntityClass = MultiPathGroupVO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String multipathGroupUuid;

    @Column
    private String path;

    @Column
    private String dmStatus;

    @Column
    private String pathStatus;

    @Column
    private String onlineStatus;
}
