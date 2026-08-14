package org.zstack.header.baremetal.network;

import org.zstack.header.baremetal.instance.BaremetalInstanceVO;
import org.zstack.header.identity.OwnedByAccount;
import org.zstack.header.network.l3.L3NetworkEO;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.network.l3.UsedIpVO;
import org.zstack.header.vo.*;
import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.Index;

import javax.persistence.*;
import java.sql.Timestamp;

/**
 * Created by GuoYi on 7/5/18.
 */
@Entity
@Table
@BaseResource
@EntityGraph(
        parents = {
                @EntityGraph.Neighbour(type = BaremetalInstanceVO.class, myField = "baremetalInstanceUuid", targetField = "uuid"),
                @EntityGraph.Neighbour(type = L3NetworkVO.class, myField = "l3NetworkUuid", targetField = "uuid"),
        }
)
public class BaremetalNicVO extends ResourceVO implements ToInventory, OwnedByAccount {
    @Column
    @ForeignKey(parentEntityClass = BaremetalInstanceVO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String baremetalInstanceUuid;

    @Column
    @ForeignKey(parentEntityClass = L3NetworkEO.class, onDeleteAction = ForeignKey.ReferenceOption.SET_NULL)
    private String l3NetworkUuid;

    @Column
    @ForeignKey(parentEntityClass = UsedIpVO.class, onDeleteAction = ForeignKey.ReferenceOption.SET_NULL)
    private String usedIpUuid;

    @Column
    @ForeignKey(parentEntityClass = BaremetalBondingVO.class, onDeleteAction = ForeignKey.ReferenceOption.SET_NULL)
    private String baremetalBondingUuid;

    @Column
    private String mac;

    @Column
    @Index
    private String ip;

    @Column
    private String netmask;

    @Column
    private String gateway;

    @Column
    private String metaData;

    @Column
    private Boolean pxe = false;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
    }

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

    public String getBaremetalInstanceUuid() {
        return baremetalInstanceUuid;
    }

    public void setBaremetalInstanceUuid(String baremetalInstanceUuid) {
        this.baremetalInstanceUuid = baremetalInstanceUuid;
    }

    public String getL3NetworkUuid() {
        return l3NetworkUuid;
    }

    public void setL3NetworkUuid(String l3NetworkUuid) {
        this.l3NetworkUuid = l3NetworkUuid;
    }

    public String getUsedIpUuid() {
        return usedIpUuid;
    }

    public void setUsedIpUuid(String usedIpUuid) {
        this.usedIpUuid = usedIpUuid;
    }

    public String getBaremetalBondingUuid() {
        return baremetalBondingUuid;
    }

    public void setBaremetalBondingUuid(String baremetalBondingUuid) {
        this.baremetalBondingUuid = baremetalBondingUuid;
    }

    public String getMac() {
        return mac;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getNetmask() {
        return netmask;
    }

    public void setNetmask(String netmask) {
        this.netmask = netmask;
    }

    public String getGateway() {
        return gateway;
    }

    public void setGateway(String gateway) {
        this.gateway = gateway;
    }

    public String getMetaData() {
        return metaData;
    }

    public void setMetaData(String metaData) {
        this.metaData = metaData;
    }

    public Boolean getPxe() {
        return pxe;
    }

    public void setPxe(Boolean pxe) {
        this.pxe = pxe;
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

    public BaremetalNicVO() {
    }

    public BaremetalNicVO(BaremetalNicVO other) {
        this.setUuid(other.getUuid());
        this.setPxe(other.getPxe());
        this.setL3NetworkUuid(other.getL3NetworkUuid());
        this.setBaremetalInstanceUuid(other.getBaremetalInstanceUuid());
        this.setBaremetalBondingUuid(other.getBaremetalBondingUuid());
        this.setMac(other.getMac());
        this.setIp(other.getIp());
        this.setNetmask(other.getNetmask());
        this.setGateway(other.getGateway());
        this.setUsedIpUuid(other.getUsedIpUuid());
        this.setMetaData(other.getMetaData());
        this.setCreateDate(other.getCreateDate());
        this.setLastOpDate(other.getLastOpDate());
        this.setAccountUuid(other.getAccountUuid());
    }
}
