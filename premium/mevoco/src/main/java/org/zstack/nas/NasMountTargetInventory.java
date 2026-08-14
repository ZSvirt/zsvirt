package org.zstack.nas;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.search.Inventory;
import org.zstack.header.search.TypeField;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by mingjian.deng on 2018/3/8.
 */
@Inventory(mappingVOClass = NasMountTargetVO.class)
@PythonClassInventory
public class NasMountTargetInventory implements Serializable {
    private String uuid;
    private String name;
    private String description;
    private String mountDomain;
    private String nasFileSystemUuid;
    @TypeField
    private String type;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public NasMountTargetInventory() {
    }

    public static NasMountTargetInventory valueOf(NasMountTargetVO vo) {
        NasMountTargetInventory inventory = new NasMountTargetInventory();
        inventory.setUuid(vo.getUuid());
        inventory.setName(vo.getName());
        inventory.setType(vo.getType());
        inventory.setDescription(vo.getDescription());
        inventory.setMountDomain(vo.getMountDomain());
        inventory.setNasFileSystemUuid(vo.getNasFileSystemUuid());
        inventory.setCreateDate(vo.getCreateDate());
        inventory.setLastOpDate(vo.getLastOpDate());
        return inventory;
    }

    public static List<NasMountTargetInventory> valueOf(Collection<NasMountTargetVO> vos) {
        List<NasMountTargetInventory> inventories = new ArrayList<>();
        for (NasMountTargetVO vo: vos) {
            inventories.add(valueOf(vo));
        }
        return inventories;
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

    public String getNasFileSystemUuid() {
        return nasFileSystemUuid;
    }

    public void setNasFileSystemUuid(String nasFileSystemUuid) {
        this.nasFileSystemUuid = nasFileSystemUuid;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
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

    public String getMountDomain() {
        return mountDomain;
    }

    public void setMountDomain(String mountDomain) {
        this.mountDomain = mountDomain;
    }
}
