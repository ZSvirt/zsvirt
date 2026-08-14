package org.zstack.network.service.lb;

import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.ForeignKey.ReferenceOption;
import org.zstack.header.vo.ToInventory;
import org.zstack.header.acl.AccessControlListVO;

import javax.persistence.*;
import java.sql.Timestamp;

/**
 * @author: zhanyong.miao
 * @date: 2020-03-11
 **/
@Entity
@Table
@EntityGraph(
        friends = {
                @EntityGraph.Neighbour(type = LoadBalancerListenerVO.class, myField = "listenerUuid", targetField = "uuid"),
                @EntityGraph.Neighbour(type = AccessControlListVO.class, myField = "aclUuid", targetField = "uuid"),
        }
)
public class LoadBalancerListenerACLRefVO implements ToInventory {
    @Id
    @Column
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @Column
    @ForeignKey(parentEntityClass = LoadBalancerListenerVO.class, parentKey = "uuid", onDeleteAction = ReferenceOption.RESTRICT)
    private String listenerUuid;

    @Column
    @ForeignKey(parentEntityClass = AccessControlListVO.class, parentKey = "uuid", onDeleteAction = ReferenceOption.RESTRICT)
    private String aclUuid;

    @Column
    @Enumerated(EnumType.STRING)
    private LoadBalancerAclType type;

    @Column
    private String serverGroupUuid;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getListenerUuid() {
        return listenerUuid;
    }

    public void setListenerUuid(String listenerUuid) {
        this.listenerUuid = listenerUuid;
    }

    public String getAclUuid() {
        return aclUuid;
    }

    public void setAclUuid(String aclUuid) {
        this.aclUuid = aclUuid;
    }

    public LoadBalancerAclType getType() {
        return type;
    }

    public void setType(LoadBalancerAclType type) {
        this.type = type;
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

    public String getServerGroupUuid() {
        return serverGroupUuid;
    }

    public void setServerGroupUuid(String serverGroupUuid) {
        this.serverGroupUuid = serverGroupUuid;
    }
}
