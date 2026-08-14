package org.zstack.network.securitygroup;

import org.zstack.header.vm.VmNicVO;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.ForeignKey.ReferenceOption;

import javax.persistence.*;
import java.sql.Timestamp;


@Entity
@Table
public class VmNicSecurityPolicyVO {
    @Id
    @Column
    private String uuid;

    @Column
    @ForeignKey(parentEntityClass = VmNicVO.class, onDeleteAction = ReferenceOption.CASCADE)
    private String vmNicUuid;

    @Column
    private String ingressPolicy;

    @Column
    private String egressPolicy;
    
    @Column
    private Timestamp createDate;
    
    @Column
    private Timestamp lastOpDate;

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getVmNicUuid() {
        return vmNicUuid;
    }

    public void setVmNicUuid(String vmNicUuid) {
        this.vmNicUuid = vmNicUuid;
    }

    public String getIngressPolicy() {
        return ingressPolicy;
    }

    public void setIngressPolicy(String ingressPolicy) {
        this.ingressPolicy = ingressPolicy;
    }

    public String getEgressPolicy() {
        return egressPolicy;
    }

    public void setEgressPolicy(String egressPolicy) {
        this.egressPolicy = egressPolicy;
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
