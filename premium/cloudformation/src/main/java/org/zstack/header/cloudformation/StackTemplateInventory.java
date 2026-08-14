package org.zstack.header.cloudformation;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.search.Inventory;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by mingjian.deng on 2018/6/5.
 */
@PythonClassInventory
@Inventory(mappingVOClass = StackTemplateVO.class)
public class StackTemplateInventory implements Serializable {
    private String uuid;
    private String name;
    private String description;
    private String type = "zstack";
    private String version;
    private Boolean state;
    private String content;
    private String md5sum;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public static StackTemplateInventory valueOf(StackTemplateVO vo) {
        StackTemplateInventory inv = new StackTemplateInventory();
        inv.uuid = vo.getUuid();
        inv.name = vo.getName();
        inv.content = vo.getContent();
        inv.type = vo.getType();
        inv.version = vo.getVersion();
        inv.state = vo.getState();
        inv.md5sum = vo.getMd5sum();
        inv.description = vo.getDescription();
        inv.createDate = vo.getCreateDate();
        inv.lastOpDate = vo.getLastOpDate();
        return inv;
    }

    public static List<StackTemplateInventory> valueOf(Collection<StackTemplateVO> vos) {
        return vos.stream().map(StackTemplateInventory::valueOf).collect(Collectors.toList());
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

    public Boolean getState() {
        return state;
    }

    public void setState(Boolean state) {
        this.state = state;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
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

    public String getMd5sum() {
        return md5sum;
    }

    public void setMd5sum(String md5sum) {
        this.md5sum = md5sum;
    }
}
