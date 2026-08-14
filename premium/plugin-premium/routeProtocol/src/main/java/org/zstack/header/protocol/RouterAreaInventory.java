package org.zstack.header.protocol;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.log.NoLogging;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.search.Inventory;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


@Inventory(mappingVOClass = RouterAreaVO.class)
@PythonClassInventory
@ExpandedQueries({
        //@ExpandedQuery(expandedField = "vmInstance", inventoryClass = VmInstanceInventory.class,
        //        foreignKey = "uuid", expandedInventoryKey = "zoneUuid"),

})

public class RouterAreaInventory implements Serializable {
    private String uuid;
    private String areaId;
    private String type;
    private String authentication;
    @NoLogging
    private String password;
    private Integer keyId;
    private Timestamp createDate;
    private Timestamp lastOpDate;


    public static RouterAreaInventory valueOf(RouterAreaVO vo) {
        RouterAreaInventory inv = new RouterAreaInventory();
        inv.setUuid(vo.getUuid());
        inv.setLastOpDate(vo.getLastOpDate());
        inv.setCreateDate(vo.getCreateDate());
        inv.setAreaId(vo.getAreaId());
        inv.setType(vo.getType().toString());
        inv.setAuthentication(vo.getAuthentication().toString());
        inv.setPassword(vo.getPassword());
        inv.setKeyId(vo.getKeyId());

        return inv;
    }

    public static List<RouterAreaInventory> valueOf(Collection<RouterAreaVO> vos) {
        List<RouterAreaInventory> invs = new ArrayList<RouterAreaInventory>();
        for (RouterAreaVO vo : vos) {
            invs.add(valueOf(vo));
        }
        return invs;
        //return vos.stream().map(RouterAreaInventory::valueOf).collect(Collectors.toList());
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getAreaId() {
        return areaId;
    }

    public void setAreaId(String areaId) {
        this.areaId = areaId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getAuthentication() {
        return authentication;
    }

    public void setAuthentication(String authentication) {
        this.authentication = authentication;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Integer getKeyId() {
        return keyId;
    }

    public void setKeyId(Integer keyId) {
        this.keyId = keyId;
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
