package org.zstack.network.securitygroup;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.query.*;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.search.Inventory;

import javax.persistence.JoinColumn;
import java.sql.Timestamp;
import java.util.*;
/**
 * @inventory
 * inventory for security group
 *
 * @category security group
 *
 * @example
 * {
"inventory": {
"uuid": "3904b4837f0c4f539063777ed463b648",
"name": "test",
"state": "Enabled",
"createDate": "May 14, 2014 9:38:24 PM",
"lastOpDate": "May 14, 2014 9:38:24 PM",
"internalId": 1,
"rules": [
{
"uuid": "ca69dcedbb4f407c9a62240bc54fd6ba",
"securityGroupUuid": "3904b4837f0c4f539063777ed463b648",
"type": "Ingress",
"startPort": 22,
"endPort": 100,
"protocol": "TCP",
"allowedCidr": "0.0.0.0/0",
"createDate": "May 14, 2014 9:38:24 PM",
"lastOpDate": "May 14, 2014 9:38:24 PM"
},
{
"uuid": "02bc62abee88444ca3e2c434a1b8fdea",
"securityGroupUuid": "3904b4837f0c4f539063777ed463b648",
"type": "Ingress",
"startPort": 10,
"endPort": 10,
"protocol": "UDP",
"allowedCidr": "192.168.0.1/0",
"createDate": "May 14, 2014 9:38:24 PM",
"lastOpDate": "May 14, 2014 9:38:24 PM"
}
],
"attachedL3NetworkUuids": []
}
}
 *
 * @since 0.1.0
 */
@Inventory(mappingVOClass = SecurityGroupVO.class)
@PythonClassInventory
@ExpandedQueries({
        @ExpandedQuery(expandedField = "vmNicRef", inventoryClass = VmNicSecurityGroupRefInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "securityGroupUuid", hidden = true),
        @ExpandedQuery(expandedField = "l3NetworkRef", inventoryClass = SecurityGroupL3NetworkRefInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "securityGroupUuid", hidden = true)
})
@ExpandedQueryAliases({
        @ExpandedQueryAlias(alias = "vmNic", expandedField = "vmNicRef.vmNic"),
        @ExpandedQueryAlias(alias = "l3Network", expandedField = "l3NetworkRef.l3Network")
})
public class SecurityGroupInventory {
    /**
     * @desc security group uuid
     */
    private String uuid;
    /**
     * @desc max length of 255 characters
     */
    private String name;
    /**
     * @desc max length of 2048 characters
     * @nullable
     */
    private String description;
    /**
     * @desc
     * .. note:: meaning of states have not been defined yet, they are reserved for future use
     * @choices
     * - Enabled
     * - Disabled
     */
    private String state;

    @Deprecated
    private Integer ipVersion;

    /**
     * @desc the time this resource gets created
     */
    private Timestamp createDate;
    /**
     * @desc last time this resource gets operated
     */
    private Timestamp lastOpDate;
    /**
     * @ignore
     */
    @APINoSee
    private long internalId;
    /**
     * @desc
     * a list of rules. See :ref:`SecurityGroupRuleInventory`
     */
    @Queryable(mappingClass = SecurityGroupRuleInventory.class,
            joinColumn = @JoinColumn(name = "securityGroupUuid"))
    private List<SecurityGroupRuleInventory> rules;
    /**
     * @desc
     * a list of l3Network uuid where this security group has attached to
     */
    @Queryable(mappingClass = SecurityGroupL3NetworkRefInventory.class,
            joinColumn = @JoinColumn(name="securityGroupUuid", referencedColumnName = "l3NetworkUuid"))
    private Set<String> attachedL3NetworkUuids;

    public SecurityGroupInventory() {
    }

    protected SecurityGroupInventory(SecurityGroupVO vo) {
        this.setUuid(vo.getUuid());
        this.setName(vo.getName());
        this.setInternalId(vo.getInternalId());
        this.setDescription(vo.getDescription());
        this.setCreateDate(vo.getCreateDate());
        this.setLastOpDate(vo.getLastOpDate());
        this.setRules(SecurityGroupRuleInventory.valueOf(vo.getRules()));
        this.setState(vo.getState().toString());
        Set<String> l3Uuids= new HashSet<String>(vo.getAttachedL3NetworkRefs().size());
        for (SecurityGroupL3NetworkRefVO ref : vo.getAttachedL3NetworkRefs()) {
            l3Uuids.add(ref.getL3NetworkUuid());
        }
        this.setAttachedL3NetworkUuids(l3Uuids);
    }

    public static SecurityGroupInventory valueOf(SecurityGroupVO vo) {
        return new SecurityGroupInventory(vo);
    }

    public static List<SecurityGroupInventory> valueOf(Collection<SecurityGroupVO> vos) {
        List<SecurityGroupInventory> invs = new ArrayList<SecurityGroupInventory>(vos.size());
        for (SecurityGroupVO vo : vos) {
            invs.add(SecurityGroupInventory.valueOf(vo));
        }
        return invs;
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

    public List<SecurityGroupRuleInventory> getRules() {
        return rules;
    }

    public void setRules(List<SecurityGroupRuleInventory> rules) {
        this.rules = rules;
    }

    public long getInternalId() {
        return internalId;
    }

    public void setInternalId(long internalId) {
        this.internalId = internalId;
    }

    public Set<String> getAttachedL3NetworkUuids() {
        return attachedL3NetworkUuids;
    }

    public void setAttachedL3NetworkUuids(Set<String> attachedL3NetworkUuids) {
        this.attachedL3NetworkUuids = attachedL3NetworkUuids;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    @Deprecated
    public Integer getIpVersion() {
        return ipVersion;
    }

    @Deprecated
    public void setIpVersion(Integer ipVersion) {
        this.ipVersion = ipVersion;
    }
}
