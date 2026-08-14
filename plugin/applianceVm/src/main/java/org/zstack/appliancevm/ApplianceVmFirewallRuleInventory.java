package org.zstack.appliancevm;

import org.zstack.header.rest.APINoSee;
import org.zstack.header.search.Inventory;
import org.zstack.utils.StringDSL;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 */
@Inventory(mappingVOClass = ApplianceVmFirewallRuleVO.class)
public class ApplianceVmFirewallRuleInventory implements Serializable {
    @APINoSee
    private Long id;
    private String applianceVmUuid;
    private String protocol;
    private Integer startPort;
    private Integer endPort;
    private String allowCidr;
    private String sourceIp;
    private String destIp;
    private String l3NetworkUuid;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public static ApplianceVmFirewallRuleInventory valueOf(ApplianceVmFirewallRuleVO vo) {
        ApplianceVmFirewallRuleInventory inv = new ApplianceVmFirewallRuleInventory();
        inv.setApplianceVmUuid(vo.getApplianceVmUuid());
        inv.setId(vo.getId());
        inv.setStartPort(vo.getStartPort());
        inv.setEndPort(vo.getEndPort());
        inv.setProtocol(vo.getProtocol().toString());
        inv.setAllowCidr(vo.getAllowCidr());
        inv.setSourceIp(vo.getSourceIp());
        inv.setDestIp(vo.getDestIp());
        inv.setL3NetworkUuid(vo.getL3NetworkUuid());
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());
        return inv;
    }

    public static List<ApplianceVmFirewallRuleInventory> valueOf(Collection<ApplianceVmFirewallRuleVO> vos) {
        List<ApplianceVmFirewallRuleInventory> invs = new ArrayList<ApplianceVmFirewallRuleInventory>();
        for (ApplianceVmFirewallRuleVO vo : vos) {
            invs.add(valueOf(vo));
        }

        return invs;
    }

    public String makeIdentity() {
        return String.format("%s-%s-%s-%s-%s-%s-%s-%s",
                applianceVmUuid, l3NetworkUuid, startPort, endPort, protocol, allowCidr, sourceIp, destIp);
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

    public int getStartPort() {
        return startPort;
    }

    public void setStartPort(int startPort) {
        this.startPort = startPort;
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

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public boolean isEquals(ApplianceVmFirewallRuleVO vo) {
        return vo != null && startPort == vo.getStartPort() && endPort == vo.getEndPort()
                && protocol.equals(vo.getProtocol().toString()) &&l3NetworkUuid.equals(vo.getL3NetworkUuid())
                && applianceVmUuid.equals(vo.getApplianceVmUuid()) && StringDSL.equals(allowCidr, vo.getAllowCidr())
                && StringDSL.equals(sourceIp, vo.getSourceIp()) && StringDSL.equals(destIp, vo.getDestIp());
    }
}
