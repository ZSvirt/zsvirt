package org.zstack.zwatch.alarm;

import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.ForeignKey;

import javax.persistence.*;
import java.io.Serializable;
import java.sql.Timestamp;

@Entity
@Table
@IdClass(EventSubscriptionActionVO.CompositeId.class)
@EntityGraph(
        parents = {
                @EntityGraph.Neighbour(type = EventSubscriptionVO.class, myField = "subscriptionUuid", targetField = "uuid")
        }
)
public class EventSubscriptionActionVO {
    static class CompositeId implements Serializable {
        private String subscriptionUuid;
        private String actionUuid;

        public String getSubscriptionUuid() {
            return subscriptionUuid;
        }

        public void setSubscriptionUuid(String subscriptionUuid) {
            this.subscriptionUuid = subscriptionUuid;
        }

        public String getActionUuid() {
            return actionUuid;
        }

        public void setActionUuid(String actionUuid) {
            this.actionUuid = actionUuid;
        }
    }

    @Column
    @Id
    @ForeignKey(parentEntityClass = EventSubscriptionVO.class, parentKey = "uuid", onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String subscriptionUuid;
    @Column
    private String actionType;
    @Column
    @Id
    private String actionUuid;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    public String getSubscriptionUuid() {
        return subscriptionUuid;
    }

    public void setSubscriptionUuid(String subscriptionUuid) {
        this.subscriptionUuid = subscriptionUuid;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public String getActionUuid() {
        return actionUuid;
    }

    public void setActionUuid(String actionUuid) {
        this.actionUuid = actionUuid;
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
