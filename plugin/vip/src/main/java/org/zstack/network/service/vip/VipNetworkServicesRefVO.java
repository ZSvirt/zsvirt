package org.zstack.network.service.vip;

import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.ForeignKey.ReferenceOption;

import javax.persistence.*;
import java.io.Serializable;
import java.sql.Timestamp;

/**
 * @author: zhanyong.miao
 * @date: 2019-05-06
 **/
@Entity
@Table
@EntityGraph(
        parents = {
                @EntityGraph.Neighbour(type = VipVO.class, myField = "vipUuid", targetField = "uuid")
        }
)
@IdClass(VipNetworkServicesRefVO.CompositeID.class)
public class VipNetworkServicesRefVO {
    static class CompositeID implements Serializable {
        private String uuid;
        private String serviceType;
        private String vipUuid;

        public String getUuid() {
            return uuid;
        }

        public void setUuid(String uuid) {
            this.uuid = uuid;
        }

        public String getServiceType() {
            return serviceType;
        }

        public void setServiceType(String serviceType) {
            this.serviceType = serviceType;
        }

        public String getVipUuid() {
            return vipUuid;
        }

        public void setVipUuid(String vipUuid) {
            this.vipUuid = vipUuid;
        }
    }
    @Id
    @Column
    private String uuid;

    @Id
    @Column
    private String serviceType;

    @Id
    @Column
    @ForeignKey(parentEntityClass = VipVO.class, onDeleteAction = ReferenceOption.CASCADE)
    private String vipUuid;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    @Override
    public boolean equals(Object r) {
        if (!(r instanceof VipNetworkServicesRefVO)) {
            return false;
        }

        VipNetworkServicesRefVO ref = (VipNetworkServicesRefVO) r;
        return ref.getUuid().equals(uuid)&&ref.getServiceType().equals(serviceType)&&ref.getVipUuid().equals(vipUuid);
    }

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

    public String getServiceType() {
        return serviceType;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    public String getVipUuid() {
        return vipUuid;
    }

    public void setVipUuid(String vipUuid) {
        this.vipUuid = vipUuid;
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

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
