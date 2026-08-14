package org.zstack.header.baremetal.preconfiguration;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.message.DocUtils;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.search.Inventory;
import org.zstack.utils.StringDSL;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by GuoYi on 2018-12-28.
 */
@PythonClassInventory
@Inventory(mappingVOClass = PreconfigurationTemplateVO.class)
@ExpandedQueries({
        @ExpandedQuery(expandedField = "customParams", inventoryClass = TemplateCustomParamInventory.class, foreignKey = "uuid", expandedInventoryKey = "templateUuid"),
})
public class PreconfigurationTemplateInventory implements Serializable {
    private String uuid;
    private String name;
    private String description;
    private String distribution;
    private String type;
    private String content;
    private String md5sum;
    private Boolean isPredefined;
    private String state;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    private Set<String> customParams;

    public static PreconfigurationTemplateInventory valueOf(PreconfigurationTemplateVO vo) {
        PreconfigurationTemplateInventory inv = new PreconfigurationTemplateInventory();
        inv.uuid = vo.getUuid();
        inv.name = vo.getName();
        inv.description = vo.getDescription();
        inv.distribution = vo.getDistribution();
        inv.type = vo.getType();
        inv.content = vo.getContent();
        inv.md5sum = vo.getMd5sum();
        inv.setPredefined(vo.getPredefined());
        inv.state = vo.getState().toString();
        inv.createDate = vo.getCreateDate();
        inv.lastOpDate = vo.getLastOpDate();
        inv.setCustomParams(vo.getCustomParams().stream().map(TemplateCustomParamVO::getParam).collect(Collectors.toSet()));
        return inv;
    }

    public static List<PreconfigurationTemplateInventory> valueOf(Collection<PreconfigurationTemplateVO> vos) {
        return vos.stream().map(PreconfigurationTemplateInventory::valueOf).collect(Collectors.toList());
    }

    public static PreconfigurationTemplateInventory __example__() {
        PreconfigurationTemplateInventory inv = new PreconfigurationTemplateInventory();
        inv.setUuid(StringDSL.createFixedUuid(PreconfigurationTemplateVO.class));
        inv.setName("centos_7_x86_64_mini_v1.cfg");
        inv.setDescription("centos-7-minimal kickstart file");
        inv.setDistribution("centos-7-x86_64");
        inv.setType(PreconfigurationTemplateType.kickstart.toString());
        inv.setContent("...");
        inv.setMd5sum("4448ac513c464e86aeb7eb75417c9345");
        inv.setPredefined(false);
        inv.setState(PreconfigurationTemplateState.Enabled.toString());
        inv.createDate = new Timestamp(DocUtils.date);
        inv.lastOpDate = new Timestamp(DocUtils.date);
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDistribution() {
        return distribution;
    }

    public void setDistribution(String distribution) {
        this.distribution = distribution;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getMd5sum() {
        return md5sum;
    }

    public void setMd5sum(String md5sum) {
        this.md5sum = md5sum;
    }

    public Boolean getPredefined() {
        return isPredefined;
    }

    public void setPredefined(Boolean predefined) {
        isPredefined = predefined;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
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

    public Set<String> getCustomParams() {
        return customParams;
    }

    public void setCustomParams(Set<String> customParams) {
        this.customParams = customParams;
    }
}

