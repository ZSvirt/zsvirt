package org.zstack.network.service.portforwarding;

import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.search.Inventory;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.network.service.vip.VipInventory;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
/**
 * @inventory
 * inventory for port forwarding rule
 *
 * @category port forwarding
 *
 * @example
 * {
"inventory": {
"uuid": "5ddaefbaba7d46d889aa3f3a6f50f6c8",
"name": "pfRule1",
"vipUuid": "22647f340e1037d4a2ea499aca42075e",
"vipPortStart": 22,
"vipPortEnd": 100,
"privatePortStart": 22,
"privatePortEnd": 100,
"vmNicUuid": "bd00f2c066c94f07b0dfae2e9e9b567f",
"protocolType": "TCP",
"allowedCidr": "77.10.3.1/24"
}
}
 *
 * @since 0.1.0
 */
@Inventory(mappingVOClass = PortForwardingRuleVO.class)
@ExpandedQueries({
        @ExpandedQuery(expandedField = "vip", inventoryClass = VipInventory.class,
                foreignKey = "vipUuid", expandedInventoryKey = "uuid"),
        @ExpandedQuery(expandedField = "vmNic", inventoryClass = VmNicInventory.class,
                foreignKey = "vmNicUuid", expandedInventoryKey = "uuid"),
        @ExpandedQuery(target = VmNicInventory.class, expandedField = "portForwarding", inventoryClass = PortForwardingRuleInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "vmNicUuid"),
        @ExpandedQuery(target = VipInventory.class, expandedField = "portForwarding", inventoryClass = PortForwardingRuleInventory.class, foreignKey = "uuid",
                expandedInventoryKey = "vipUuid")
})
public class PortForwardingRuleInventory implements Serializable {
    /**
     * @desc rule uuid
     */
    private String uuid;
    /**
     * @desc max length of 255 characters
     */
    private String name;
    /**
     * @desc max length of 2048 characters
     */
    private String description;

    private String vipIp;

    private String guestIp;
    /**
     * @desc uuid of vip this port forwarding rule created on
     */
    private String vipUuid;
    /**
     * @desc start port to be mapped
     */
    private Integer vipPortStart;
    /**
     * @desc end port to be mapped
     */
    private Integer vipPortEnd;
    /**
     * @desc start port the vipPortStart maps to
     */
    private Integer privatePortStart;
    /**
     * @desc end port the vipPortEnd maps to
     */
    private Integer privatePortEnd;
    /**
     * @desc uuid of vm nic(see :ref:`VmNicInventory`) the rule applies to. When null, the rule
     * is not attached to any vm nic yet
     *
     * @nullable
     */
    private String vmNicUuid;
    /**
     * @desc network protocol type
     * @choices
     * - TCP
     * - UDP
     */
    private String protocolType;

    private String state;

    /**
     * @desc if not null, the rule only applies to traffic from this CIDR, other traffic are denied
     * @nullable
     */
    private String allowedCidr;
    /**
     * @desc the time this resource gets created
     */
    private Timestamp createDate;
    /**
     * @desc last time this resource gets operated
     */
    private Timestamp lastOpDate;
    
    public static PortForwardingRuleInventory valueOf(PortForwardingRuleVO vo) {
        PortForwardingRuleInventory inv = new PortForwardingRuleInventory();
        inv.setName(vo.getName());
        inv.setDescription(vo.getDescription());
        inv.setAllowedCidr(vo.getAllowedCidr());
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());
        inv.setPrivatePortEnd(vo.getPrivatePortEnd());
        inv.setPrivatePortStart(vo.getPrivatePortStart());
        inv.setVipUuid(vo.getVipUuid());
        inv.setVipPortStart(vo.getVipPortStart());
        inv.setVipPortEnd(vo.getVipPortEnd());
        inv.setUuid(vo.getUuid());
        inv.setVmNicUuid(vo.getVmNicUuid());
        inv.setProtocolType(vo.getProtocolType().toString());
        inv.setState(vo.getState().toString());
        inv.setVipIp(vo.getVipIp());
        inv.setGuestIp(vo.getGuestIp());
        return inv;
    }
    
    public static List<PortForwardingRuleInventory> valueOf(Collection<PortForwardingRuleVO> vos) {
        List<PortForwardingRuleInventory> invs = new ArrayList<PortForwardingRuleInventory>();
        for (PortForwardingRuleVO vo : vos) {
            invs.add(valueOf(vo));
        }
        return invs;
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

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
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

    public String getProtocolType() {
        return protocolType;
    }

    public void setProtocolType(String protocolType) {
        this.protocolType = protocolType;
    }

    public String getAllowedCidr() {
        return allowedCidr;
    }

    public void setAllowedCidr(String allowedCidr) {
        this.allowedCidr = allowedCidr;
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
