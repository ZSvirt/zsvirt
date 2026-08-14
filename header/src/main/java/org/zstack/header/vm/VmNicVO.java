package org.zstack.header.vm;

import org.zstack.header.identity.OwnedByAccount;
import org.zstack.header.network.l3.L3NetworkEO;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.network.l3.UsedIpVO;
import org.zstack.header.vo.*;
import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.ForeignKey.ReferenceOption;
import org.zstack.header.vo.Index;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table
@BaseResource
@EntityGraph(
        parents = {
                @EntityGraph.Neighbour(type = VmInstanceVO.class, myField = "vmInstanceUuid", targetField = "uuid"),
                @EntityGraph.Neighbour(type = L3NetworkVO.class, myField = "l3NetworkUuid", targetField = "uuid")
        }
)
public class VmNicVO extends ResourceVO implements OwnedByAccount {
    @Column
    @ForeignKey(parentEntityClass = VmInstanceEO.class, onDeleteAction = ReferenceOption.CASCADE)
    private String vmInstanceUuid;

    @Column
    @ForeignKey(parentEntityClass = UsedIpVO.class, onDeleteAction = ReferenceOption.SET_NULL)
    private String usedIpUuid;

    @Column
    @ForeignKey(parentEntityClass = L3NetworkEO.class, onDeleteAction = ReferenceOption.SET_NULL)
    private String l3NetworkUuid;

    @Column
    @Index
    private String ip;

    @Column
    private String netmask;

    @Column
    private String gateway;

    @Deprecated
    @Column
    private Integer ipVersion;

    @Column
    private String mac;

    @Column
    private String hypervisorType;

    @Column
    private String metaData;

    @Column
    private int deviceId;

    @Column
    private String internalName;

    @Column
    private String driverType;

    @Column
    private String type;

    @Column
    @Enumerated(EnumType.STRING)
    private VmNicState state = VmNicState.enable;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "vmNicUuid", insertable = false, updatable = false)
    @NoView
    private Set<UsedIpVO> usedIps = new HashSet<>();

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

    public static VmNicVO findTheEarliestOne(List<VmNicVO> nics) {
        VmNicVO nic = null;
        for (VmNicVO n : nics) {
            if (nic == null || nic.getCreateDate().after(n.getCreateDate())) {
                nic = n;
            } else if (nic.getCreateDate().equals(n.getCreateDate()) && nic.getDeviceId() > n.getDeviceId()) {
                nic = n;
            }
        }
        return nic;
    }

    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public String getUsedIpUuid() {
        return usedIpUuid;
    }

    public void setUsedIpUuid(String usedIpUuid) {
        this.usedIpUuid = usedIpUuid;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getMac() {
        return mac;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

    public String getHypervisorType() {
        return hypervisorType;
    }

    public void setHypervisorType(String hypervisorType) {
        this.hypervisorType = hypervisorType;
    }

    public String getL3NetworkUuid() {
        return l3NetworkUuid;
    }

    public void setL3NetworkUuid(String l3NetworkUuid) {
        this.l3NetworkUuid = l3NetworkUuid;
    }

    public int getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(int deviceId) {
        this.deviceId = deviceId;
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

    public String getInternalName() {
        return internalName;
    }

    public void setInternalName(String internalName) {
        this.internalName = internalName;
    }

    public static String generateNicInternalName(long vmInternalId, long nicDeviceId) {
        return String.format("vnic%s.%s", vmInternalId, nicDeviceId);
    }

    @Deprecated
    public Integer getIpVersion() {
        return ipVersion;
    }

    @Deprecated
    public void setIpVersion(Integer ipVersion) {
        this.ipVersion = ipVersion;
    }

    public Set<UsedIpVO> getUsedIps() {
        return usedIps;
    }

    public void setUsedIps(Set<UsedIpVO> usedIps) {
        this.usedIps = usedIps;
    }

    public String getDriverType() {
        return driverType;
    }

    public void setDriverType(String driverType) {
        this.driverType = driverType;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public VmNicState getState() {
        return state;
    }

    public void setState(VmNicState enable) {
        this.state = enable;
    }

    public void removeIp() {
        this.setUsedIpUuid(null);
        this.setIp(null);
        this.setNetmask(null);
        this.setGateway(null);
        this.setIpVersion(null);
    }
}
