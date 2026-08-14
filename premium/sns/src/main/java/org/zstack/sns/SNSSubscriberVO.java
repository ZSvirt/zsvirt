package org.zstack.sns;

import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.ForeignKey;

import javax.persistence.*;
import java.io.Serializable;
import java.sql.Timestamp;

@Entity
@Table
@IdClass(SNSSubscriberVO.CompositeId.class)
@EntityGraph(
        friends = {
                @EntityGraph.Neighbour(type = SNSTopicVO.class, myField = "topicUuid", targetField = "uuid"),
                @EntityGraph.Neighbour(type = SNSApplicationEndpointVO.class, myField = "endpointUuid", targetField = "uuid"),
        }
)
public class SNSSubscriberVO {
    static class CompositeId implements Serializable {
        private String topicUuid;
        private String endpointUuid;

        public String getTopicUuid() {
            return topicUuid;
        }

        public void setTopicUuid(String topicUuid) {
            this.topicUuid = topicUuid;
        }

        public String getEndpointUuid() {
            return endpointUuid;
        }

        public void setEndpointUuid(String endpointUuid) {
            this.endpointUuid = endpointUuid;
        }
    }

    @Column
    @Id
    @ForeignKey(parentEntityClass = SNSTopicVO.class, parentKey = "uuid", onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String topicUuid;
    @Column
    @Id
    @ForeignKey(parentEntityClass = SNSApplicationEndpointVO.class, parentKey = "uuid", onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String endpointUuid;
    @Column
    private Timestamp createDate;
    @Column
    private Timestamp lastOpDate;

    public String getTopicUuid() {
        return topicUuid;
    }

    public void setTopicUuid(String topicUuid) {
        this.topicUuid = topicUuid;
    }

    public String getEndpointUuid() {
        return endpointUuid;
    }

    public void setEndpointUuid(String endpointUuid) {
        this.endpointUuid = endpointUuid;
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
