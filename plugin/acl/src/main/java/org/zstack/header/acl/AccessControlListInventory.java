package org.zstack.header.acl;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.search.Inventory;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author: zhanyong.miao
 * @date: 2020-03-05
 **/
@PythonClassInventory
@Inventory(mappingVOClass = AccessControlListVO.class)
public class AccessControlListInventory {
    private String uuid;
    private String  name;
    private Integer ipVersion;
    private String description;

    private Timestamp createDate;
    private Timestamp lastOpDate;
    private List<AccessControlListEntryInventory> entries;

    public AccessControlListInventory() {
    }

    public static  List<AccessControlListInventory> valueOf(Collection<AccessControlListVO> vos) {
        List<AccessControlListInventory> invs = new ArrayList<>();
        for (AccessControlListVO vo : vos ) {
            invs.add(valueOf(vo));
        }
        return invs;
    }

    public static AccessControlListInventory valueOf(AccessControlListVO vo) {
        AccessControlListInventory inv = new AccessControlListInventory();
        inv.setCreateDate(vo.getCreateDate());
        inv.setDescription(vo.getDescription());
        inv.setEntries(AccessControlListEntryInventory.valueOf(vo.getEntries()));
        inv.setIpVersion(vo.getIpVersion());
        inv.setLastOpDate(vo.getLastOpDate());
        inv.setName(vo.getName());
        inv.setUuid(vo.getUuid());

        return inv;
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

    public Integer getIpVersion() {
        return ipVersion;
    }

    public void setIpVersion(Integer ipVersion) {
        this.ipVersion = ipVersion;
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

    public List<AccessControlListEntryInventory> getEntries() {
        return entries;
    }

    public void setEntries(List<AccessControlListEntryInventory> entries) {
        this.entries = entries;
    }
}