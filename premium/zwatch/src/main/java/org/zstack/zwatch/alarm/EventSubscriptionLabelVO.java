package org.zstack.zwatch.alarm;

import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.ForeignKey;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table
@EntityGraph(
        parents = {
                @EntityGraph.Neighbour(type = EventSubscriptionVO.class, myField = "subscriptionUuid", targetField = "uuid")
        }
)
public class EventSubscriptionLabelVO extends LabelAO {
    @Column
    @ForeignKey(parentEntityClass = EventSubscriptionVO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String subscriptionUuid;

    public String getSubscriptionUuid() {
        return subscriptionUuid;
    }

    public void setSubscriptionUuid(String subscriptionUuid) {
        this.subscriptionUuid = subscriptionUuid;
    }
}
