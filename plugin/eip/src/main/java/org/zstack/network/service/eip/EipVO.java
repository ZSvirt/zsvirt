package org.zstack.network.service.eip;

import org.zstack.header.identity.OwnedByAccount;
import org.zstack.header.vm.VmNicVO;
import org.zstack.header.vo.BaseResource;
import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.ForeignKey.ReferenceOption;
import org.zstack.header.vo.Index;
import org.zstack.header.vo.ResourceVO;
import org.zstack.network.service.vip.VipVO;

import javax.persistence.*;
import java.sql.Timestamp;

/**
 */
@Entity
@Table
@BaseResource
@EntityGraph(
        parents = {
                @EntityGraph.Neighbour(type = VipVO.class, myField = "vipUuid", targetField = "uuid")
        },

        friends = {
                @EntityGraph.Neighbour(type = VmNicVO.class, myField = "vmNicUuid", targetField = "uuid")
        }
)
public class EipVO extends ResourceVO implements OwnedByAccount {
    @Column
    @Index
    private String  name;

    @Column
    private String  description;

    @Column
    private String  vipIp;

    @Column
    private String  guestIp;

    @Column
    @ForeignKey(parentEntityClass = VmNicVO.class, onDeleteAction = ReferenceOption.SET_NULL)
    private String vmNicUuid;

    @Column
    @Enumerated(EnumType.STRING)
    private EipState state;

    @Column
    @ForeignKey(parentEntityClass = VipVO.class, onDeleteAction = ReferenceOption.CASCADE)
    private String vipUuid;

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

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
    }

    public String getVipIp() {
        return vipIp;
    }

    public void setVipIp(String vipIp) {
        this.vipIp = vipIp;
    }

    public String getGuestIp() {
        return guestIp;
    }

    public void setGuestIp(String guestIp) {
        this.guestIp = guestIp;
    }

    public EipState getState() {
        return state;
    }

    public void setState(EipState state) {
        this.state = state;
    }

    public String getVipUuid() {
        return vipUuid;
    }

    public void setVipUuid(String vipUuid) {
        this.vipUuid = vipUuid;
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
