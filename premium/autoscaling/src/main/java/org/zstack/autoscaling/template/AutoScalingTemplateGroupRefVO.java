package org.zstack.autoscaling.template;

import org.zstack.autoscaling.group.AutoScalingGroupVO;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.network.l2.L2NetworkVO;
import org.zstack.header.search.SqlTrigger;
import org.zstack.header.search.TriggerIndex;
import org.zstack.header.vo.*;
import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.ForeignKey;
import javax.persistence.*;
import java.sql.Timestamp;

/**
 * Create by weiwang at 2018/8/16
 */
@Entity
@Table
@TriggerIndex
@SqlTrigger(foreignVOClass = AutoScalingGroupVO.class, foreignVOJoinColumn = "groupUuid")
@EntityGraph(
        friends = {
                @EntityGraph.Neighbour(type = AutoScalingTemplateVO.class, myField = "templateUuid", targetField = "uuid"),
                @EntityGraph.Neighbour(type = AutoScalingGroupVO.class, myField = "groupUuid", targetField = "uuid"),
        }
)
public class AutoScalingTemplateGroupRefVO {
    @Column
    @ForeignKey(parentEntityClass = AutoScalingTemplateVO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String templateUuid;

    @Column
    @Id
    @ForeignKey(parentEntityClass = AutoScalingGroupVO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String groupUuid;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    public String getTemplateUuid() {
        return templateUuid;
    }

    public void setTemplateUuid(String templateUuid) {
        this.templateUuid = templateUuid;
    }

    public String getGroupUuid() {
        return groupUuid;
    }

    public void setGroupUuid(String groupUuid) {
        this.groupUuid = groupUuid;
    }

    public Timestamp getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Timestamp createDate) {
        this.createDate = createDate;
    }

    public Timestamp getLastOpDate() {
        return lastOpDate;
    }

    public void setLastOpDate(Timestamp lastOpDate) {
        this.lastOpDate = lastOpDate;
    }
}
