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
 * Created by mingjian.deng on 2018/3/5.
 */
@Inventory(mappingVOClass = NasFileSystemVO.class)
@PythonClassInventory
public class NasFileSystemInventory implements Serializable {
    private String uuid;
    private NasProtocolType protocol;
    @TypeField
    private String type;
    private String name;
    private String description;
    private String fileSystemId;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public NasFileSystemInventory() {
    }

    public static NasFileSystemInventory valueOf(NasFileSystemVO vo) {
        NasFileSystemInventory inventory = new NasFileSystemInventory();
        inventory.setUuid(vo.getUuid());
        inventory.setName(vo.getName());
        inventory.setProtocol(vo.getProtocol());
        inventory.setType(vo.getType());
        inventory.setFileSystemId(vo.getFileSystemId());
        inventory.setDescription(vo.getDescription());
        inventory.setCreateDate(vo.getCreateDate());
        inventory.setLastOpDate(vo.getLastOpDate());
        return inventory;
    }

    public static List<NasFileSystemInventory> valueOf(Collection<NasFileSystemVO> vos) {
        List<NasFileSystemInventory> inventories = new ArrayList<>();
        for (NasFileSystemVO vo: vos) {
            inventories.add(valueOf(vo));
        }
        return inventories;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public NasProtocolType getProtocol() {
        return protocol;
    }

    public void setProtocol(NasProtocolType protocol) {
        this.protocol = protocol;
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

    public String getFileSystemId() {
        return fileSystemId;
    }

    public void setFileSystemId(String fileSystemId) {
        this.fileSystemId = fileSystemId;
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

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
}
