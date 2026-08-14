package org.zstack.autoscaling.group;

import org.zstack.autoscaling.template.AutoScalingTemplateGroupRefVO;
import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.search.Inventory;
import org.zstack.header.search.TypeField;
import org.zstack.header.tag.SystemTagVO;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Create by weiwang at 2018/8/16
 */
@PythonClassInventory
@Inventory(mappingVOClass = AutoScalingGroupVO.class, collectionValueOfMethod = "valueOf1")
public class AutoScalingGroupInventory implements Serializable {
    private String name;

    private String uuid;

    @TypeField
    private String scalingResourceType;

    private String state;

    private Long defaultCooldown;

    private String description;

    private Integer minResourceSize;

    private Integer maxResourceSize;

    private String removalPolicy;

    private Timestamp createDate;

    private Timestamp lastOpDate;

    private List<String> attachedTemplates;

    private List<String> systemTags;

    public AutoScalingGroupInventory() {
    }

    public AutoScalingGroupInventory(AutoScalingGroupVO vo) {
        this.setUuid(vo.getUuid());
        this.setName(vo.getResourceName());
        this.setScalingResourceType(vo.getScalingResourceType().toString());
        this.setState(vo.getState().name());
        this.setDescription(vo.getDescription());
        this.setCreateDate(vo.getCreateDate());
        this.setLastOpDate(vo.getLastOpDate());
        this.setMaxResourceSize(vo.getMaxResourceSize());
        this.setMinResourceSize(vo.getMinResourceSize());
        this.setRemovalPolicy(vo.getRemovalPolicy().toString());
        this.setDefaultCooldown(vo.getDefaultCooldown());

        this.setSystemTags(new ArrayList<>());
        if (vo.getSystemTags() != null && !vo.getSystemTags().isEmpty()) {
            for (SystemTagVO tagVO : vo.getSystemTags()) {
                this.getSystemTags().add(tagVO.getTag());
            }
        }

        this.setAttachedTemplates(new ArrayList<>());
        if (vo.getAttachedTemplates() != null && !vo.getAttachedTemplates().isEmpty()) {
            for (AutoScalingTemplateGroupRefVO refVO : vo.getAttachedTemplates()) {
                this.getAttachedTemplates().add(refVO.getTemplateUuid());
            }
        }
    }

    public static AutoScalingGroupInventory valueOf(AutoScalingGroupVO vo) {
        return new AutoScalingGroupInventory(vo);
    }

    public static List<AutoScalingGroupInventory> valueOf1(Collection<AutoScalingGroupVO> vos) {
        List<AutoScalingGroupInventory> invs = new ArrayList<AutoScalingGroupInventory>();
        for (AutoScalingGroupVO vo : vos) {
            invs.add(valueOf(vo));
        }

        return invs;
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

    public List<String> getSystemTags() {
        return systemTags;
    }

    public void setSystemTags(List<String> systemTags) {
        this.systemTags = systemTags;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getScalingResourceType() {
        return scalingResourceType;
    }

    public void setScalingResourceType(String scalingResourceType) {
        this.scalingResourceType = scalingResourceType;
    }

    public Integer getMinResourceSize() {
        return minResourceSize;
    }

    public void setMinResourceSize(Integer minResourceSize) {
        this.minResourceSize = minResourceSize;
    }

    public Integer getMaxResourceSize() {
        return maxResourceSize;
    }

    public void setMaxResourceSize(Integer maxResourceSize) {
        this.maxResourceSize = maxResourceSize;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getRemovalPolicy() {
        return removalPolicy;
    }

    public void setRemovalPolicy(String removalPolicy) {
        this.removalPolicy = removalPolicy;
    }

    public List<String> getAttachedTemplates() {
        return attachedTemplates;
    }

    public void setAttachedTemplates(List<String> attachedTemplates) {
        this.attachedTemplates = attachedTemplates;
    }

    public Long getDefaultCooldown() {
        return defaultCooldown;
    }

    public void setDefaultCooldown(Long defaultCooldown) {
        this.defaultCooldown = defaultCooldown;
    }
}
