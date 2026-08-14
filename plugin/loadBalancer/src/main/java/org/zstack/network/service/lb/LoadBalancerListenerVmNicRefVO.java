package org.zstack.network.service.lb;

import org.zstack.header.vm.VmNicVO;
import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.ForeignKey.ReferenceOption;

import javax.persistence.*;
import java.sql.Timestamp;

/**
 * Created by frank on 8/8/2015.
 */
@Entity
@Table
@EntityGraph(
        friends = {
                @EntityGraph.Neighbour(type = LoadBalancerListenerVO.class, myField = "listenerUuid", targetField = "uuid"),
                @EntityGraph.Neighbour(type = VmNicVO.class, myField = "vmNicUuid", targetField = "uuid"),
        }
)
public class LoadBalancerListenerVmNicRefVO {
    @Id
    @Column
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private long id;

    @Column
    @ForeignKey(parentEntityClass = LoadBalancerListenerVO.class, parentKey = "uuid", onDeleteAction = ReferenceOption.CASCADE)
    private String listenerUuid;

    @Column
    @ForeignKey(parentEntityClass = VmNicVO.class, parentKey = "uuid", onDeleteAction = ReferenceOption.CASCADE)
    private String vmNicUuid;

    @Column
    @Enumerated(EnumType.STRING)
    private LoadBalancerVmNicStatus status;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
    }

    public LoadBalancerVmNicStatus getStatus() {
        return status;
    }

    public void setStatus(LoadBalancerVmNicStatus status) {
        this.status = status;
    }

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

    public String getVmNicUuid() {
        return vmNicUuid;
    }

    public void setVmNicUuid(String vmNicUuid) {
        this.vmNicUuid = vmNicUuid;
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
