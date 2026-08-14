package org.zstack.network.l2.virtualSwitch.header;

import org.zstack.header.host.HostEO;
import org.zstack.header.identity.OwnedByAccount;
import org.zstack.header.network.l2.L2NetworkVO;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.NoView;
import org.zstack.header.vo.ResourceVO;
import org.zstack.header.vo.SoftDeletionCascade;
import org.zstack.header.vo.SoftDeletionCascades;

import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.*;

@Entity
@Table
@PrimaryKeyJoinColumn(name = "uuid", referencedColumnName = "uuid")
@SoftDeletionCascades({
        @SoftDeletionCascade(parent = HostEO.class, joinColumn = "hostUuid")
})
@EntityGraph(
    parents = {
                @EntityGraph.Neighbour(type = HostKernelInterfaceUsedIpVO.class, myField = "uuid", targetField = "hostKernelInterfaceUuid"),
                @EntityGraph.Neighbour(type = HostKernelInterfaceTrafficTypeVO.class, myField = "uuid", targetField = "hostKernelInterfaceUuid"),
        }
)
public class HostKernelInterfaceVO extends ResourceVO implements OwnedByAccount {

    @Column
    private String name;

    @Column
    private String description;

    @Column
    @ForeignKey(parentEntityClass = HostEO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String hostUuid;

    @Column
    @ForeignKey(parentEntityClass = L2NetworkVO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String l2NetworkUuid;

    @Column
    @ForeignKey(parentEntityClass = L3NetworkVO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String l3NetworkUuid;

    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "hostKernelInterfaceUuid", insertable = false, updatable = false)
    @NoView
    private Set<HostKernelInterfaceUsedIpVO> usedIps = new HashSet<HostKernelInterfaceUsedIpVO>();

    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "hostKernelInterfaceUuid", insertable = false, updatable = false)
    @NoView
    private Set<HostKernelInterfaceTrafficTypeVO> trafficTypes = new HashSet<HostKernelInterfaceTrafficTypeVO>();

    @Column
    private Timestamp createDate;
    
    @Column
    private Timestamp lastOpDate;

    @Transient
    private String accountUuid;

    @Override
    public String getAccountUuid() {
        return accountUuid;
    }

    @Override
    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public String getL2NetworkUuid() {
        return l2NetworkUuid;
    }

    public void setL2NetworkUuid(String l2NetworkUuid) {
        this.l2NetworkUuid = l2NetworkUuid;
    }

    public String getL3NetworkUuid() {
        return l3NetworkUuid;
    }

    public void setL3NetworkUuid(String l3NetworkUuid) {
        this.l3NetworkUuid = l3NetworkUuid;
    }

    public Set<HostKernelInterfaceUsedIpVO> getUsedIps() {
        return usedIps;
    }

    public void setUsedIps(Set<HostKernelInterfaceUsedIpVO> usedIps) {
        this.usedIps = usedIps;
    }

    public Set<HostKernelInterfaceTrafficTypeVO> getTrafficTypes() {
        return trafficTypes;
    }

    public void setTrafficTypes(Set<HostKernelInterfaceTrafficTypeVO> trafficTypes) {
        this.trafficTypes = trafficTypes;
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
