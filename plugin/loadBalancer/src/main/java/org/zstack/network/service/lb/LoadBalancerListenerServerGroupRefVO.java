package org.zstack.network.service.lb;

import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.ForeignKey;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@Table
@EntityGraph(
        friends = {
                @EntityGraph.Neighbour(type = LoadBalancerListenerVO.class, myField = "listenerUuid", targetField = "uuid"),
                @EntityGraph.Neighbour(type = LoadBalancerServerGroupVO.class, myField = "serverGroupUuid", targetField = "uuid"),
        }
)
public class LoadBalancerListenerServerGroupRefVO {
        @Id
        @Column
        @GeneratedValue(strategy=GenerationType.IDENTITY)
        private long id;

        @Column
        @ForeignKey(parentEntityClass = LoadBalancerListenerVO.class, parentKey = "uuid", onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
        private String listenerUuid;

        @Column
        @ForeignKey(parentEntityClass = LoadBalancerServerGroupVO.class, parentKey = "uuid", onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
        private String serverGroupUuid;

        @Column
        private Timestamp createDate;

        @Column
        private Timestamp lastOpDate;

        public long getId() {
                return id;
        }

        public void setId(long id) {
                this.id = id;
        }

        public String getListenerUuid() {
                return listenerUuid;
        }

        public void setListenerUuid(String listenerUuid) {
                this.listenerUuid = listenerUuid;
        }

        public String getServerGroupUuid() {
                return serverGroupUuid;
        }

        public void setServerGroupUuid(String serverGroupUuid) {
                this.serverGroupUuid = serverGroupUuid;
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
