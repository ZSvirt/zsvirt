package org.zstack.header.network.l3;

import org.zstack.header.vm.VmNicVO;
import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.ForeignKey.ReferenceOption;
import org.zstack.header.vo.Index;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@Table
@Inheritance(strategy = InheritanceType.JOINED)
@EntityGraph(
        parents = {
                @EntityGraph.Neighbour(type = IpRangeVO.class, myField = "ipRangeUuid", targetField = "uuid"),
                @EntityGraph.Neighbour(type = L3NetworkVO.class, myField = "l3NetworkUuid", targetField = "uuid"),
                @EntityGraph.Neighbour(type = VmNicVO.class, myField = "vmNicUuid", targetField = "uuid"),
        }
)
public class UsedIpVO {
    @Id
    @Column
    private String uuid;

    @Column
    @ForeignKey(parentEntityClass = IpRangeEO.class, onDeleteAction = ReferenceOption.CASCADE)
    private String ipRangeUuid;

    @Column
    @ForeignKey(parentEntityClass = L3NetworkEO.class, onDeleteAction = ReferenceOption.CASCADE)
    private String l3NetworkUuid;

    @Column
    @ForeignKey(parentEntityClass = VmNicVO.class, onDeleteAction = ReferenceOption.CASCADE)
    private String vmNicUuid;

    @Column
    private Integer ipVersion;

    @Column
    @Index
    private String ip;

    @Column
    private String gateway;

    @Column
    private String netmask;

    @Column
    @Index
    private long ipInLong;

    @Column
    @Index
    private byte[] ipInBinary;

    @Column
    private String usedFor;

    @Column
    private String metaData;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
    }

    public UsedIpVO(String rangeUuid, String ip) {
        this.ip = ip;
        this.ipRangeUuid = rangeUuid;
    }

    public UsedIpVO() {
    }

    public UsedIpVO(UsedIpVO vo) {
        this.setUuid(vo.getUuid());
        this.setIpRangeUuid(vo.getIpRangeUuid());
        this.setL3NetworkUuid(vo.getL3NetworkUuid());
        this.setVmNicUuid(vo.getVmNicUuid());
        this.setIpVersion(vo.getIpVersion());
        this.setIp(vo.getIp());
        this.setGateway(vo.getGateway());
        this.setNetmask(vo.getNetmask());
        this.setIpInLong(vo.getIpInLong());
        this.setIpInBinary(vo.getIpInBinary());
        this.setUsedFor(vo.getUsedFor());
        this.setMetaData(vo.getMetaData());
        this.setCreateDate(vo.getCreateDate());
        this.setLastOpDate(vo.getLastOpDate());
    }

    public String getUuid() {
        return uuid;
    }


    public void setUuid(String uuid) {
        this.uuid = uuid;
    }


    public String getIpRangeUuid() {
        return ipRangeUuid;
    }

    public void setIpRangeUuid(String ipRangeUuid) {
        this.ipRangeUuid = ipRangeUuid;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public long getIpInLong() {
        return ipInLong;
    }

    public void setIpInLong(long ipInLong) {
        this.ipInLong = ipInLong;
    }

    public byte[] getIpInBinary() {
        return ipInBinary;
    }

    public void setIpInBinary(byte[] ipInBinary) {
        this.ipInBinary = ipInBinary;
    }

    public String getL3NetworkUuid() {
        return l3NetworkUuid;
    }

    public void setL3NetworkUuid(String l3NetworkUuid) {
        this.l3NetworkUuid = l3NetworkUuid;
    }

    public String getGateway() {
        return gateway;
    }

    public void setGateway(String gateway) {
        this.gateway = gateway;
    }

    public String getNetmask() {
        return netmask;
    }

    public void setNetmask(String netmask) {
        this.netmask = netmask;
    }

    public String getUsedFor() {
        return usedFor;
    }

    public void setUsedFor(String usedFor) {
        this.usedFor = usedFor;
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

    public String getMetaData() {
        return metaData;
    }

    public void setMetaData(String metaData) {
        this.metaData = metaData;
    }

    public String getVmNicUuid() {
        return vmNicUuid;
    }

    public void setVmNicUuid(String vmNicUuid) {
        this.vmNicUuid = vmNicUuid;
    }

    public Integer getIpVersion() {
        return ipVersion;
    }

    public long getIpVersionl() {
        return ipVersion;
    }

    public void setIpVersion(Integer ipVersion) {
        this.ipVersion = ipVersion;
    }
}
