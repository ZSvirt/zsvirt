package org.zstack.appliancevm;

import org.zstack.header.network.l3.L3NetworkEO;
import org.zstack.header.vm.VmInstanceEO;
import org.zstack.header.vm.VmNicVO;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.ForeignKey.ReferenceOption;
import org.zstack.header.vo.Index;
import org.zstack.utils.CollectionUtils;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.Collection;

/**
 */
@Entity
@Table
public class ApplianceVmFirewallRuleVO {
    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    @Column
    private long id;

    @Column
    @ForeignKey(parentEntityClass = VmInstanceEO.class, onDeleteAction = ReferenceOption.CASCADE)
    private String applianceVmUuid;

    @Column
    @Index
    @Enumerated(EnumType.STRING)
    private ApplianceVmFirewallProtocol protocol;

    @Column
    @Index
    private int startPort;

    @Column
    @Index
    private int endPort;

    @Column
    @Index
    private String allowCidr;

    @Column
    @Index
    private String sourceIp;

    @Column
    @Index
    private String destIp;

    @Column
    @ForeignKey(parentEntityClass = L3NetworkEO.class, onDeleteAction = ReferenceOption.CASCADE)
    private String l3NetworkUuid;

    @Column
    @Index
    private String identity;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
    }

    public void makeIdentity() {
        identity = String.format("%s-%s-%s-%s-%s-%s-%s-%s",
                applianceVmUuid, l3NetworkUuid, startPort, endPort, protocol, allowCidr, sourceIp, destIp);
    }

    public String getIdentity() {
        return identity;
    }

    public void setIdentity(String identity) {
        this.identity = identity;
    }

    public String getL3NetworkUuid() {
        return l3NetworkUuid;
    }

    public void setL3NetworkUuid(String l3NetworkUuid) {
        this.l3NetworkUuid = l3NetworkUuid;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getApplianceVmUuid() {
        return applianceVmUuid;
    }

    public void setApplianceVmUuid(String applianceVmUuid) {
        this.applianceVmUuid = applianceVmUuid;
    }

    public ApplianceVmFirewallProtocol getProtocol() {
        return protocol;
    }

    public void setProtocol(ApplianceVmFirewallProtocol protocol) {
        this.protocol = protocol;
    }

    public int getStartPort() {
        return startPort;
    }

    public void setStartPort(int startPort) {
        if (endPort == 0) {
            endPort = startPort;
        }
        this.startPort = startPort;
    }

    public String getSourceIp() {
        return sourceIp;
    }

    public void setSourceIp(String sourceIp) {
        this.sourceIp = sourceIp;
    }

    public String getDestIp() {
        return destIp;
    }

    public void setDestIp(String destIp) {
        this.destIp = destIp;
    }

    public int getEndPort() {
        return endPort;
    }

    public void setEndPort(int endPort) {
        this.endPort = endPort;
    }

    public String getAllowCidr() {
        return allowCidr;
    }

    public void setAllowCidr(String allowCidr) {
        this.allowCidr = allowCidr;
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


    public ApplianceVmFirewallRuleTO toRuleTO(Collection<VmNicVO> nics) {
        ApplianceVmFirewallRuleTO to = new ApplianceVmFirewallRuleTO();
        VmNicVO nic = CollectionUtils.findOneOrNull(nics, arg -> arg.getL3NetworkUuid().equals(l3NetworkUuid));
        to.setDestIp(destIp);
        to.setSourceIp(sourceIp);
        to.setAllowCidr(allowCidr);
        to.setEndPort(endPort);
        to.setStartPort(startPort);
        to.setProtocol(protocol.toString());
        to.setNicMac(nic.getMac());
        return to;
    }
}
