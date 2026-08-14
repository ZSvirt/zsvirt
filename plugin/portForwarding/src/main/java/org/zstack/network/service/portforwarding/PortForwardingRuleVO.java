package org.zstack.network.service.portforwarding;

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

@Entity
@Table
@BaseResource
@EntityGraph(
        parents = {
                @EntityGraph.Neighbour(type = VipVO.class, myField = "vipUuid", targetField = "uuid"),
        },

        friends = {
                @EntityGraph.Neighbour(type = VmNicVO.class, myField = "vmNicUuid", targetField = "uuid"),
        }
)
public class PortForwardingRuleVO extends ResourceVO implements OwnedByAccount {
    @Column
    @Index
    private String name;
    
    @Column
    private String description;

    @Column
    @ForeignKey(parentEntityClass = VipVO.class, onDeleteAction = ReferenceOption.RESTRICT)
    private String vipUuid;

    @Column
    private String vipIp;

    @Column
    private String guestIp;

    @Column
    @Index
    private int vipPortStart;
    
    @Column
    @Index
    private int vipPortEnd;
    
    @Column
    @Index
    private int privatePortStart;
    
    @Column
    @Index
    private int privatePortEnd;
    
    @Column
    @ForeignKey(parentEntityClass = VmNicVO.class, onDeleteAction = ReferenceOption.SET_NULL)
    private String vmNicUuid;
    
    @Column
    private String allowedCidr;

    @Column
    @Enumerated(EnumType.STRING)
    private PortForwardingRuleState state;

    @Column
	@Enumerated(EnumType.STRING)
    private PortForwardingProtocolType protocolType;
    
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

    public PortForwardingRuleState getState() {
        return state;
    }

    public void setState(PortForwardingRuleState state) {
        this.state = state;
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

    public String getVipUuid() {
        return vipUuid;
    }

    public void setVipUuid(String vipUuid) {
        this.vipUuid = vipUuid;
    }

    public int getVipPortStart() {
        return vipPortStart;
    }

    public void setVipPortStart(int vipPortStart) {
        this.vipPortStart = vipPortStart;
    }

    public int getVipPortEnd() {
        return vipPortEnd;
    }

    public void setVipPortEnd(int vipPortEnd) {
        this.vipPortEnd = vipPortEnd;
    }

    public int getPrivatePortStart() {
        return privatePortStart;
    }

    public void setPrivatePortStart(int privatePortStart) {
        this.privatePortStart = privatePortStart;
    }

    public int getPrivatePortEnd() {
        return privatePortEnd;
    }

    public void setPrivatePortEnd(int privatePortEnd) {
        this.privatePortEnd = privatePortEnd;
    }

    public String getVmNicUuid() {
        return vmNicUuid;
    }

    public void setVmNicUuid(String vmNicUuid) {
        this.vmNicUuid = vmNicUuid;
    }

    public String getAllowedCidr() {
        return allowedCidr;
    }

    public void setAllowedCidr(String allowedCidr) {
        this.allowedCidr = allowedCidr;
    }

    public PortForwardingProtocolType getProtocolType() {
        return protocolType;
    }

    public void setProtocolType(PortForwardingProtocolType protocolType) {
        this.protocolType = protocolType;
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
