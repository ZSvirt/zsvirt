package org.zstack.header.vipQos;

import org.zstack.header.vo.ForeignKey;
import org.zstack.network.service.vip.VipVO;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@Table
public class VipQosVO {
    @Id
    @Column
    private String uuid;

    @Column
    @ForeignKey(parentEntityClass = VipVO.class, parentKey = "uuid", onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String vipUuid;

    @Column
    private int port;

    @Column
    private long inboundBandwidth;

    @Column
    private long outboundBandwidth;

    @Column
    private String type;

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

    public String getVipUuid() {
        return vipUuid;
    }

    public void setVipUuid(String vipUuid) {
        this.vipUuid = vipUuid;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public long getInboundBandwidth() {
        return inboundBandwidth;
    }

    public void setInboundBandwidth(long inboundBandwidth) {
        this.inboundBandwidth = inboundBandwidth;
    }

    public long getOutboundBandwidth() {
        return outboundBandwidth;
    }

    public void setOutboundBandwidth(long outboundBandwidth) {
        this.outboundBandwidth = outboundBandwidth;
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
