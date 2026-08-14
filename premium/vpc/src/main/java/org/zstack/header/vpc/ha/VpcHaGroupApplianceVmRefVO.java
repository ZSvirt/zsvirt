package org.zstack.header.vpc.ha;

import org.zstack.appliancevm.ApplianceVmVO;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.ForeignKey.ReferenceOption;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table
public class VpcHaGroupApplianceVmRefVO {
    @Id
    @Column
    @ForeignKey(parentEntityClass = ApplianceVmVO.class, onDeleteAction = ReferenceOption.RESTRICT)
    private String uuid;

    @Column
    @ForeignKey(parentEntityClass = VpcHaGroupVO.class, onDeleteAction = ReferenceOption.CASCADE)
    private String vpcHaRouterUuid;


    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getVpcHaRouterUuid() {
        return vpcHaRouterUuid;
    }

    public void setVpcHaRouterUuid(String vpcHaRouterUuid) {
        this.vpcHaRouterUuid = vpcHaRouterUuid;
    }
}
