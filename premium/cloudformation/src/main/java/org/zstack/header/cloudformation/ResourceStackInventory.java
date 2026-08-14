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
@Inventory(mappingVOClass = ResourceStackVO.class)
public class ResourceStackInventory implements Serializable {
    private String uuid;
    private String name;
    private String description;
    private String version;
    private String type;
    private String templateContent;
    private String paramContent;
    private String status;
    private String reason;
    private String outputs;
    private Boolean enableRollback;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public static ResourceStackInventory valueOf(ResourceStackVO vo) {
        ResourceStackInventory inv = new ResourceStackInventory();
        inv.uuid = vo.getUuid();
        inv.name = vo.getName();
        inv.reason = vo.getReason();
        inv.enableRollback = vo.isEnableRollback();
        inv.templateContent = vo.getTemplateContent();
        inv.paramContent = vo.getParamContent();
        inv.type = vo.getType();
        inv.status = vo.getStatus().toString();
        inv.version = vo.getVersion();
        inv.outputs = vo.getOutputs();
        inv.description = vo.getDescription();
        inv.createDate = vo.getCreateDate();
        inv.lastOpDate = vo.getLastOpDate();
        return inv;
    }

    public static List<ResourceStackInventory> valueOf(Collection<ResourceStackVO> vos) {
        return vos.stream().map(ResourceStackInventory::valueOf).collect(Collectors.toList());
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

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTemplateContent() {
        return templateContent;
    }

    public void setTemplateContent(String templateContent) {
        this.templateContent = templateContent;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public boolean isEnableRollback() {
        return enableRollback;
    }

    public void setEnableRollback(boolean enableRollback) {
        this.enableRollback = enableRollback;
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

    public String getParamContent() {
        return paramContent;
    }

    public void setParamContent(String paramContent) {
        this.paramContent = paramContent;
    }

    public String getOutputs() {
        return outputs;
    }

    public void setOutputs(String outputs) {
        this.outputs = outputs;
    }
}
